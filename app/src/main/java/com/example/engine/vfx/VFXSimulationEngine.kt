package com.example.engine.vfx

import com.example.model.vfx.*
import kotlin.math.*
import kotlin.random.Random

/**
 * High-Performance VFX Simulation Engine.
 * Supports hybrid native JNI acceleration (when libkorva_vfx.so is present)
 * and ultra-optimized CPU particle simulation.
 */
class VFXSimulationEngine(
    var effect: VFXEffect = VFXEffect(),
    poolCapacity: Int = 1200
) {
    val pool = VFXParticlePool(poolCapacity)
    private val emitterSpawnAccumulators = mutableMapOf<String, Float>()
    private val emitterBurstTimers = mutableMapOf<String, Float>()
    var currentTime: Float = 0f
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
                // Ignore fallback
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
     * Advance simulation by deltaTime seconds.
     */
    fun update(deltaTime: Float) {
        if (deltaTime <= 0f) return
        val dt = minOf(deltaTime, 0.05f) // Clamp delta to avoid physics explosions
        currentTime += dt

        if (effect.duration > 0f && currentTime >= effect.duration) {
            if (effect.looping) {
                currentTime %= effect.duration
            }
        }

        // If native C++ engine is active, forward update
        if (com.korva.engine.VFXNativeBridge.isNativeLoaded) {
            try {
                com.korva.engine.VFXNativeBridge.updateEffect(dt)
            } catch (e: Throwable) {
                // Fallback to Kotlin physics
            }
        }

        // 1. Spawning Phase
        for (emitter in effect.emitters) {
            val emitterKey = emitter.id

            // Handle Continuous Spawning
            if (emitter.spawnRate > 0f) {
                val acc = (emitterSpawnAccumulators[emitterKey] ?: 0f) + emitter.spawnRate * dt
                val spawnCount = acc.toInt()
                emitterSpawnAccumulators[emitterKey] = acc - spawnCount

                for (i in 0 until spawnCount) {
                    spawnParticle(emitter)
                }
            }

            // Handle Burst Spawning
            if (emitter.burstCount > 0 && emitter.burstInterval > 0f) {
                val timer = (emitterBurstTimers[emitterKey] ?: 0f) + dt
                if (timer >= emitter.burstInterval) {
                    emitterBurstTimers[emitterKey] = timer - emitter.burstInterval
                    for (i in 0 until emitter.burstCount) {
                        spawnParticle(emitter)
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
                pool.release(p)
                continue
            }

            val progress = p.getLifeProgress()

            // Find emitter modules
            val emitter = effect.emitters.firstOrNull { it.textureAtlas == p.textureRect.toString() }
                ?: effect.emitters.firstOrNull()

            if (emitter != null) {
                // Apply Gravity Module
                val gravityMod = emitter.findGravityModule()
                if (gravityMod != null) {
                    p.velocity.y += gravityMod.gravity * dt * 60f
                    p.velocity.x *= (1.0f - (1.0f - gravityMod.damping) * dt)
                }

                // Apply Scale Curve Module
                val scaleMod = emitter.findScaleModule()
                if (scaleMod != null) {
                    val scaleFactor = scaleMod.scaleCurve.evaluate(progress)
                    p.scale.x = p.baseScale.x * scaleFactor
                    p.scale.y = p.baseScale.y * scaleFactor
                }

                // Apply Color Gradient Module
                val colorMod = emitter.findColorModule()
                if (colorMod != null) {
                    val evaluatedColor = colorMod.colorGradient.evaluate(progress)
                    p.color.r = evaluatedColor.r
                    p.color.g = evaluatedColor.g
                    p.color.b = evaluatedColor.b
                    p.color.a = evaluatedColor.a
                }

                // Apply Alpha Curve Module
                val alphaMod = emitter.findAlphaModule()
                if (alphaMod != null) {
                    val alphaVal = alphaMod.alphaCurve.evaluate(progress)
                    p.alpha = alphaVal
                }
            }

            // Update Position & Rotation
            p.position.x += p.velocity.x * dt
            p.position.y += p.velocity.y * dt
            p.rotation += p.angularVelocity * dt
        }
    }

    private fun spawnParticle(emitter: VFXEmitter) {
        val p = pool.acquire() ?: return

        p.reset()
        p.lifetime = (emitter.particleLifetime * (0.8f + Random.nextFloat() * 0.4f)).coerceAtLeast(0.1f)
        p.age = 0f
        p.isActive = true

        // 1. Calculate Spawn Origin based on ShapeType
        when (emitter.shapeType) {
            ShapeType.POINT -> {
                p.position.x = 0f
                p.position.y = 0f
            }
            ShapeType.CIRCLE -> {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val radius = sqrt(Random.nextFloat()) * emitter.shapeSize.x
                p.position.x = cos(angle) * radius
                p.position.y = sin(angle) * radius
            }
            ShapeType.RING -> {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
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
        }

        // 2. Calculate Initial Velocity and Direction
        val spreadRad = Math.toRadians(emitter.spreadAngle.toDouble()).toFloat()
        val baseAngle = -Math.PI.toFloat() / 2f // Upward default (-90 deg)
        val angleOffset = (Random.nextFloat() - 0.5f) * spreadRad
        val finalAngle = baseAngle + angleOffset

        val speed = emitter.speedMin + Random.nextFloat() * (emitter.speedMax - emitter.speedMin)
        p.velocity.x = cos(finalAngle) * speed
        p.velocity.y = sin(finalAngle) * speed

        // 3. Scale & Color
        val scaleX = emitter.baseScaleMin.x + Random.nextFloat() * (emitter.baseScaleMax.x - emitter.baseScaleMin.x)
        val scaleY = emitter.baseScaleMin.y + Random.nextFloat() * (emitter.baseScaleMax.y - emitter.baseScaleMin.y)
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
    }
}
