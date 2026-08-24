package com.example.model.vfx

import java.util.UUID

/**
 * 2D Float Vector for position, velocity, and scale.
 */
data class Vector2(
    var x: Float = 0f,
    var y: Float = 0f
) {
    fun copy(): Vector2 = Vector2(x, y)
}

/**
 * RGBA Color representation (channels 0-255, alpha float 0.0-1.0).
 */
data class ColorRGBA(
    var r: Int = 255,
    var g: Int = 255,
    var b: Int = 255,
    var a: Float = 1.0f
) {
    fun toColorLong(): Long {
        val alphaInt = (a.coerceIn(0f, 1f) * 255).toInt()
        return ((alphaInt.toLong() and 0xFF) shl 24) or
                ((r.toLong() and 0xFF) shl 16) or
                ((g.toLong() and 0xFF) shl 8) or
                (b.toLong() and 0xFF)
    }

    companion object {
        fun fromColorLong(color: Long): ColorRGBA {
            val a = ((color shr 24) and 0xFF).toFloat() / 255f
            val r = ((color shr 16) and 0xFF).toInt()
            val g = ((color shr 8) and 0xFF).toInt()
            val b = (color and 0xFF).toInt()
            return ColorRGBA(r, g, b, a)
        }
    }
}

/**
 * Normalized UV texture atlas coordinates (0.0 - 1.0).
 */
data class TextureRect(
    var uvX: Float = 0f,
    var uvY: Float = 0f,
    var uvWidth: Float = 1f,
    var uvHeight: Float = 1f
)

/**
 * Single keyframe point for time-evaluated curves.
 */
data class CurveKeyframe(
    val time: Float = 0f,  // Normalized lifespan time [0.0, 1.0]
    val value: Float = 1f  // Evaluated multiplier/value
)

/**
 * Single color keypoint for time-evaluated color gradients.
 */
data class GradientColorKey(
    val time: Float = 0f, // Normalized lifespan time [0.0, 1.0]
    val color: ColorRGBA = ColorRGBA(255, 255, 255, 1f)
)

/**
 * Emitter spawn surface shape.
 */
enum class ShapeType(val id: Int, val displayName: String) {
    POINT(0, "Point"),
    LINE(1, "Line"),
    CIRCLE(2, "Circle"),
    RECTANGLE(3, "Rectangle"),
    RING(4, "Ring");

    companion object {
        fun fromId(id: Int): ShapeType = entries.find { it.id == id } ?: CIRCLE
    }
}

/**
 * Curve interpolation mode.
 */
enum class InterpolationType(val id: Int, val displayName: String) {
    LINEAR(0, "Linear"),
    CUBIC(1, "Cubic (Hermite)"),
    STEP(2, "Step (Snap)");

    companion object {
        fun fromId(id: Int): InterpolationType = entries.find { it.id == id } ?: LINEAR
    }
}

/**
 * Binary-serialized identifier per behavior module.
 */
enum class ModuleTypeId(val id: Int, val displayName: String) {
    LIFETIME(0x01, "Lifetime"),
    VELOCITY(0x02, "Velocity"),
    GRAVITY(0x03, "Gravity"),
    ROTATION(0x04, "Rotation"),
    SCALE_OVER_LIFETIME(0x05, "Scale Over Lifetime"),
    COLOR_OVER_LIFETIME(0x06, "Color Over Lifetime"),
    ALPHA_OVER_LIFETIME(0x07, "Alpha Over Lifetime");

    companion object {
        fun fromId(id: Int): ModuleTypeId = entries.find { it.id == id } ?: LIFETIME
    }
}

/**
 * Graphics rendering composite mode.
 */
enum class BlendMode(val id: Int, val displayName: String) {
    NORMAL(0, "Normal (Alpha Blend)"),
    ADDITIVE(1, "Additive (Luminous Glow)"),
    MULTIPLY(2, "Multiply (Darkening)"),
    SCREEN(3, "Screen (Soft Glow)");

    companion object {
        fun fromId(id: Int): BlendMode = entries.find { it.id == id } ?: NORMAL
    }
}

/**
 * Evaluates time-based scalar values via keyframe interpolation.
 */
data class VFXCurve(
    var interpolation: InterpolationType = InterpolationType.LINEAR,
    val keyframes: MutableList<CurveKeyframe> = mutableListOf()
) {
    fun addKeyframe(time: Float, value: Float) {
        val clampedTime = time.coerceIn(0f, 1f)
        keyframes.removeAll { kotlin.math.abs(it.time - clampedTime) < 0.001f }
        keyframes.add(CurveKeyframe(clampedTime, value))
        keyframes.sortBy { it.time }
    }

    fun evaluate(normalizedTime: Float): Float {
        if (keyframes.isEmpty()) return 1f
        if (keyframes.size == 1) return keyframes[0].value

        val t = normalizedTime.coerceIn(0f, 1f)
        if (t <= keyframes.first().time) return keyframes.first().value
        if (t >= keyframes.last().time) return keyframes.last().value

        for (i in 0 until keyframes.size - 1) {
            val k0 = keyframes[i]
            val k1 = keyframes[i + 1]
            if (t >= k0.time && t <= k1.time) {
                val span = (k1.time - k0.time).coerceAtLeast(0.0001f)
                val factor = (t - k0.time) / span
                return when (interpolation) {
                    InterpolationType.LINEAR -> k0.value + (k1.value - k0.value) * factor
                    InterpolationType.CUBIC -> {
                        val smooth = factor * factor * (3f - 2f * factor)
                        k0.value + (k1.value - k0.value) * smooth
                    }
                    InterpolationType.STEP -> k0.value
                }
            }
        }
        return keyframes.last().value
    }
}

/**
 * Evaluates time-based RGBA colors via color keys.
 */
data class VFXGradient(
    var interpolation: InterpolationType = InterpolationType.LINEAR,
    val keys: MutableList<GradientColorKey> = mutableListOf()
) {
    fun addKey(time: Float, color: ColorRGBA) {
        val clampedTime = time.coerceIn(0f, 1f)
        keys.removeAll { kotlin.math.abs(it.time - clampedTime) < 0.001f }
        keys.add(GradientColorKey(clampedTime, color))
        keys.sortBy { it.time }
    }

    fun evaluate(normalizedTime: Float): ColorRGBA {
        if (keys.isEmpty()) return ColorRGBA(255, 255, 255, 1f)
        if (keys.size == 1) return keys[0].color

        val t = normalizedTime.coerceIn(0f, 1f)
        if (t <= keys.first().time) return keys.first().color
        if (t >= keys.last().time) return keys.last().color

        for (i in 0 until keys.size - 1) {
            val k0 = keys[i]
            val k1 = keys[i + 1]
            if (t >= k0.time && t <= k1.time) {
                val span = (k1.time - k0.time).coerceAtLeast(0.0001f)
                val factor = (t - k0.time) / span
                val c0 = k0.color
                val c1 = k1.color
                return when (interpolation) {
                    InterpolationType.LINEAR, InterpolationType.CUBIC -> {
                        ColorRGBA(
                            r = (c0.r + (c1.r - c0.r) * factor).toInt().coerceIn(0, 255),
                            g = (c0.g + (c1.g - c0.g) * factor).toInt().coerceIn(0, 255),
                            b = (c0.b + (c1.b - c0.b) * factor).toInt().coerceIn(0, 255),
                            a = (c0.a + (c1.a - c0.a) * factor).coerceIn(0f, 1f)
                        )
                    }
                    InterpolationType.STEP -> c0
                }
            }
        }
        return keys.last().color
    }
}

/**
 * Modular behavior plugin base.
 */
sealed class VFXModule(val typeId: ModuleTypeId, val name: String)

class LifetimeModule : VFXModule(ModuleTypeId.LIFETIME, "Lifetime")
class VelocityModule : VFXModule(ModuleTypeId.VELOCITY, "Velocity")
data class GravityModule(var gravity: Float = -9.81f, var damping: Float = 1.0f) : VFXModule(ModuleTypeId.GRAVITY, "Gravity")
class RotationModule : VFXModule(ModuleTypeId.ROTATION, "Rotation")
data class ScaleModule(val scaleCurve: VFXCurve = VFXCurve()) : VFXModule(ModuleTypeId.SCALE_OVER_LIFETIME, "ScaleOverLifetime")
data class ColorModule(val colorGradient: VFXGradient = VFXGradient()) : VFXModule(ModuleTypeId.COLOR_OVER_LIFETIME, "ColorOverLifetime")
data class AlphaModule(val alphaCurve: VFXCurve = VFXCurve()) : VFXModule(ModuleTypeId.ALPHA_OVER_LIFETIME, "AlphaOverLifetime")

/**
 * Lightweight particle instance data container.
 */
data class VFXParticle(
    val position: Vector2 = Vector2(),
    val velocity: Vector2 = Vector2(),
    var rotation: Float = 0f,
    var angularVelocity: Float = 0f,
    var lifetime: Float = 1f,
    var age: Float = 0f,
    val scale: Vector2 = Vector2(1f, 1f),
    val baseScale: Vector2 = Vector2(1f, 1f),
    val color: ColorRGBA = ColorRGBA(),
    val baseColor: ColorRGBA = ColorRGBA(),
    var alpha: Float = 1f,
    val textureRect: TextureRect = TextureRect(),
    var emitterIndex: Int = 0,
    var isActive: Boolean = false
) {
    fun getLifeProgress(): Float = if (lifetime > 0f) (age / lifetime).coerceIn(0f, 1f) else 1f
    fun isDead(): Boolean = age >= lifetime

    fun reset() {
        position.x = 0f
        position.y = 0f
        velocity.x = 0f
        velocity.y = 0f
        rotation = 0f
        angularVelocity = 0f
        lifetime = 1f
        age = 0f
        scale.x = 1f
        scale.y = 1f
        baseScale.x = 1f
        baseScale.y = 1f
        color.r = 255
        color.g = 255
        color.b = 255
        color.a = 1f
        alpha = 1f
        emitterIndex = 0
        isActive = false
    }
}

/**
 * Zero-allocation memory pool manager.
 */
class VFXParticlePool(val capacity: Int = 1200) {
    val particles = Array(capacity) { VFXParticle() }
    var activeCount: Int = 0
        private set

    fun acquire(): VFXParticle? {
        for (i in 0 until capacity) {
            if (!particles[i].isActive) {
                particles[i].isActive = true
                activeCount++
                return particles[i]
            }
        }
        return null
    }

    fun release(particle: VFXParticle) {
        if (particle.isActive) {
            particle.isActive = false
            activeCount = (activeCount - 1).coerceAtLeast(0)
        }
    }

    fun clear() {
        for (p in particles) {
            p.reset()
        }
        activeCount = 0
    }
}

/**
 * Spawns and configures particles based on shapes and emission rates.
 */
data class VFXEmitter(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "Emitter",
    var shapeType: ShapeType = ShapeType.CIRCLE,
    var shapeSize: Vector2 = Vector2(15f, 15f),
    var spawnRate: Float = 50f,             // particles per second
    var burstCount: Int = 0,                // Instant burst count
    var burstInterval: Float = 0f,          // Seconds between bursts
    var particleLifetime: Float = 1.5f,     // Lifespan in seconds
    var speedMin: Float = 10f,
    var speedMax: Float = 50f,
    var spreadAngle: Float = 45f,           // Degrees
    var baseScaleMin: Vector2 = Vector2(8f, 8f),
    var baseScaleMax: Vector2 = Vector2(16f, 16f),
    var textureAtlas: String = "vfx_atlas.png",
    var textureUVRect: TextureRect = TextureRect(0f, 0f, 0.25f, 0.25f),
    val modules: MutableList<VFXModule> = mutableListOf()
) {
    fun findGravityModule(): GravityModule? = modules.filterIsInstance<GravityModule>().firstOrNull()
    fun findScaleModule(): ScaleModule? = modules.filterIsInstance<ScaleModule>().firstOrNull()
    fun findColorModule(): ColorModule? = modules.filterIsInstance<ColorModule>().firstOrNull()
    fun findAlphaModule(): AlphaModule? = modules.filterIsInstance<AlphaModule>().firstOrNull()

    fun deepCopy(): VFXEmitter {
        return copy(
            shapeSize = shapeSize.copy(),
            baseScaleMin = baseScaleMin.copy(),
            baseScaleMax = baseScaleMax.copy(),
            textureUVRect = textureUVRect.copy(),
            modules = modules.map { mod ->
                when (mod) {
                    is GravityModule -> mod.copy()
                    is ScaleModule -> ScaleModule(VFXCurve(mod.scaleCurve.interpolation, mod.scaleCurve.keyframes.map { it.copy() }.toMutableList()))
                    is ColorModule -> ColorModule(VFXGradient(mod.colorGradient.interpolation, mod.colorGradient.keys.map { it.copy(color = it.color.copy()) }.toMutableList()))
                    is AlphaModule -> AlphaModule(VFXCurve(mod.alphaCurve.interpolation, mod.alphaCurve.keyframes.map { it.copy() }.toMutableList()))
                    is LifetimeModule -> LifetimeModule()
                    is VelocityModule -> VelocityModule()
                    is RotationModule -> RotationModule()
                }
            }.toMutableList()
        )
    }
}

/**
 * Top-level VFX Effect container managing emitters, particle pool, and playback.
 */
data class VFXEffect(
    var name: String = "Custom Explosion",
    var effectId: String = "fx_custom_01",
    var version: String = "1.0",
    var duration: Float = 2.0f,
    var looping: Boolean = true,
    var blendMode: BlendMode = BlendMode.ADDITIVE,
    val emitters: MutableList<VFXEmitter> = mutableListOf()
) {
    fun deepCopy(): VFXEffect {
        return copy(
            emitters = emitters.map { it.deepCopy() }.toMutableList()
        )
    }
}
