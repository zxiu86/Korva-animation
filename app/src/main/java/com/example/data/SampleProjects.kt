package com.example.data

import com.example.model.*
import java.util.UUID

object SampleProjects {

    fun getSamples(): List<KorProject> = listOf(
        createKnightAttackProject(),
        createSlimeBounceProject(),
        createCoinSpinProject(),
        createSlashFxProject()
    )

    fun createDefaultProject(): KorProject {
        return createKnightAttackProject()
    }

    private fun createKnightAttackProject(): KorProject {
        val swordLayer = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Hero Blade",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.SWORD,
            width = 32f,
            height = 110f,
            zIndex = 3,
            pivotX = 0.5f,
            pivotY = 0.85f,
            shapeStyle = ShapeStyle(fillColor = 0xFFA855F7, strokeColor = 0xFFFFFFFF, strokeWidth = 2.5f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 60f, y = 10f, rotation = -20f, scaleX = 1f, scaleY = 1f, easing = EasingType.BACK_IN_OUT),
                Keyframe(frame = 8, x = 40f, y = -10f, rotation = -60f, scaleX = 1.05f, scaleY = 1.05f, easing = EasingType.EASE_IN_QUAD),
                Keyframe(frame = 16, x = 110f, y = 30f, rotation = 75f, scaleX = 1.2f, scaleY = 1.2f, easing = EasingType.BOUNCE_OUT),
                Keyframe(frame = 24, x = 80f, y = 20f, rotation = 20f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 32, x = 60f, y = 10f, rotation = -20f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC)
            )
        )

        val slashArcLayer = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Slash Wave FX",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.SLASH_FX,
            width = 140f,
            height = 140f,
            zIndex = 4,
            shapeStyle = ShapeStyle(fillColor = 0xFFC084FC, strokeColor = 0xFFFFFFFF, strokeWidth = 2f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 100f, y = 20f, rotation = 0f, scaleX = 0.2f, scaleY = 0.2f, opacity = 0f, easing = EasingType.LINEAR),
                Keyframe(frame = 12, x = 90f, y = 10f, rotation = -30f, scaleX = 0.4f, scaleY = 0.4f, opacity = 0.2f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 16, x = 120f, y = 25f, rotation = 45f, scaleX = 1.3f, scaleY = 1.3f, opacity = 1f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 22, x = 140f, y = 35f, rotation = 60f, scaleX = 1.5f, scaleY = 1.5f, opacity = 0f, easing = EasingType.LINEAR),
                Keyframe(frame = 32, x = 140f, y = 35f, rotation = 60f, scaleX = 1f, scaleY = 1f, opacity = 0f, easing = EasingType.LINEAR)
            )
        )

        val headLayer = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Hero Helmet",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.ROUNDED_RECT,
            width = 65f,
            height = 65f,
            zIndex = 2,
            shapeStyle = ShapeStyle(fillColor = 0xFF60A5FA, strokeColor = 0xFFFFFFFF, strokeWidth = 2.5f, cornerRadius = 18f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 0f, y = -75f, rotation = 0f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 8, x = -10f, y = -70f, rotation = -8f, scaleX = 0.98f, scaleY = 1.02f, easing = EasingType.EASE_IN_QUAD),
                Keyframe(frame = 16, x = 25f, y = -65f, rotation = 12f, scaleX = 1.05f, scaleY = 0.95f, easing = EasingType.BOUNCE_OUT),
                Keyframe(frame = 24, x = 10f, y = -73f, rotation = 4f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 32, x = 0f, y = -75f, rotation = 0f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC)
            )
        )

        val bodyLayer = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Hero Torso",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.ROUNDED_RECT,
            width = 75f,
            height = 90f,
            zIndex = 1,
            shapeStyle = ShapeStyle(fillColor = 0xFF2563EB, strokeColor = 0xFFFFFFFF, strokeWidth = 2.5f, cornerRadius = 14f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 0f, y = 5f, rotation = 0f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 8, x = -8f, y = 8f, rotation = -5f, scaleX = 1.02f, scaleY = 0.98f, easing = EasingType.EASE_IN_QUAD),
                Keyframe(frame = 16, x = 20f, y = 12f, rotation = 8f, scaleX = 1.08f, scaleY = 0.94f, easing = EasingType.BOUNCE_OUT),
                Keyframe(frame = 24, x = 6f, y = 7f, rotation = 2f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 32, x = 0f, y = 5f, rotation = 0f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC)
            )
        )

        val shieldLayer = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Hero Shield",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.SHIELD,
            width = 50f,
            height = 70f,
            zIndex = 0,
            shapeStyle = ShapeStyle(fillColor = 0xFF38BDF8, strokeColor = 0xFFFFFFFF, strokeWidth = 2f),
            keyframes = listOf(
                Keyframe(frame = 0, x = -50f, y = 10f, rotation = 5f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 8, x = -55f, y = 5f, rotation = 10f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 16, x = -35f, y = 15f, rotation = -8f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 24, x = -45f, y = 12f, rotation = 2f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 32, x = -50f, y = 10f, rotation = 5f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC)
            )
        )

        return KorProject(
            id = "sample_knight_attack",
            name = "Knight Slash Attack",
            fps = 24,
            totalFrames = 32,
            currentFrame = 0,
            loopMode = LoopMode.REPEAT,
            speedMultiplier = 1.0f,
            resolution = ResolutionPreset.RES_720P,
            layers = listOf(shieldLayer, bodyLayer, headLayer, swordLayer, slashArcLayer)
        )
    }

    private fun createSlimeBounceProject(): KorProject {
        val slimeBody = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Slime Body",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.SLIME,
            width = 110f,
            height = 95f,
            zIndex = 1,
            pivotX = 0.5f,
            pivotY = 0.9f,
            shapeStyle = ShapeStyle(fillColor = 0xFF22C55E, strokeColor = 0xFFFFFFFF, strokeWidth = 3f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 0f, y = 60f, rotation = 0f, scaleX = 1.25f, scaleY = 0.75f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 6, x = 0f, y = 20f, rotation = 0f, scaleX = 0.85f, scaleY = 1.2f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 14, x = 0f, y = -100f, rotation = 0f, scaleX = 1.0f, scaleY = 1.05f, easing = EasingType.EASE_IN_QUAD),
                Keyframe(frame = 22, x = 0f, y = 40f, rotation = 0f, scaleX = 0.9f, scaleY = 1.15f, easing = EasingType.EASE_IN_QUAD),
                Keyframe(frame = 26, x = 0f, y = 60f, rotation = 0f, scaleX = 1.35f, scaleY = 0.65f, easing = EasingType.BOUNCE_OUT),
                Keyframe(frame = 32, x = 0f, y = 60f, rotation = 0f, scaleX = 1.25f, scaleY = 0.75f, easing = EasingType.EASE_IN_OUT_CUBIC)
            )
        )

        val starSparks = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Impact Sparkle",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.STAR,
            width = 40f,
            height = 40f,
            zIndex = 2,
            shapeStyle = ShapeStyle(fillColor = 0xFFFACC15, strokeColor = 0xFFFFFFFF, strokeWidth = 1.5f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 60f, y = 50f, rotation = 0f, scaleX = 0f, scaleY = 0f, opacity = 0f, easing = EasingType.LINEAR),
                Keyframe(frame = 25, x = 0f, y = 60f, rotation = 0f, scaleX = 0.1f, scaleY = 0.1f, opacity = 0f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 27, x = 60f, y = 40f, rotation = 45f, scaleX = 1f, scaleY = 1f, opacity = 1f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 32, x = 80f, y = 30f, rotation = 90f, scaleX = 0.2f, scaleY = 0.2f, opacity = 0f, easing = EasingType.LINEAR)
            )
        )

        return KorProject(
            id = "sample_slime_bounce",
            name = "Slime Physics Bounce",
            fps = 24,
            totalFrames = 32,
            currentFrame = 0,
            loopMode = LoopMode.REPEAT,
            speedMultiplier = 1.0f,
            resolution = ResolutionPreset.RES_720P,
            layers = listOf(slimeBody, starSparks)
        )
    }

    private fun createCoinSpinProject(): KorProject {
        val coinLayer = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Gold Coin 3D Spin",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.COIN,
            width = 90f,
            height = 90f,
            zIndex = 1,
            shapeStyle = ShapeStyle(fillColor = 0xFFFACC15, strokeColor = 0xFFFFFFFF, strokeWidth = 3f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 0f, y = 0f, rotation = 0f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 6, x = 0f, y = -15f, rotation = 0f, scaleX = 0.3f, scaleY = 1.02f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 12, x = 0f, y = -25f, rotation = 0f, scaleX = 0.05f, scaleY = 1.05f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 18, x = 0f, y = -15f, rotation = 0f, scaleX = -0.3f, scaleY = 1.02f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 24, x = 0f, y = 0f, rotation = 0f, scaleX = -1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 30, x = 0f, y = 15f, rotation = 0f, scaleX = -0.3f, scaleY = 0.98f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 36, x = 0f, y = 25f, rotation = 0f, scaleX = 0.05f, scaleY = 0.95f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 42, x = 0f, y = 15f, rotation = 0f, scaleX = 0.3f, scaleY = 0.98f, easing = EasingType.EASE_IN_OUT_CUBIC),
                Keyframe(frame = 48, x = 0f, y = 0f, rotation = 0f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC)
            )
        )

        return KorProject(
            id = "sample_coin_spin",
            name = "Golden Coin 3D Spin",
            fps = 30,
            totalFrames = 48,
            currentFrame = 0,
            loopMode = LoopMode.REPEAT,
            speedMultiplier = 1.0f,
            resolution = ResolutionPreset.RES_720P,
            layers = listOf(coinLayer)
        )
    }

    private fun createSlashFxProject(): KorProject {
        val blade = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Energy Blade",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.SWORD,
            width = 30f,
            height = 120f,
            zIndex = 1,
            pivotX = 0.5f,
            pivotY = 0.9f,
            shapeStyle = ShapeStyle(fillColor = 0xFF38BDF8, strokeColor = 0xFFFFFFFF, strokeWidth = 2f),
            keyframes = listOf(
                Keyframe(frame = 0, x = -80f, y = 50f, rotation = -90f, scaleX = 1f, scaleY = 1f, easing = EasingType.BACK_IN_OUT),
                Keyframe(frame = 8, x = 0f, y = -60f, rotation = 0f, scaleX = 1.2f, scaleY = 1.2f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 16, x = 80f, y = 50f, rotation = 90f, scaleX = 1f, scaleY = 1f, easing = EasingType.BOUNCE_OUT),
                Keyframe(frame = 24, x = -80f, y = 50f, rotation = -90f, scaleX = 1f, scaleY = 1f, easing = EasingType.EASE_IN_OUT_CUBIC)
            )
        )

        val arcFx = AnimationLayer(
            id = UUID.randomUUID().toString(),
            name = "Energy Arc Wave",
            type = LayerType.SPRITE_PRESET,
            shapeKind = ShapeKind.SLASH_FX,
            width = 160f,
            height = 160f,
            zIndex = 2,
            shapeStyle = ShapeStyle(fillColor = 0xFFA855F7, strokeColor = 0xFF38BDF8, strokeWidth = 3f),
            keyframes = listOf(
                Keyframe(frame = 0, x = 0f, y = -30f, rotation = -45f, scaleX = 0.2f, scaleY = 0.2f, opacity = 0f, easing = EasingType.LINEAR),
                Keyframe(frame = 6, x = 0f, y = -40f, rotation = 0f, scaleX = 1.4f, scaleY = 1.4f, opacity = 1f, easing = EasingType.EASE_OUT_QUAD),
                Keyframe(frame = 12, x = 20f, y = -50f, rotation = 30f, scaleX = 1.8f, scaleY = 1.8f, opacity = 0f, easing = EasingType.LINEAR),
                Keyframe(frame = 24, x = 0f, y = -30f, rotation = -45f, scaleX = 0.2f, scaleY = 0.2f, opacity = 0f, easing = EasingType.LINEAR)
            )
        )

        return KorProject(
            id = "sample_slash_fx",
            name = "Neon Energy Slash FX",
            fps = 24,
            totalFrames = 24,
            currentFrame = 0,
            loopMode = LoopMode.REPEAT,
            speedMultiplier = 1.0f,
            resolution = ResolutionPreset.RES_720P,
            layers = listOf(blade, arcFx)
        )
    }
}
