package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.*

object SpriteSheetGenerator {

    fun generateSpriteSheet(
        project: KorProject,
        columns: Int = 4,
        frameStep: Int = 1,
        frameWidth: Int = 128,
        frameHeight: Int = 128,
        transparentBg: Boolean = true
    ): Bitmap {
        val frameIndices = (0 until project.totalFrames step frameStep).toList()
        val totalCells = frameIndices.size
        val rows = kotlin.math.ceil(totalCells.toDouble() / columns.toDouble()).toInt()

        val sheetWidth = columns * frameWidth
        val sheetHeight = rows * frameHeight

        val bitmap = Bitmap.createBitmap(sheetWidth, sheetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!transparentBg) {
            val bgPaint = Paint().apply {
                color = project.backgroundColor.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, sheetWidth.toFloat(), sheetHeight.toFloat(), bgPaint)
        }

        val sortedLayers = project.layers.filter { it.isVisible }.sortedBy { it.zIndex }

        for ((index, frame) in frameIndices.withIndex()) {
            val col = index % columns
            val row = index / columns
            val cellLeft = col * frameWidth.toFloat()
            val cellTop = row * frameHeight.toFloat()
            val cellCenterX = cellLeft + frameWidth / 2f
            val cellCenterY = cellTop + frameHeight / 2f

            canvas.save()
            // Clip to frame boundary
            canvas.clipRect(cellLeft, cellTop, cellLeft + frameWidth, cellTop + frameHeight)

            // Render each layer at this frame
            for (layer in sortedLayers) {
                val transform = EasingFunctions.evaluateLayerAtFrame(layer, frame.toFloat())
                if (transform.opacity <= 0f) continue

                canvas.save()
                val posX = cellCenterX + transform.x * (frameWidth.toFloat() / 400f)
                val posY = cellCenterY + transform.y * (frameHeight.toFloat() / 400f)

                canvas.translate(posX, posY)
                canvas.rotate(transform.rotation)
                canvas.scale(transform.scaleX, transform.scaleY)

                val scaledW = layer.width * (frameWidth.toFloat() / 400f)
                val scaledH = layer.height * (frameHeight.toFloat() / 400f)
                val left = -scaledW * layer.pivotX
                val top = -scaledH * layer.pivotY
                val right = left + scaledW
                val bottom = top + scaledH

                drawLayerShape(canvas, layer, left, top, right, bottom, transform.opacity)

                canvas.restore()
            }

            canvas.restore()
        }

        return bitmap
    }

    private fun drawLayerShape(
        canvas: Canvas,
        layer: AnimationLayer,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        alpha: Float
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.shapeStyle.fillColor.toInt()
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.shapeStyle.strokeColor.toInt()
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = layer.shapeStyle.strokeWidth
        }

        val w = right - left
        val h = bottom - top
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f

        when (layer.shapeKind) {
            ShapeKind.RECTANGLE -> {
                if (layer.shapeStyle.hasFill) canvas.drawRect(left, top, right, bottom, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawRect(left, top, right, bottom, strokePaint)
            }
            ShapeKind.ROUNDED_RECT -> {
                val r = layer.shapeStyle.cornerRadius
                val rect = RectF(left, top, right, bottom)
                if (layer.shapeStyle.hasFill) canvas.drawRoundRect(rect, r, r, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            ShapeKind.CIRCLE -> {
                val radius = kotlin.math.min(w, h) / 2f
                if (layer.shapeStyle.hasFill) canvas.drawCircle(cx, cy, radius, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawCircle(cx, cy, radius, strokePaint)
            }
            ShapeKind.STAR -> {
                val path = Path()
                val radiusOuter = kotlin.math.min(w, h) / 2f
                val radiusInner = radiusOuter * 0.45f
                val numPoints = 5
                for (i in 0 until numPoints * 2) {
                    val r = if (i % 2 == 0) radiusOuter else radiusInner
                    val angle = i * Math.PI.toFloat() / numPoints - Math.PI.toFloat() / 2f
                    val px = cx + r * kotlin.math.cos(angle)
                    val py = cy + r * kotlin.math.sin(angle)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                if (layer.shapeStyle.hasFill) canvas.drawPath(path, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(path, strokePaint)
            }
            ShapeKind.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(cx, top)
                    lineTo(right, bottom)
                    lineTo(left, bottom)
                    close()
                }
                if (layer.shapeStyle.hasFill) canvas.drawPath(path, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(path, strokePaint)
            }
            ShapeKind.DIAMOND -> {
                val path = Path().apply {
                    moveTo(cx, top)
                    lineTo(right, cy)
                    lineTo(cx, bottom)
                    lineTo(left, cy)
                    close()
                }
                if (layer.shapeStyle.hasFill) canvas.drawPath(path, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(path, strokePaint)
            }
            ShapeKind.SWORD -> {
                // Draw blade
                val bladePath = Path().apply {
                    moveTo(cx, top)
                    lineTo(cx + w * 0.3f, top + h * 0.2f)
                    lineTo(cx + w * 0.2f, top + h * 0.75f)
                    lineTo(cx - w * 0.2f, top + h * 0.75f)
                    lineTo(cx - w * 0.3f, top + h * 0.2f)
                    close()
                }
                if (layer.shapeStyle.hasFill) canvas.drawPath(bladePath, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(bladePath, strokePaint)
                // Guard & Hilt
                val guardRect = RectF(cx - w * 0.45f, top + h * 0.75f, cx + w * 0.45f, top + h * 0.82f)
                canvas.drawRect(guardRect, fillPaint)
                val hiltRect = RectF(cx - w * 0.12f, top + h * 0.82f, cx + w * 0.12f, bottom)
                canvas.drawRect(hiltRect, strokePaint)
            }
            ShapeKind.SHIELD -> {
                val shieldPath = Path().apply {
                    moveTo(left, top)
                    lineTo(right, top)
                    lineTo(right, top + h * 0.5f)
                    quadTo(cx, bottom, left, top + h * 0.5f)
                    close()
                }
                if (layer.shapeStyle.hasFill) canvas.drawPath(shieldPath, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(shieldPath, strokePaint)
            }
            ShapeKind.SLIME -> {
                val slimePath = Path().apply {
                    moveTo(cx, top)
                    cubicTo(right, top + h * 0.2f, right + w * 0.1f, bottom, cx, bottom)
                    cubicTo(left - w * 0.1f, bottom, left, top + h * 0.2f, cx, top)
                    close()
                }
                if (layer.shapeStyle.hasFill) canvas.drawPath(slimePath, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(slimePath, strokePaint)
            }
            ShapeKind.COIN -> {
                val r = kotlin.math.min(w, h) / 2f
                if (layer.shapeStyle.hasFill) canvas.drawCircle(cx, cy, r, fillPaint)
                val innerR = r * 0.7f
                canvas.drawCircle(cx, cy, innerR, strokePaint)
            }
            ShapeKind.SLASH_FX -> {
                val slashPath = Path().apply {
                    moveTo(left, top + h * 0.8f)
                    quadTo(cx, top, right, top + h * 0.2f)
                    quadTo(cx + w * 0.1f, top + h * 0.3f, left + w * 0.2f, bottom)
                    close()
                }
                if (layer.shapeStyle.hasFill) canvas.drawPath(slashPath, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(slashPath, strokePaint)
            }
            ShapeKind.ARROW -> {
                val arrowPath = Path().apply {
                    moveTo(right, cy)
                    lineTo(cx, top)
                    lineTo(cx, cy - h * 0.2f)
                    lineTo(left, cy - h * 0.2f)
                    lineTo(left, cy + h * 0.2f)
                    lineTo(cx, cy + h * 0.2f)
                    lineTo(cx, bottom)
                    close()
                }
                if (layer.shapeStyle.hasFill) canvas.drawPath(arrowPath, fillPaint)
                if (layer.shapeStyle.hasStroke) canvas.drawPath(arrowPath, strokePaint)
            }
        }
    }
}
