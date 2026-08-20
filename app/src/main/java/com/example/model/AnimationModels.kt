package com.example.model

import java.util.UUID

enum class ResolutionPreset(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val tag: String
) {
    RES_440P("440p", "440p SD", 854, 480, "440p"),
    RES_720P("720p", "720p HD", 1280, 720, "720p"),
    RES_1080P("1080p", "1080p FHD", 1920, 1080, "1080p"),
    RES_2K("2k", "2K QHD", 2560, 1440, "2K"),
    RES_SQUARE("square", "1:1 Square", 512, 512, "1:1"),
    RES_RETRO("retro", "Pixel Retro", 320, 240, "Retro");

    companion object {
        fun fromId(id: String): ResolutionPreset {
            return entries.find { it.id == id } ?: RES_720P
        }
    }
}

enum class EasingType(val displayName: String, val description: String) {
    LINEAR("Linear", "Constant speed interpolation"),
    EASE_IN_QUAD("Ease In", "Starts slow, accelerates"),
    EASE_OUT_QUAD("Ease Out", "Starts fast, decelerates"),
    EASE_IN_OUT_CUBIC("Ease In-Out", "Smooth acceleration and deceleration"),
    BOUNCE_OUT("Bounce", "Bouncy landing physics"),
    ELASTIC_OUT("Elastic", "Springy overshoot settling"),
    BACK_IN_OUT("Anticipation / Back", "Pulls back before rushing forward"),
    STEP("Step (Snap)", "No interpolation, jumps at keyframe")
}

enum class LoopMode(val label: String) {
    REPEAT("Loop Repeat"),
    ONCE("Play Once"),
    PING_PONG("Ping-Pong")
}

enum class AccelerationType(val label: String, val multiplierDesc: String) {
    CONSTANT("Constant (1.0x)", "Uniform time progression"),
    ACCELERATING("Accelerating (Ease In)", "Scene speeds up over time"),
    DECELERATING("Decelerating (Ease Out)", "Scene slows down towards end"),
    S_CURVE("Smooth Pulse", "Wave velocity oscillation")
}

enum class LayerType(val displayName: String) {
    VECTOR_SHAPE("Vector Shape"),
    SPRITE_PRESET("Game Sprite"),
    CUSTOM_IMAGE("Imported Image"),
    BONE_JOINT("Bone / Limb"),
    TEXT_LABEL("Text / UI"),
    PARTICLE_FX("Particle FX")
}

enum class ShapeKind(val displayName: String) {
    RECTANGLE("Rectangle"),
    ROUNDED_RECT("Rounded Box"),
    CIRCLE("Circle"),
    STAR("5-Point Star"),
    TRIANGLE("Triangle"),
    DIAMOND("Diamond"),
    ARROW("Game Arrow"),
    SWORD("Blade Weapon"),
    SHIELD("Hero Shield"),
    SLIME("Game Slime"),
    COIN("Gold Coin"),
    SLASH_FX("Slash Arc FX")
}

data class ShapeStyle(
    val fillColor: Long = 0xFFA855F7,
    val strokeColor: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 2f,
    val cornerRadius: Float = 12f,
    val hasFill: Boolean = true,
    val hasStroke: Boolean = false
)

data class Keyframe(
    val frame: Int,
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val opacity: Float = 1f,
    val easing: EasingType = EasingType.EASE_IN_OUT_CUBIC,
    val colorTint: Long? = null
)

data class AnimationLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer",
    val type: LayerType = LayerType.VECTOR_SHAPE,
    val shapeKind: ShapeKind = ShapeKind.ROUNDED_RECT,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val zIndex: Int = 0,
    val customImageUri: String? = null,
    val presetAssetId: String? = null,
    val shapeStyle: ShapeStyle = ShapeStyle(),
    val textContent: String = "2D Game",
    val keyframes: List<Keyframe> = listOf(Keyframe(frame = 0, x = 0f, y = 0f)),
    val pivotX: Float = 0.5f,
    val pivotY: Float = 0.5f,
    val parentId: String? = null,
    val width: Float = 100f,
    val height: Float = 100f
)

data class InterpolatedTransform(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scaleX: Float,
    val scaleY: Float,
    val opacity: Float,
    val colorTint: Long?
)

data class KorProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Animation",
    val version: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis(),
    val fps: Int = 24,
    val totalFrames: Int = 48,
    val currentFrame: Int = 0,
    val loopMode: LoopMode = LoopMode.REPEAT,
    val speedMultiplier: Float = 1.0f,
    val accelerationType: AccelerationType = AccelerationType.CONSTANT,
    val resolution: ResolutionPreset = ResolutionPreset.RES_720P,
    val layers: List<AnimationLayer> = emptyList(),
    val backgroundColor: Long = 0xFF12161F
)

enum class EditorTool(val label: String) {
    SELECT("Select & Move"),
    ROTATE("Rotate Tool"),
    SCALE("Scale Tool"),
    PIVOT("Pivot Adjustment"),
    HAND_PAN("Pan & Navigate")
}
