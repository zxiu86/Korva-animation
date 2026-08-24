package com.example.data

import com.example.model.vfx.*

object VFXPresets {

    fun createFireExplosion(): VFXEffect {
        val effect = VFXEffect(
            name = "FireExplosion",
            effectId = "fx_fire_exp_001",
            version = "1.0",
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

        sparkEmitter.modules.add(GravityModule(gravity = 1.8f, damping = 0.95f))

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

    fun createMagicSparkles(): VFXEffect {
        val effect = VFXEffect(
            name = "CosmicSparkles",
            effectId = "fx_cosmic_002",
            version = "1.0",
            duration = 3.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE
        )

        val emitter = VFXEmitter(
            name = "StarField",
            shapeType = ShapeType.RING,
            shapeSize = Vector2(40f, 40f),
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
        emitter.modules.add(GravityModule(gravity = -0.5f, damping = 0.99f))

        effect.emitters.add(emitter)
        return effect
    }

    fun createEnergySlash(): VFXEffect {
        val effect = VFXEffect(
            name = "NeonSlash",
            effectId = "fx_slash_003",
            version = "1.0",
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
        effect.emitters.add(emitter)
        return effect
    }

    fun createGoldCoinBurst(): VFXEffect {
        val effect = VFXEffect(
            name = "GoldBurst",
            effectId = "fx_gold_004",
            version = "1.0",
            duration = 1.5f,
            looping = true,
            blendMode = BlendMode.NORMAL
        )

        val emitter = VFXEmitter(
            name = "CoinBurst",
            shapeType = ShapeType.CIRCLE,
            shapeSize = Vector2(10f, 10f),
            spawnRate = 60f,
            particleLifetime = 1.2f,
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
        effect.emitters.add(emitter)
        return effect
    }

    fun createEmptyBlankEffect(name: String = "Custom VFX"): VFXEffect {
        val effect = VFXEffect(
            name = name,
            effectId = "fx_custom_${System.currentTimeMillis() % 10000}",
            version = "1.0",
            duration = 2.0f,
            looping = true,
            blendMode = BlendMode.ADDITIVE
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
        createFireExplosion(),
        createMagicSparkles(),
        createEnergySlash(),
        createGoldCoinBurst()
    )
}
