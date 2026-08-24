package com.example.data

import com.example.model.vfx.*

/**
 * Production-Ready VFX Presets for Korva VFX Engine 2.0.
 * Showcases all 16 modules including Vortex, Curl Turbulence, Trails, Collision, Velocity Alignment, and Sub-Emitters.
 */
object VFXPresets {

    /**
     * Cosmic Vortex Portal (Example 1 from C++ API Reference)
     */
    fun createCosmicVortex(): VFXEffect {
        val effect = VFXEffect(
            name = "Cosmic Vortex Portal",
            effectId = "fx_cosmic_vortex",
            version = "1.10",
            duration = 4.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE,
            timeScale = 1.0f
        )

        val swirlEmitter = VFXEmitter(
            name = "StarsSwirl",
            shapeType = ShapeType.RING,
            shapeSize = Vector2(80f, 20f),
            spawnRate = 120.0f,
            particleLifetime = 2.2f,
            speedMin = 10.0f,
            speedMax = 40.0f,
            spreadAngle = 360.0f,
            baseScaleMin = Vector2(4f, 4f),
            baseScaleMax = Vector2(10f, 10f)
        )

        // Vortex Module: Angular Swirl + Inward Radial Pull
        swirlEmitter.modules.add(VortexModule(center = Vector2(0f, 0f), vortexStrength = 75.0f, radialPull = 25.0f))

        // Curl Turbulence
        swirlEmitter.modules.add(TurbulenceModule(strength = 18.0f, frequency = 0.08f, scrollSpeed = 1.2f, useCurlNoise = true))

        // Scale Curve
        val scaleCurve = VFXCurve(interpolation = InterpolationType.CUBIC).apply {
            addKeyframe(0.0f, 0.2f)
            addKeyframe(0.5f, 1.4f)
            addKeyframe(1.0f, 0.0f)
        }
        swirlEmitter.modules.add(ScaleModule(scaleCurve))

        // Cosmic Color Gradient (Cyan -> Neon Violet -> Magenta)
        val colorGrad = VFXGradient().apply {
            addKey(0.0f, ColorRGBA(0, 240, 255, 1.0f))
            addKey(0.5f, ColorRGBA(180, 50, 255, 0.9f))
            addKey(1.0f, ColorRGBA(255, 0, 150, 0.0f))
        }
        swirlEmitter.modules.add(ColorModule(colorGrad))

        // Ribbon Trails
        swirlEmitter.modules.add(TrailModule(segmentInterval = 0.025f, trailLifetime = 0.35f, maxPoints = 12))

        effect.emitters.add(swirlEmitter)
        return effect
    }

    /**
     * Meteor Strike with Sparks, Ground Collision, and Debris
     */
    fun createMeteorStrike(): VFXEffect {
        val effect = VFXEffect(
            name = "Meteor Strike & Debris",
            effectId = "fx_meteor_strike",
            version = "1.10",
            duration = 3.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE,
            timeScale = 1.0f
        )

        // Emitter 0: Falling Meteor Core
        val meteorEmitter = VFXEmitter(
            name = "FallingMeteors",
            shapeType = ShapeType.LINE,
            shapeSize = Vector2(100f, 0f),
            spawnRate = 25.0f,
            particleLifetime = 1.6f,
            speedMin = 120.0f,
            speedMax = 220.0f,
            spreadAngle = 30.0f,
            baseScaleMin = Vector2(8f, 8f),
            baseScaleMax = Vector2(14f, 14f),
            onCollisionSubEmitter = 1 // Spawn impact sparks on collision
        )

        meteorEmitter.modules.add(GravityModule(gravity = 6.0f, damping = 0.98f))
        meteorEmitter.modules.add(CollisionModule(floorY = 140f, restitution = 0.5f, friction = 0.7f))
        meteorEmitter.modules.add(VelocityAlignmentModule(stretchFactor = 0.035f))
        meteorEmitter.modules.add(TrailModule(segmentInterval = 0.02f, trailLifetime = 0.4f, maxPoints = 16))

        val meteorColor = VFXGradient().apply {
            addKey(0.0f, ColorRGBA(255, 240, 180, 1.0f))
            addKey(0.4f, ColorRGBA(255, 120, 20, 0.9f))
            addKey(1.0f, ColorRGBA(180, 20, 0, 0.0f))
        }
        meteorEmitter.modules.add(ColorModule(meteorColor))
        effect.emitters.add(meteorEmitter)

        // Emitter 1: Ground Impact Spark Debris (Sub-Emitter)
        val impactSparks = VFXEmitter(
            name = "ImpactSparks",
            shapeType = ShapeType.POINT,
            shapeSize = Vector2(0f, 0f),
            spawnRate = 0f, // Only triggered on collision
            burstCount = 0,
            particleLifetime = 0.6f,
            speedMin = 60f,
            speedMax = 150f,
            spreadAngle = 160f,
            baseScaleMin = Vector2(3f, 3f),
            baseScaleMax = Vector2(6f, 6f)
        )
        impactSparks.modules.add(GravityModule(gravity = 4.0f, damping = 0.95f))
        impactSparks.modules.add(DragModule(linearDrag = 0.4f, quadraticDrag = 0.02f))
        val sparkGrad = VFXGradient().apply {
            addKey(0.0f, ColorRGBA(255, 255, 120, 1.0f))
            addKey(0.5f, ColorRGBA(255, 100, 0, 0.8f))
            addKey(1.0f, ColorRGBA(80, 0, 0, 0.0f))
        }
        impactSparks.modules.add(ColorModule(sparkGrad))
        effect.emitters.add(impactSparks)

        return effect
    }

    /**
     * Electric Arc Storm
     */
    fun createElectricArcStorm(): VFXEffect {
        val effect = VFXEffect(
            name = "Electric Arc Storm",
            effectId = "fx_electric_arc",
            version = "1.10",
            duration = 2.5f,
            looping = true,
            blendMode = BlendMode.ADDITIVE,
            timeScale = 1.0f
        )

        val arcEmitter = VFXEmitter(
            name = "LightningArcs",
            shapeType = ShapeType.CIRCLE,
            shapeSize = Vector2(25f, 25f),
            spawnRate = 90f,
            particleLifetime = 0.8f,
            speedMin = 80f,
            speedMax = 200f,
            spreadAngle = 360f,
            baseScaleMin = Vector2(4f, 4f),
            baseScaleMax = Vector2(10f, 10f)
        )

        arcEmitter.modules.add(TurbulenceModule(strength = 45f, frequency = 0.15f, scrollSpeed = 3.0f, useCurlNoise = true))
        arcEmitter.modules.add(VelocityAlignmentModule(stretchFactor = 0.04f))

        val electricGrad = VFXGradient().apply {
            addKey(0.0f, ColorRGBA(255, 255, 255, 1.0f))
            addKey(0.3f, ColorRGBA(100, 220, 255, 1.0f))
            addKey(0.8f, ColorRGBA(40, 100, 255, 0.7f))
            addKey(1.0f, ColorRGBA(0, 20, 150, 0.0f))
        }
        arcEmitter.modules.add(ColorBySpeedModule(electricGrad, minSpeed = 50f, maxSpeed = 220f))
        arcEmitter.modules.add(TrailModule(segmentInterval = 0.015f, trailLifetime = 0.2f, maxPoints = 10))

        effect.emitters.add(arcEmitter)
        return effect
    }

    /**
     * Classic Fire & Heavy Smoke Explosion
     */
    fun createFireExplosion(): VFXEffect {
        val effect = VFXEffect(
            name = "Fire Explosion",
            effectId = "fx_fire_exp_001",
            version = "1.10",
            duration = 2.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE
        )

        // Emitter 1: SmokeEmitter
        val smokeEmitter = VFXEmitter(
            name = "SmokeEmitter",
            shapeType = ShapeType.CIRCLE,
            shapeSize = Vector2(18f, 18f),
            spawnRate = 45.0f,
            particleLifetime = 2.0f,
            speedMin = 15.0f,
            speedMax = 50.0f,
            spreadAngle = 60.0f,
            baseScaleMin = Vector2(16f, 16f),
            baseScaleMax = Vector2(28f, 28f),
            textureAtlas = "vfx_atlas.png",
            textureUVRect = TextureRect(0f, 0f, 0.25f, 0.25f)
        )

        val smokeScaleCurve = VFXCurve(interpolation = InterpolationType.CUBIC).apply {
            addKeyframe(0.0f, 0.2f)
            addKeyframe(0.3f, 1.0f)
            addKeyframe(0.7f, 0.8f)
            addKeyframe(1.0f, 0.0f)
        }
        smokeEmitter.modules.add(ScaleModule(smokeScaleCurve))

        val smokeAlphaCurve = VFXCurve(interpolation = InterpolationType.LINEAR).apply {
            addKeyframe(0.0f, 0.8f)
            addKeyframe(0.25f, 1.0f)
            addKeyframe(1.0f, 0.0f)
        }
        smokeEmitter.modules.add(AlphaModule(smokeAlphaCurve))
        smokeEmitter.modules.add(GravityModule(gravity = -1.5f, damping = 0.98f))
        smokeEmitter.modules.add(TurbulenceModule(strength = 15f, frequency = 0.04f, scrollSpeed = 0.8f, useCurlNoise = true))

        val smokeColorGrad = VFXGradient(interpolation = InterpolationType.LINEAR).apply {
            addKey(0.0f, ColorRGBA(255, 120, 30, 0.9f))
            addKey(0.4f, ColorRGBA(80, 80, 90, 0.7f))
            addKey(1.0f, ColorRGBA(30, 30, 35, 0.0f))
        }
        smokeEmitter.modules.add(ColorModule(smokeColorGrad))
        effect.emitters.add(smokeEmitter)

        // Emitter 2: SparkEmitter
        val sparkEmitter = VFXEmitter(
            name = "SparkEmitter",
            shapeType = ShapeType.POINT,
            shapeSize = Vector2(0f, 0f),
            spawnRate = 120.0f,
            particleLifetime = 1.0f,
            speedMin = 60.0f,
            speedMax = 180.0f,
            spreadAngle = 360.0f,
            baseScaleMin = Vector2(3f, 3f),
            baseScaleMax = Vector2(7f, 7f),
            textureAtlas = "vfx_atlas.png",
            textureUVRect = TextureRect(0.25f, 0f, 0.25f, 0.25f)
        )

        sparkEmitter.modules.add(GravityModule(gravity = 2.2f, damping = 0.95f))
        sparkEmitter.modules.add(DragModule(linearDrag = 0.5f, quadraticDrag = 0.015f))
        sparkEmitter.modules.add(VelocityAlignmentModule(stretchFactor = 0.025f))

        val sparkGrad = VFXGradient(interpolation = InterpolationType.LINEAR).apply {
            addKey(0.0f, ColorRGBA(255, 255, 255, 1.0f))
            addKey(0.25f, ColorRGBA(255, 255, 0, 1.0f))
            addKey(0.5f, ColorRGBA(255, 165, 0, 1.0f))
            addKey(0.75f, ColorRGBA(255, 69, 0, 0.8f))
            addKey(1.0f, ColorRGBA(100, 0, 0, 0.0f))
        }
        sparkEmitter.modules.add(ColorModule(sparkGrad))
        effect.emitters.add(sparkEmitter)

        return effect
    }

    /**
     * Magic Sparkles with Star Attractor Field
     */
    fun createMagicSparkles(): VFXEffect {
        val effect = VFXEffect(
            name = "Magic Sparkles",
            effectId = "fx_cosmic_002",
            version = "1.10",
            duration = 3.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE
        )

        val emitter = VFXEmitter(
            name = "StarField",
            shapeType = ShapeType.RING,
            shapeSize = Vector2(50f, 50f),
            spawnRate = 80f,
            particleLifetime = 1.8f,
            speedMin = 10f,
            speedMax = 35f,
            spreadAngle = 360f,
            baseScaleMin = Vector2(4f, 4f),
            baseScaleMax = Vector2(10f, 10f)
        )

        val colorGrad = VFXGradient().apply {
            addKey(0.0f, ColorRGBA(160, 230, 255, 1.0f))
            addKey(0.5f, ColorRGBA(200, 120, 255, 0.9f))
            addKey(1.0f, ColorRGBA(120, 40, 255, 0.0f))
        }
        emitter.modules.add(ColorModule(colorGrad))

        val scaleCurve = VFXCurve(interpolation = InterpolationType.CUBIC).apply {
            addKeyframe(0.0f, 0.0f)
            addKeyframe(0.5f, 1.2f)
            addKeyframe(1.0f, 0.0f)
        }
        emitter.modules.add(ScaleModule(scaleCurve))
        emitter.modules.add(AttractorModule(targetPosition = Vector2(0f, 0f), strength = 30f))
        emitter.modules.add(TurbulenceModule(strength = 10f, frequency = 0.06f, scrollSpeed = 0.8f, useCurlNoise = true))

        effect.emitters.add(emitter)
        return effect
    }

    /**
     * Neon Energy Slash
     */
    fun createEnergySlash(): VFXEffect {
        val effect = VFXEffect(
            name = "Neon Slash",
            effectId = "fx_slash_003",
            version = "1.10",
            duration = 1.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE
        )

        val emitter = VFXEmitter(
            name = "SlashArc",
            shapeType = ShapeType.LINE,
            shapeSize = Vector2(60f, 0f),
            spawnRate = 160f,
            particleLifetime = 0.6f,
            speedMin = 40f,
            speedMax = 120f,
            spreadAngle = 30f,
            baseScaleMin = Vector2(6f, 6f),
            baseScaleMax = Vector2(14f, 14f)
        )

        val colorGrad = VFXGradient().apply {
            addKey(0.0f, ColorRGBA(100, 255, 220, 1.0f))
            addKey(0.4f, ColorRGBA(0, 200, 255, 0.9f))
            addKey(1.0f, ColorRGBA(0, 50, 180, 0.0f))
        }
        emitter.modules.add(ColorModule(colorGrad))
        emitter.modules.add(VelocityAlignmentModule(stretchFactor = 0.04f))
        emitter.modules.add(TrailModule(segmentInterval = 0.02f, trailLifetime = 0.25f, maxPoints = 8))

        effect.emitters.add(emitter)
        return effect
    }

    /**
     * Gold Coin Burst with Ground Bouncing
     */
    fun createGoldCoinBurst(): VFXEffect {
        val effect = VFXEffect(
            name = "Gold Coin Burst",
            effectId = "fx_gold_004",
            version = "1.10",
            duration = 1.5f,
            looping = true,
            blendMode = BlendMode.NORMAL
        )

        val emitter = VFXEmitter(
            name = "CoinBurst",
            shapeType = ShapeType.CIRCLE,
            shapeSize = Vector2(10f, 10f),
            spawnRate = 60f,
            particleLifetime = 1.4f,
            speedMin = 80f,
            speedMax = 200f,
            spreadAngle = 360f,
            baseScaleMin = Vector2(10f, 10f),
            baseScaleMax = Vector2(16f, 16f)
        )

        val colorGrad = VFXGradient().apply {
            addKey(0.0f, ColorRGBA(255, 235, 100, 1.0f))
            addKey(0.6f, ColorRGBA(255, 180, 0, 1.0f))
            addKey(1.0f, ColorRGBA(200, 120, 0, 0.0f))
        }
        emitter.modules.add(ColorModule(colorGrad))
        emitter.modules.add(GravityModule(gravity = 3.5f, damping = 0.96f))
        emitter.modules.add(CollisionModule(floorY = 120f, restitution = 0.65f, friction = 0.85f))
        effect.emitters.add(emitter)
        return effect
    }

    fun createEmptyBlankEffect(name: String = "Custom VFX"): VFXEffect {
        val effect = VFXEffect(
            name = name,
            effectId = "fx_custom_${System.currentTimeMillis() % 10000}",
            version = "1.10",
            duration = 2.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE,
            timeScale = 1.0f
        )

        val baseEmitter = VFXEmitter(
            name = "CoreEmitter",
            shapeType = ShapeType.CIRCLE,
            shapeSize = Vector2(10f, 10f),
            spawnRate = 60f,
            particleLifetime = 1.2f,
            speedMin = 30f,
            speedMax = 90f,
            spreadAngle = 360f,
            baseScaleMin = Vector2(8f, 8f),
            baseScaleMax = Vector2(16f, 16f)
        )
        baseEmitter.modules.add(GravityModule(gravity = 0f, damping = 0.98f))
        baseEmitter.modules.add(ScaleModule(VFXCurve(interpolation = InterpolationType.CUBIC).apply {
            addKeyframe(0.0f, 0.2f)
            addKeyframe(0.3f, 1.2f)
            addKeyframe(1.0f, 0.0f)
        }))
        baseEmitter.modules.add(AlphaModule(VFXCurve(interpolation = InterpolationType.LINEAR).apply {
            addKeyframe(0.0f, 1.0f)
            addKeyframe(0.7f, 0.9f)
            addKeyframe(1.0f, 0.0f)
        }))
        baseEmitter.modules.add(ColorModule(VFXGradient().apply {
            addKey(0.0f, ColorRGBA(100, 220, 255, 1.0f))
            addKey(1.0f, ColorRGBA(255, 60, 180, 0.0f))
        }))
        effect.emitters.add(baseEmitter)
        return effect
    }

    val ALL_PRESETS = listOf(
        createCosmicVortex(),
        createMeteorStrike(),
        createElectricArcStorm(),
        createFireExplosion(),
        createMagicSparkles(),
        createEnergySlash(),
        createGoldCoinBurst()
    )
}
