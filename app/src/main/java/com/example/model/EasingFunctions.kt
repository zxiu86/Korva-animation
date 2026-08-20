package com.example.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object EasingFunctions {

    fun calculate(t: Float, easing: EasingType): Float {
        val clamped = t.coerceIn(0f, 1f)
        return when (easing) {
            EasingType.LINEAR -> clamped
            EasingType.EASE_IN_QUAD -> clamped * clamped
            EasingType.EASE_OUT_QUAD -> 1f - (1f - clamped) * (1f - clamped)
            EasingType.EASE_IN_OUT_CUBIC -> {
                if (clamped < 0.5f) {
                    4f * clamped * clamped * clamped
                } else {
                    1f - (-2f * clamped + 2f).pow(3) / 2f
                }
            }
            EasingType.BOUNCE_OUT -> easeOutBounce(clamped)
            EasingType.ELASTIC_OUT -> {
                if (clamped == 0f) 0f
                else if (clamped == 1f) 1f
                else {
                    val c4 = (2f * Math.PI.toFloat()) / 3f
                    (2.0).pow((-10 * clamped).toDouble()).toFloat() * sin((clamped * 10f - 0.75f) * c4) + 1f
                }
            }
            EasingType.BACK_IN_OUT -> {
                val c1 = 1.70158f
                val c2 = c1 * 1.525f
                if (clamped < 0.5f) {
                    ((2f * clamped).pow(2) * ((c2 + 1f) * 2f * clamped - c2)) / 2f
                } else {
                    ((2f * clamped - 2f).pow(2) * ((c2 + 1f) * (clamped * 2f - 2f) + c2) + 2f) / 2f
                }
            }
            EasingType.STEP -> if (clamped >= 1f) 1f else 0f
        }
    }

    private fun easeOutBounce(x: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f

        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                val x2 = x - (1.5f / d1)
                n1 * x2 * x2 + 0.75f
            }
            x < 2.5f / d1 -> {
                val x2 = x - (2.25f / d1)
                n1 * x2 * x2 + 0.9375f
            }
            else -> {
                val x2 = x - (2.625f / d1)
                n1 * x2 * x2 + 0.984375f
            }
        }
    }

    fun interpolate(k1: Keyframe, k2: Keyframe, currentFrame: Float): InterpolatedTransform {
        if (k1.frame == k2.frame) {
            return InterpolatedTransform(
                x = k1.x,
                y = k1.y,
                rotation = k1.rotation,
                scaleX = k1.scaleX,
                scaleY = k1.scaleY,
                opacity = k1.opacity,
                colorTint = k1.colorTint
            )
        }

        val rawProgress = (currentFrame - k1.frame) / (k2.frame - k1.frame).toFloat()
        val factor = calculate(rawProgress, k1.easing)

        val x = k1.x + (k2.x - k1.x) * factor
        val y = k1.y + (k2.y - k1.y) * factor
        val rotation = k1.rotation + (k2.rotation - k1.rotation) * factor
        val scaleX = k1.scaleX + (k2.scaleX - k1.scaleX) * factor
        val scaleY = k1.scaleY + (k2.scaleY - k1.scaleY) * factor
        val opacity = k1.opacity + (k2.opacity - k1.opacity) * factor

        return InterpolatedTransform(
            x = x,
            y = y,
            rotation = rotation,
            scaleX = scaleX,
            scaleY = scaleY,
            opacity = opacity.coerceIn(0f, 1f),
            colorTint = if (factor > 0.5f) k2.colorTint ?: k1.colorTint else k1.colorTint
        )
    }

    fun evaluateLayerAtFrame(layer: AnimationLayer, frame: Float): InterpolatedTransform {
        val keyframes = layer.keyframes
        val size = keyframes.size
        if (size == 0) {
            return InterpolatedTransform(0f, 0f, 0f, 1f, 1f, layer.opacity, null)
        }
        if (size == 1) {
            val k = keyframes[0]
            return InterpolatedTransform(k.x, k.y, k.rotation, k.scaleX, k.scaleY, (k.opacity * layer.opacity).coerceIn(0f, 1f), k.colorTint)
        }

        // Fast boundary checks (keyframes are kept sorted by frame in ViewModel)
        val first = keyframes[0]
        if (frame <= first.frame) {
            return InterpolatedTransform(first.x, first.y, first.rotation, first.scaleX, first.scaleY, (first.opacity * layer.opacity).coerceIn(0f, 1f), first.colorTint)
        }

        val last = keyframes[size - 1]
        if (frame >= last.frame) {
            return InterpolatedTransform(last.x, last.y, last.rotation, last.scaleX, last.scaleY, (last.opacity * layer.opacity).coerceIn(0f, 1f), last.colorTint)
        }

        // Binary search or direct scan for surrounding keyframes
        for (i in 0 until size - 1) {
            val k1 = keyframes[i]
            val k2 = keyframes[i + 1]
            if (frame >= k1.frame && frame <= k2.frame) {
                val base = interpolate(k1, k2, frame)
                return base.copy(opacity = (base.opacity * layer.opacity).coerceIn(0f, 1f))
            }
        }

        return InterpolatedTransform(last.x, last.y, last.rotation, last.scaleX, last.scaleY, (last.opacity * layer.opacity).coerceIn(0f, 1f), last.colorTint)
    }
}
