package com.example.model.vfx

import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

/**
 * 2D Float Vector for position, velocity, scale, and force fields.
 */
data class Vector2(
    var x: Float = 0f,
    var y: Float = 0f
) {
    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vector2 = if (scalar != 0f) Vector2(x / scalar, y / scalar) else Vector2(0f, 0f)

    fun lengthSq(): Float = x * x + y * y
    fun length(): Float = sqrt(lengthSq())

    fun normalized(): Vector2 {
        val len = length()
        return if (len > 0.0001f) Vector2(x / len, y / len) else Vector2(0f, 0f)
    }

    fun distanceTo(other: Vector2): Float = (this - other).length()

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

    fun copy(): ColorRGBA = ColorRGBA(r, g, b, a)

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
) {
    fun copy(): TextureRect = TextureRect(uvX, uvY, uvWidth, uvHeight)
}

/**
 * Historical ribbon trail sample point for particle tails and light streaks.
 */
data class TrailPoint(
    val position: Vector2 = Vector2(),
    var age: Float = 0f,
    var width: Float = 4f,
    val color: ColorRGBA = ColorRGBA()
) {
    fun copy(): TrailPoint = TrailPoint(position.copy(), age, width, color.copy())
}

/**
 * Axis-aligned bounding box for effect rendering and frustum culling.
 */
data class EffectBounds(
    var minX: Float = 0f,
    var minY: Float = 0f,
    var maxX: Float = 0f,
    var maxY: Float = 0f
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(0f)
    val height: Float get() = (maxY - minY).coerceAtLeast(0f)
}

/**
 * Quad vertex layout for zero-copy OpenGL ES / C++ Batch Renderer (32 bytes).
 */
data class VFXVertex(
    val x: Float,
    val y: Float,
    val u: Float,
    val v: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float
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
    RING(4, "Ring"),
    CONE(5, "Cone");

    companion object {
        fun fromId(id: Int): ShapeType = entries.find { it.id == id } ?: CIRCLE
    }
}

/**
 * Particle geometry shape profile with support for asymmetric tapering (sharp at one end, thick at the other).
 */
enum class ParticleGeometry(val id: Int, val displayName: String) {
    TAPERED_NEEDLE(0, "Tapered (Sharp / Thick)"),
    TEARDROP(1, "Teardrop (Dart)"),
    DIAMOND(2, "Sharp Diamond"),
    CRESCENT(3, "Crescent Arc"),
    ELLIPSE(4, "Standard Ellipse");

    companion object {
        fun fromId(id: Int): ParticleGeometry = entries.find { it.id == id } ?: TAPERED_NEEDLE
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
 * Binary-serialized identifier for all 16 VFX behavior modules (Spec v1.10).
 */
enum class ModuleTypeId(val id: Int, val displayName: String) {
    LIFETIME(0x01, "Lifetime"),
    VELOCITY(0x02, "Velocity"),
    GRAVITY(0x03, "Gravity & Damping"),
    ROTATION(0x04, "Rotation"),
    SCALE_OVER_LIFETIME(0x05, "Scale Over Lifetime"),
    COLOR_OVER_LIFETIME(0x06, "Color Over Lifetime"),
    ALPHA_OVER_LIFETIME(0x07, "Alpha Over Lifetime"),
    TURBULENCE(0x08, "Turbulence (Curl/Perlin Noise)"),
    DRAG(0x09, "Drag & Friction"),
    VORTEX(0x0A, "Vortex & Swirl"),
    ATTRACTOR(0x0B, "Attractor & Gravity Well"),
    COLLISION(0x0C, "Collision & Floor Bounce"),
    FLIPBOOK(0x0D, "Flipbook Animation"),
    VELOCITY_ALIGNMENT(0x0E, "Velocity Alignment (Stretch)"),
    COLOR_BY_SPEED(0x0F, "Color By Speed"),
    TRAIL(0x10, "Ribbon Trail");

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
 * High-performance 2D Perlin and Divergence-Free Curl Noise generator for organic physics.
 */
object VFXNoise {
    private val P = intArrayOf(
        151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,
        8,99,37,240,21,10,23,190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,
        35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,
        134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,
        55,46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,
        18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,
        250,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,
        189,28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,
        172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,
        228,251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,
        107,49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,
        138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    )

    private val PERM = IntArray(512) { P[it % 256] }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    private fun grad(hash: Int, x: Float, y: Float): Float {
        val h = hash and 7
        val u = if (h < 4) x else y
        val v = if (h < 4) y else x
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }

    fun perlin2D(x: Float, y: Float): Float {
        val xi = (x.toInt() and 255)
        val yi = (y.toInt() and 255)
        val xf = x - x.toInt().toFloat()
        val yf = y - y.toInt().toFloat()

        val u = fade(xf)
        val v = fade(yf)

        val aa = PERM[PERM[xi] + yi]
        val ab = PERM[PERM[xi] + yi + 1]
        val ba = PERM[PERM[xi + 1] + yi]
        val bb = PERM[PERM[xi + 1] + yi + 1]

        val x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1f, yf))
        val x2 = lerp(u, grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f))
        return lerp(v, x1, x2)
    }

    /**
     * Divergence-free 2D Curl Noise for organic fluid and vortex motions.
     */
    fun curlNoise2D(x: Float, y: Float, epsilon: Float = 0.1f): Vector2 {
        val n1 = perlin2D(x, y + epsilon)
        val n2 = perlin2D(x, y - epsilon)
        val n3 = perlin2D(x + epsilon, y)
        val n4 = perlin2D(x - epsilon, y)

        val dx = (n1 - n2) / (2f * epsilon)
        val dy = (n3 - n4) / (2f * epsilon)

        return Vector2(dy, -dx)
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
        keyframes.removeAll { abs(it.time - clampedTime) < 0.001f }
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
        keys.removeAll { abs(it.time - clampedTime) < 0.001f }
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
 * Modular behavior plugin base class.
 */
sealed class VFXModule(val typeId: ModuleTypeId, val name: String)

class LifetimeModule : VFXModule(ModuleTypeId.LIFETIME, "Lifetime")
class VelocityModule : VFXModule(ModuleTypeId.VELOCITY, "Velocity")
data class GravityModule(var gravity: Float = -9.81f, var damping: Float = 1.0f) : VFXModule(ModuleTypeId.GRAVITY, "Gravity")
class RotationModule : VFXModule(ModuleTypeId.ROTATION, "Rotation")
data class ScaleModule(val scaleCurve: VFXCurve = VFXCurve()) : VFXModule(ModuleTypeId.SCALE_OVER_LIFETIME, "ScaleOverLifetime")
data class ColorModule(val colorGradient: VFXGradient = VFXGradient()) : VFXModule(ModuleTypeId.COLOR_OVER_LIFETIME, "ColorOverLifetime")
data class AlphaModule(val alphaCurve: VFXCurve = VFXCurve()) : VFXModule(ModuleTypeId.ALPHA_OVER_LIFETIME, "AlphaOverLifetime")

data class TurbulenceModule(
    var strength: Float = 25f,
    var frequency: Float = 0.05f,
    var scrollSpeed: Float = 1.0f,
    var useCurlNoise: Boolean = true
) : VFXModule(ModuleTypeId.TURBULENCE, "Turbulence")

data class DragModule(
    var linearDrag: Float = 0.5f,
    var quadraticDrag: Float = 0.01f
) : VFXModule(ModuleTypeId.DRAG, "Drag")

data class VortexModule(
    var center: Vector2 = Vector2(0f, 0f),
    var vortexStrength: Float = 50f,
    var radialPull: Float = 15f
) : VFXModule(ModuleTypeId.VORTEX, "Vortex")

data class AttractorModule(
    var targetPosition: Vector2 = Vector2(0f, 0f),
    var strength: Float = 60f
) : VFXModule(ModuleTypeId.ATTRACTOR, "Attractor")

data class CollisionModule(
    var floorY: Float = 150f,
    var restitution: Float = 0.7f,
    var friction: Float = 0.8f
) : VFXModule(ModuleTypeId.COLLISION, "Collision")

data class FlipbookModule(
    var columns: Int = 4,
    var rows: Int = 4,
    var totalFrames: Int = 16,
    var frameRate: Float = 30f,
    var loop: Boolean = true
) : VFXModule(ModuleTypeId.FLIPBOOK, "Flipbook")

data class VelocityAlignmentModule(
    var stretchFactor: Float = 0.03f
) : VFXModule(ModuleTypeId.VELOCITY_ALIGNMENT, "VelocityAlignment")

data class ColorBySpeedModule(
    val gradient: VFXGradient = VFXGradient(),
    var minSpeed: Float = 0f,
    var maxSpeed: Float = 120f
) : VFXModule(ModuleTypeId.COLOR_BY_SPEED, "ColorBySpeed")

data class TrailModule(
    var segmentInterval: Float = 0.02f,
    var trailLifetime: Float = 0.3f,
    var maxPoints: Int = 16
) : VFXModule(ModuleTypeId.TRAIL, "Trail")

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
    var isActive: Boolean = false,
    val trails: MutableList<TrailPoint> = mutableListOf(),
    var trailTimer: Float = 0f,
    var frameIndex: Int = 0,
    var frameTimer: Float = 0f,
    var stretch: Float = 1f,
    var particleGeometry: ParticleGeometry = ParticleGeometry.TAPERED_NEEDLE,
    var taperFactor: Float = 0.8f,
    var headThickness: Float = 0.15f,
    var tailThickness: Float = 1.0f
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
        trails.clear()
        trailTimer = 0f
        frameIndex = 0
        frameTimer = 0f
        stretch = 1f
        particleGeometry = ParticleGeometry.TAPERED_NEEDLE
        taperFactor = 0.8f
        headThickness = 0.15f
        tailThickness = 1.0f
    }
}

/**
 * Zero-allocation memory pool manager.
 */
class VFXParticlePool(val capacity: Int = 1600) {
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
 * Spawns and configures particles based on shapes and emission rates with sub-emitter support.
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
    var onBirthSubEmitter: Int = -1,        // Index of sub-emitter spawned on particle birth
    var onDeathSubEmitter: Int = -1,        // Index of sub-emitter spawned on particle death
    var onCollisionSubEmitter: Int = -1,    // Index of sub-emitter spawned on collision
    var particleGeometry: ParticleGeometry = ParticleGeometry.TAPERED_NEEDLE,
    var taperFactor: Float = 0.8f,          // 0.0 = uniform, 1.0 = sharp needle tip & thick base, -1.0 = thick head & needle tail
    var headThickness: Float = 0.15f,       // Tip width multiplier (0.05 to 1.0)
    var tailThickness: Float = 1.0f,        // Base width multiplier (0.5 to 3.0)
    val modules: MutableList<VFXModule> = mutableListOf()
) {
    fun findGravityModule(): GravityModule? = modules.filterIsInstance<GravityModule>().firstOrNull()
    fun findScaleModule(): ScaleModule? = modules.filterIsInstance<ScaleModule>().firstOrNull()
    fun findColorModule(): ColorModule? = modules.filterIsInstance<ColorModule>().firstOrNull()
    fun findAlphaModule(): AlphaModule? = modules.filterIsInstance<AlphaModule>().firstOrNull()
    fun findTurbulenceModule(): TurbulenceModule? = modules.filterIsInstance<TurbulenceModule>().firstOrNull()
    fun findDragModule(): DragModule? = modules.filterIsInstance<DragModule>().firstOrNull()
    fun findVortexModule(): VortexModule? = modules.filterIsInstance<VortexModule>().firstOrNull()
    fun findAttractorModule(): AttractorModule? = modules.filterIsInstance<AttractorModule>().firstOrNull()
    fun findCollisionModule(): CollisionModule? = modules.filterIsInstance<CollisionModule>().firstOrNull()
    fun findFlipbookModule(): FlipbookModule? = modules.filterIsInstance<FlipbookModule>().firstOrNull()
    fun findVelocityAlignmentModule(): VelocityAlignmentModule? = modules.filterIsInstance<VelocityAlignmentModule>().firstOrNull()
    fun findColorBySpeedModule(): ColorBySpeedModule? = modules.filterIsInstance<ColorBySpeedModule>().firstOrNull()
    fun findTrailModule(): TrailModule? = modules.filterIsInstance<TrailModule>().firstOrNull()

    fun deepCopy(): VFXEmitter {
        return copy(
            shapeSize = shapeSize.copy(),
            baseScaleMin = baseScaleMin.copy(),
            baseScaleMax = baseScaleMax.copy(),
            textureUVRect = textureUVRect.copy(),
            particleGeometry = particleGeometry,
            taperFactor = taperFactor,
            headThickness = headThickness,
            tailThickness = tailThickness,
            modules = modules.map { mod ->
                when (mod) {
                    is GravityModule -> mod.copy()
                    is ScaleModule -> ScaleModule(VFXCurve(mod.scaleCurve.interpolation, mod.scaleCurve.keyframes.map { it.copy() }.toMutableList()))
                    is ColorModule -> ColorModule(VFXGradient(mod.colorGradient.interpolation, mod.colorGradient.keys.map { it.copy(color = it.color.copy()) }.toMutableList()))
                    is AlphaModule -> AlphaModule(VFXCurve(mod.alphaCurve.interpolation, mod.alphaCurve.keyframes.map { it.copy() }.toMutableList()))
                    is TurbulenceModule -> mod.copy()
                    is DragModule -> mod.copy()
                    is VortexModule -> mod.copy(center = mod.center.copy())
                    is AttractorModule -> mod.copy(targetPosition = mod.targetPosition.copy())
                    is CollisionModule -> mod.copy()
                    is FlipbookModule -> mod.copy()
                    is VelocityAlignmentModule -> mod.copy()
                    is ColorBySpeedModule -> ColorBySpeedModule(VFXGradient(mod.gradient.interpolation, mod.gradient.keys.map { it.copy(color = it.color.copy()) }.toMutableList()), mod.minSpeed, mod.maxSpeed)
                    is TrailModule -> mod.copy()
                    is LifetimeModule -> LifetimeModule()
                    is VelocityModule -> VelocityModule()
                    is RotationModule -> RotationModule()
                }
            }.toMutableList()
        )
    }
}

/**
 * Top-level VFX Effect container managing emitters, particle pool, bounds, and playback.
 */
data class VFXEffect(
    var name: String = "Custom Explosion",
    var effectId: String = "fx_custom_01",
    var version: String = "1.10",
    var duration: Float = 2.0f,
    var looping: Boolean = true,
    var blendMode: BlendMode = BlendMode.ADDITIVE,
    var timeScale: Float = 1.0f,
    val emitters: MutableList<VFXEmitter> = mutableListOf()
) {
    fun calculateBounds(particles: Array<VFXParticle>): EffectBounds {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var hasActive = false

        for (p in particles) {
            if (!p.isActive) continue
            hasActive = true
            val radius = max(p.scale.x, p.scale.y) * 0.5f
            minX = min(minX, p.position.x - radius)
            minY = min(minY, p.position.y - radius)
            maxX = max(maxX, p.position.x + radius)
            maxY = max(maxY, p.position.y + radius)
        }

        return if (hasActive) EffectBounds(minX, minY, maxX, maxY) else EffectBounds(-50f, -50f, 50f, 50f)
    }

    fun deepCopy(): VFXEffect {
        return copy(
            emitters = emitters.map { it.deepCopy() }.toMutableList()
        )
    }
}
