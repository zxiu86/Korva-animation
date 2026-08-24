package com.example.engine.vfx

import com.example.model.vfx.*
import kotlin.math.*
import kotlin.random.Random

/**
 * High-Performance VFX Simulation Engine 2.0.
 * Supports all 16 behavior modules, Sub-Emitters, Particle Trails, Curl/Perlin Noise,
 * Zero-Allocation Pool, Bounds Calculation, and Native JNI Bridge.
 */
class VFXSimulationEngine(
    var effect: VFXEffect = VFXEffect(),
    poolCapacity: Int = 1600
) {
    val pool = VFXParticlePool(poolCapacity)
    private val emitterSpawnAccumulators = mutableMapOf<String, Float>()
    private val emitterBurstTimers = mutableMapOf<String, Float>()
    var currentTime: Float = 0f
        private set

    var bounds: EffectBounds = EffectBounds(-100f, -100f, 100f, 100f)
        private set

    val isNativeConnected: Boolean
        get() = com.korva.engine.VFXNativeBridge.isNativeLoaded || com.example.engine.vfx.VFXNativeBridge.isNativeLoaded

    init {
        initEffect()
    }

    fun setEffectTarget(newEffect: VFXEffect) {
        effect = newEffect
        reset()
    }

    private fun initEffect() {
        if (com.korva.engine.VFXNativeBridge.isNativeLoaded) {
            try {
                com.korva.engine.VFXNativeBridge.initEffect(effect.name, effect.effectId)
            } catch (e: Throwable) {
                // Graceful fallback
            }
        }
    }

    fun reset() {
        pool.clear()
        emitterSpawnAccumulators.clear()
        emitterBurstTimers.clear()
        currentTime = 0f
        initEffect()
    }

    /**
     * Fast-forward simulation (prewarm) to populate initial particle state.
     */
    fun prewarm(warmTimeSeconds: Float = 1.0f, step: Float = 0.016f) {
        var elapsed = 0f
        while (elapsed < warmTimeSeconds) {
            update(step, isPrewarm = true)
            elapsed += step
        }
    }

    /**
     * Advance simulation by deltaTime seconds.
     */
    fun update(deltaTime: Float, isPrewarm: Boolean = false) {
        if (deltaTime <= 0f) return
        val dt = min(deltaTime, 0.05f) * effect.timeScale
        currentTime += dt

        if (effect.duration > 0f && currentTime >= effect.duration) {
            if (effect.looping) {
                currentTime %= effect.duration
            }
        }

        // If native C++ engine is active, forward update
        if (com.korva.engine.VFXNativeBridge.isNativeLoaded && !isPrewarm) {
            try {
                com.korva.engine.VFXNativeBridge.updateEffect(dt)
            } catch (e: Throwable) {
                // Fallback to Kotlin physics
            }
        }

        // 1. Spawning Phase
        effect.emitters.forEachIndexed { emitterIdx, emitter ->
            val emitterKey = emitter.id

            // Handle Continuous Spawning
            if (emitter.spawnRate > 0f) {
                val acc = (emitterSpawnAccumulators[emitterKey] ?: 0f) + emitter.spawnRate * dt
                val spawnCount = acc.toInt()
                emitterSpawnAccumulators[emitterKey] = acc - spawnCount

                for (i in 0 until spawnCount) {
                    spawnParticle(emitter, emitterIdx)
                }
            }

            // Handle Burst Spawning
            if (emitter.burstCount > 0 && emitter.burstInterval > 0f) {
                val timer = (emitterBurstTimers[emitterKey] ?: 0f) + dt
                if (timer >= emitter.burstInterval) {
                    emitterBurstTimers[emitterKey] = timer - emitter.burstInterval
                    for (i in 0 until emitter.burstCount) {
                        spawnParticle(emitter, emitterIdx)
                    }
                } else {
                    emitterBurstTimers[emitterKey] = timer
                }
            }
        }

        // 2. Physics & Module Evaluation Phase
        val particles = pool.particles
        for (p in particles) {
            if (!p.isActive) continue

            p.age += dt
            if (p.isDead()) {
                // Trigger onDeath Sub-Emitter
                val emitter = effect.emitters.getOrNull(p.emitterIndex)
                if (emitter != null && emitter.onDeathSubEmitter in 0 until effect.emitters.size) {
                    triggerSubEmitter(effect.emitters[emitter.onDeathSubEmitter], emitter.onDeathSubEmitter, p.position, count = 4)
                }
                pool.release(p)
                continue
            }

            val progress = p.getLifeProgress()
            val emitter = effect.emitters.getOrNull(p.emitterIndex) ?: effect.emitters.firstOrNull()

            if (emitter != null) {
                // Module: Gravity & Damping
                val gravityMod = emitter.findGravityModule()
                if (gravityMod != null) {
                    p.velocity.y += gravityMod.gravity * dt * 60f
                    val dampFactor = (1.0f - (1.0f - gravityMod.damping) * dt).coerceIn(0.1f, 1f)
                    p.velocity.x *= dampFactor
                }

                // Module: Scale Over Lifetime
                val scaleMod = emitter.findScaleModule()
                if (scaleMod != null) {
                    val scaleFactor = scaleMod.scaleCurve.evaluate(progress)
                    p.scale.x = p.baseScale.x * scaleFactor
                    p.scale.y = p.baseScale.y * scaleFactor
                }

                // Module: Color Over Lifetime
                val colorMod = emitter.findColorModule()
                if (colorMod != null) {
                    val evaluatedColor = colorMod.colorGradient.evaluate(progress)
                    p.color.r = evaluatedColor.r
                    p.color.g = evaluatedColor.g
                    p.color.b = evaluatedColor.b
                    p.color.a = evaluatedColor.a
                }

                // Module: Alpha Over Lifetime
                val alphaMod = emitter.findAlphaModule()
                if (alphaMod != null) {
                    p.alpha = alphaMod.alphaCurve.evaluate(progress)
                }

                // Module: Turbulence (Curl or Perlin Noise)
                val turbMod = emitter.findTurbulenceModule()
                if (turbMod != null && turbMod.strength > 0f) {
                    val nx = (p.position.x * turbMod.frequency) + (currentTime * turbMod.scrollSpeed)
                    val ny = (p.position.y * turbMod.frequency) + (currentTime * turbMod.scrollSpeed)
                    if (turbMod.useCurlNoise) {
                        val curl = VFXNoise.curlNoise2D(nx, ny)
                        p.velocity.x += curl.x * turbMod.strength * dt * 30f
                        p.velocity.y += curl.y * turbMod.strength * dt * 30f
                    } else {
                        val noise = VFXNoise.perlin2D(nx, ny)
                        p.velocity.x += cos(noise * PI.toFloat() * 2f) * turbMod.strength * dt * 30f
                        p.velocity.y += sin(noise * PI.toFloat() * 2f) * turbMod.strength * dt * 30f
                    }
                }

                // Module: Drag & Friction
                val dragMod = emitter.findDragModule()
                if (dragMod != null) {
                    val speed = p.velocity.length()
                    val drag = (dragMod.linearDrag + dragMod.quadraticDrag * speed) * dt
                    val factor = max(0f, 1f - drag)
                    p.velocity.x *= factor
                    p.velocity.y *= factor
                }

                // Module: Vortex & Swirl
                val vortexMod = emitter.findVortexModule()
                if (vortexMod != null && vortexMod.vortexStrength != 0f) {
                    val dx = p.position.x - vortexMod.center.x
                    val dy = p.position.y - vortexMod.center.y
                    val dist = max(10f, sqrt(dx * dx + dy * dy))
                    val tanX = -dy / dist
                    val tanY = dx / dist
                    val radX = -dx / dist
                    val radY = -dy / dist

                    p.velocity.x += (tanX * vortexMod.vortexStrength + radX * vortexMod.radialPull) * dt * 10f
                    p.velocity.y += (tanY * vortexMod.vortexStrength + radY * vortexMod.radialPull) * dt * 10f
                }

                // Module: Attractor (Gravity Well)
                val attrMod = emitter.findAttractorModule()
                if (attrMod != null && attrMod.strength != 0f) {
                    val dx = attrMod.targetPosition.x - p.position.x
                    val dy = attrMod.targetPosition.y - p.position.y
                    val dist = max(15f, sqrt(dx * dx + dy * dy))
                    val pull = (attrMod.strength * 1000f) / (dist * dist + 200f)
                    p.velocity.x += (dx / dist) * pull * dt
                    p.velocity.y += (dy / dist) * pull * dt
                }

                // Module: Collision with Ground / Floor Plane
                val collMod = emitter.findCollisionModule()
                if (collMod != null) {
                    if (p.position.y >= collMod.floorY && p.velocity.y > 0f) {
                        p.position.y = collMod.floorY
                        p.velocity.y = -p.velocity.y * collMod.restitution
                        p.velocity.x *= collMod.friction

                        // Trigger onCollision Sub-Emitter
                        if (emitter.onCollisionSubEmitter in 0 until effect.emitters.size) {
                            triggerSubEmitter(effect.emitters[emitter.onCollisionSubEmitter], emitter.onCollisionSubEmitter, p.position, count = 2)
                        }
                    }
                }

                // Module: Flipbook Sprite Sheet Frame Animation
                val flipMod = emitter.findFlipbookModule()
                if (flipMod != null && flipMod.columns > 0 && flipMod.rows > 0) {
                    p.frameTimer += dt
                    val frameDuration = if (flipMod.frameRate > 0f) 1f / flipMod.frameRate else 0.033f
                    if (p.frameTimer >= frameDuration) {
                        p.frameTimer %= frameDuration
                        p.frameIndex = if (flipMod.loop) (p.frameIndex + 1) % flipMod.totalFrames else min(p.frameIndex + 1, flipMod.totalFrames - 1)
                    }

                    val col = p.frameIndex % flipMod.columns
                    val row = p.frameIndex / flipMod.columns
                    val frameUWidth = 1f / flipMod.columns.toFloat()
                    val frameVHeight = 1f / flipMod.rows.toFloat()
                    p.textureRect.uvX = col * frameUWidth
                    p.textureRect.uvY = row * frameVHeight
                    p.textureRect.uvWidth = frameUWidth
                    p.textureRect.uvHeight = frameVHeight
                }

                // Module: Velocity Alignment (Stretching Billboard along velocity vector)
                val alignMod = emitter.findVelocityAlignmentModule()
                if (alignMod != null && alignMod.stretchFactor > 0f) {
                    val speed = p.velocity.length()
                    if (speed > 1f) {
                        p.rotation = Math.toDegrees(atan2(p.velocity.y.toDouble(), p.velocity.x.toDouble())).toFloat()
                        p.stretch = 1f + speed * alignMod.stretchFactor
                    } else {
                        p.stretch = 1f
                    }
                } else {
                    p.stretch = 1f
                }

                // Module: Color By Speed
                val speedColorMod = emitter.findColorBySpeedModule()
                if (speedColorMod != null) {
                    val currentSpeed = p.velocity.length()
                    val speedSpan = max(0.1f, speedColorMod.maxSpeed - speedColorMod.minSpeed)
                    val speedNorm = ((currentSpeed - speedColorMod.minSpeed) / speedSpan).coerceIn(0f, 1f)
                    val speedCol = speedColorMod.gradient.evaluate(speedNorm)
                    p.color.r = speedCol.r
                    p.color.g = speedCol.g
                    p.color.b = speedCol.b
                    p.color.a = speedCol.a
                }

                // Module: Trail Ribbon Points
                val trailMod = emitter.findTrailModule()
                if (trailMod != null) {
                    p.trailTimer += dt
                    if (p.trailTimer >= trailMod.segmentInterval) {
                        p.trailTimer = 0f
                        p.trails.add(
                            TrailPoint(
                                position = p.position.copy(),
                                age = 0f,
                                width = p.scale.x * 0.75f,
                                color = p.color.copy()
                            )
                        )
                        if (p.trails.size > trailMod.maxPoints) {
                            p.trails.removeAt(0)
                        }
                    }

                    // Update trail points lifetime
                    val it = p.trails.iterator()
                    while (it.hasNext()) {
                        val tp = it.next()
                        tp.age += dt
                        if (tp.age >= trailMod.trailLifetime) {
                            it.remove()
                        }
                    }
                } else {
                    p.trails.clear()
                }
            }

            // Update Position & Rotation
            p.position.x += p.velocity.x * dt
            p.position.y += p.velocity.y * dt
            p.rotation += p.angularVelocity * dt
        }

        // 3. Compute Bounds
        bounds = effect.calculateBounds(pool.particles)
    }

    private fun spawnParticle(emitter: VFXEmitter, emitterIndex: Int = 0, origin: Vector2? = null) {
        val p = pool.acquire() ?: return

        p.reset()
        p.emitterIndex = emitterIndex
        p.lifetime = (emitter.particleLifetime * (0.85f + Random.nextFloat() * 0.3f)).coerceAtLeast(0.1f)
        p.age = 0f
        p.isActive = true

        // 1. Calculate Spawn Origin based on ShapeType
        if (origin != null) {
            p.position.x = origin.x
            p.position.y = origin.y
        } else {
            when (emitter.shapeType) {
                ShapeType.POINT -> {
                    p.position.x = 0f
                    p.position.y = 0f
                }
                ShapeType.CIRCLE -> {
                    val angle = Random.nextFloat() * 2f * PI.toFloat()
                    val radius = sqrt(Random.nextFloat()) * emitter.shapeSize.x
                    p.position.x = cos(angle) * radius
                    p.position.y = sin(angle) * radius
                }
                ShapeType.RING -> {
                    val angle = Random.nextFloat() * 2f * PI.toFloat()
                    val innerR = emitter.shapeSize.x * 0.7f
                    val outerR = emitter.shapeSize.x
                    val radius = innerR + Random.nextFloat() * (outerR - innerR)
                    p.position.x = cos(angle) * radius
                    p.position.y = sin(angle) * radius
                }
                ShapeType.RECTANGLE -> {
                    p.position.x = (Random.nextFloat() - 0.5f) * emitter.shapeSize.x * 2f
                    p.position.y = (Random.nextFloat() - 0.5f) * emitter.shapeSize.y * 2f
                }
                ShapeType.LINE -> {
                    val offset = (Random.nextFloat() - 0.5f) * emitter.shapeSize.x * 2f
                    p.position.x = offset
                    p.position.y = 0f
                }
                ShapeType.CONE -> {
                    val angle = (Random.nextFloat() - 0.5f) * Math.toRadians(emitter.spreadAngle.toDouble()).toFloat()
                    val dist = Random.nextFloat() * emitter.shapeSize.x
                    p.position.x = sin(angle) * dist
                    p.position.y = -cos(angle) * dist
                }
            }
        }

        // 2. Calculate Initial Velocity and Direction
        val finalAngle = if (emitter.spreadAngle >= 350f) {
            Random.nextFloat() * 2f * PI.toFloat()
        } else {
            val spreadRad = Math.toRadians(emitter.spreadAngle.toDouble()).toFloat()
            val baseAngle = -PI.toFloat() / 2f // Upward default (-90 deg)
            val angleOffset = (Random.nextFloat() - 0.5f) * spreadRad
            baseAngle + angleOffset
        }

        val speed = emitter.speedMin + Random.nextFloat() * (emitter.speedMax - emitter.speedMin).coerceAtLeast(0f)
        p.velocity.x = cos(finalAngle) * speed
        p.velocity.y = sin(finalAngle) * speed

        // 3. Scale & Color
        val scaleX = emitter.baseScaleMin.x + Random.nextFloat() * (emitter.baseScaleMax.x - emitter.baseScaleMin.x).coerceAtLeast(0f)
        val scaleY = emitter.baseScaleMin.y + Random.nextFloat() * (emitter.baseScaleMax.y - emitter.baseScaleMin.y).coerceAtLeast(0f)
        p.baseScale.x = scaleX
        p.baseScale.y = scaleY
        p.scale.x = scaleX
        p.scale.y = scaleY

        p.rotation = Random.nextFloat() * 360f
        p.angularVelocity = (Random.nextFloat() - 0.5f) * 180f

        p.textureRect.uvX = emitter.textureUVRect.uvX
        p.textureRect.uvY = emitter.textureUVRect.uvY
        p.textureRect.uvWidth = emitter.textureUVRect.uvWidth
        p.textureRect.uvHeight = emitter.textureUVRect.uvHeight

        // Trigger onBirth Sub-Emitter
        if (emitter.onBirthSubEmitter in 0 until effect.emitters.size) {
            triggerSubEmitter(effect.emitters[emitter.onBirthSubEmitter], emitter.onBirthSubEmitter, p.position, count = 1)
        }
    }

    private fun triggerSubEmitter(subEmitter: VFXEmitter, subEmitterIdx: Int, position: Vector2, count: Int = 1) {
        for (i in 0 until count) {
            spawnParticle(subEmitter, subEmitterIdx, origin = position)
        }
    }
}
