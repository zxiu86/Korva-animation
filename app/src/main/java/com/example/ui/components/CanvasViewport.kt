package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel
import kotlin.math.*

private object ViewportPathEngine {
    val tempPath1 = Path()
    val tempPath2 = Path()
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
}

private enum class TransformHandleType {
    NONE,
    BODY_MOVE,
    ROTATION_KNOB,
    CORNER_TL,
    CORNER_TR,
    CORNER_BR,
    CORNER_BL,
    EDGE_TOP,
    EDGE_RIGHT,
    EDGE_BOTTOM,
    EDGE_LEFT,
    PIVOT_KNOB
}

/**
 * High-Performance World Matrix & Local Geometry Engine.
 * Evaluates exact vertices, center offsets, bounding boxes, and handle anchors
 * synchronized with scale, rotation, and pivot point.
 */
private data class ViewportLayerGeometry(
    val origin: Offset,
    val pTL: Offset,
    val pTR: Offset,
    val pBR: Offset,
    val pBL: Offset,
    val midTop: Offset,
    val midRight: Offset,
    val midBottom: Offset,
    val midLeft: Offset,
    val rotHandle: Offset,
    val rotationRad: Double,
    val rotationDeg: Float,
    val scaleX: Float,
    val scaleY: Float,
    val l0: Float,
    val t0: Float,
    val r0: Float,
    val b0: Float,
    val zoom: Float
) {
    fun localToWorld(local: Offset): Offset {
        val cosA = cos(rotationRad).toFloat()
        val sinA = sin(rotationRad).toFloat()
        val wx = origin.x + local.x * cosA - local.y * sinA
        val wy = origin.y + local.x * sinA + local.y * cosA
        return Offset(wx, wy)
    }

    fun worldToLocal(world: Offset): Offset {
        val dx = world.x - origin.x
        val dy = world.y - origin.y
        val cosA = cos(rotationRad).toFloat()
        val sinA = sin(rotationRad).toFloat()
        val u = dx * cosA + dy * sinA
        val v = -dx * sinA + dy * cosA
        return Offset(u, v)
    }

    fun isInsideBody(local: Offset): Boolean {
        val sx = if (abs(scaleX) < 0.001f) 0.001f else scaleX
        val sy = if (abs(scaleY) < 0.001f) 0.001f else scaleY
        val uUnscaled = local.x / sx
        val vUnscaled = local.y / sy
        val minU = min(l0, r0) - 14f
        val maxU = max(l0, r0) + 14f
        val minV = min(t0, b0) - 14f
        val maxV = max(t0, b0) + 14f
        return (uUnscaled in minU..maxU) && (vUnscaled in minV..maxV)
    }
}

private fun computeViewportGeometry(
    layer: AnimationLayer,
    transform: InterpolatedTransform,
    stageCenterX: Float,
    stageCenterY: Float,
    zoom: Float
): ViewportLayerGeometry {
    val origin = Offset(stageCenterX + transform.x * zoom, stageCenterY + transform.y * zoom)
    val layerW = layer.width * zoom
    val layerH = layer.height * zoom

    val l0 = -layerW * layer.pivotX
    val t0 = -layerH * layer.pivotY
    val r0 = l0 + layerW
    val b0 = t0 + layerH

    val pTL = Offset(l0 * transform.scaleX, t0 * transform.scaleY)
    val pTR = Offset(r0 * transform.scaleX, t0 * transform.scaleY)
    val pBR = Offset(r0 * transform.scaleX, b0 * transform.scaleY)
    val pBL = Offset(l0 * transform.scaleX, b0 * transform.scaleY)

    val midTop = Offset((pTL.x + pTR.x) / 2f, (pTL.y + pTR.y) / 2f)
    val midRight = Offset((pTR.x + pBR.x) / 2f, (pTR.y + pBR.y) / 2f)
    val midBottom = Offset((pBL.x + pBR.x) / 2f, (pBL.y + pBR.y) / 2f)
    val midLeft = Offset((pTL.x + pBL.x) / 2f, (pTL.y + pBL.y) / 2f)

    val dirY = if (transform.scaleY >= 0f) -1f else 1f
    val rotHandle = Offset(midTop.x, midTop.y + dirY * 30f)

    return ViewportLayerGeometry(
        origin = origin,
        pTL = pTL,
        pTR = pTR,
        pBR = pBR,
        pBL = pBL,
        midTop = midTop,
        midRight = midRight,
        midBottom = midBottom,
        midLeft = midLeft,
        rotHandle = rotHandle,
        rotationRad = Math.toRadians(transform.rotation.toDouble()),
        rotationDeg = transform.rotation,
        scaleX = transform.scaleX,
        scaleY = transform.scaleY,
        l0 = l0,
        t0 = t0,
        r0 = r0,
        b0 = b0,
        zoom = zoom
    )
}

/**
 * Rebuilt Next-Gen 2D Animation Stage Viewport.
 * Ultra-responsive 60/120fps hardware rendering, World-Matrix bound gizmo handles,
 * non-blocking gesture manipulation, motion trajectories, and clean workspace.
 */
@Composable
fun CanvasViewport(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.project.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val zoom by viewModel.viewportZoom.collectAsState()
    val pan by viewModel.viewportPan.collectAsState()
    val onionSkinEnabled by viewModel.onionSkinEnabled.collectAsState()
    val pastOnionCount by viewModel.onionSkinPastFrames.collectAsState()
    val futureOnionCount by viewModel.onionSkinFutureFrames.collectAsState()
    val gridVisible by viewModel.gridVisible.collectAsState()
    val snapToGrid by viewModel.snapToGrid.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val showMotionTrajectory by viewModel.showMotionTrajectory.collectAsState()
    val showSafeZones by viewModel.showSafeZones.collectAsState()
    val showRuleOfThirds by viewModel.showRuleOfThirds.collectAsState()
    val canvasBgMode by viewModel.canvasBgMode.collectAsState()
    val activeVfxCount by viewModel.vfxActiveParticleCount.collectAsState()
    val currentVFXEffect by viewModel.currentVFXEffect.collectAsState()

    val selectedLayer = project.layers.find { it.id == selectedLayerId }
    val selectedTransform = selectedLayer?.let { EasingFunctions.evaluateLayerAtFrame(it, currentFrame) }

    var currentHandle by remember { mutableStateOf(TransformHandleType.NONE) }
    var isDragging by remember { mutableStateOf(false) }
    var liveTooltipText by remember { mutableStateOf("") }

    // Stable gesture origin and anchor variables
    var dragStartTouchPos by remember { mutableStateOf(Offset.Zero) }
    var dragStartOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragStartTouchAngle by remember { mutableStateOf(0f) }
    var dragStartLayerRotation by remember { mutableStateOf(0f) }
    var dragStartLayerX by remember { mutableStateOf(0f) }
    var dragStartLayerY by remember { mutableStateOf(0f) }
    var dragStartScaleX by remember { mutableStateOf(1f) }
    var dragStartScaleY by remember { mutableStateOf(1f) }
    var dragStartPivotX by remember { mutableStateOf(0.5f) }
    var dragStartPivotY by remember { mutableStateOf(0.5f) }

    // Magnetic snapping guides
    var snapGuideX by remember { mutableStateOf<Float?>(null) }
    var snapGuideY by remember { mutableStateOf<Float?>(null) }

    // Viewport Overlays Popup Menu State
    var showViewportSettingsMenu by remember { mutableStateOf(false) }

    val activeOverlaysCount = (if (onionSkinEnabled) 1 else 0) +
        (if (showMotionTrajectory) 1 else 0) +
        (if (gridVisible) 1 else 0) +
        (if (snapToGrid) 1 else 0) +
        (if (showSafeZones) 1 else 0) +
        (if (showRuleOfThirds) 1 else 0) +
        (if (canvasBgMode != 0) 1 else 0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidianDark)
            .clipToBounds()
            .testTag("canvas_viewport")
            .pointerInput(activeTool, selectedLayerId) {
                if (activeTool == EditorTool.HAND_PAN) {
                    detectTransformGestures { _, panDelta, zoomDelta, _ ->
                        viewModel.updateViewportPan(panDelta)
                        viewModel.updateViewportZoom(zoomDelta)
                    }
                } else {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val currentProject = viewModel.project.value
                            val currentF = viewModel.currentFrame.value
                            val currentZoom = viewModel.viewportZoom.value
                            val currentPan = viewModel.viewportPan.value
                            val stageCenterX = size.width / 2f + currentPan.x
                            val stageCenterY = size.height / 2f + currentPan.y

                            val layer = currentProject.layers.find { it.id == selectedLayerId }
                            if (layer == null || layer.isLocked || viewModel.isPlaying.value) {
                                currentHandle = TransformHandleType.NONE
                                return@detectDragGestures
                            }

                            val tf = EasingFunctions.evaluateLayerAtFrame(layer, currentF)
                            val geo = computeViewportGeometry(
                                layer = layer,
                                transform = tf,
                                stageCenterX = stageCenterX,
                                stageCenterY = stageCenterY,
                                zoom = currentZoom
                            )

                            val local = geo.worldToLocal(startOffset)
                            val hitRadius = 36f

                            dragStartTouchPos = startOffset
                            dragStartOrigin = geo.origin
                            dragStartLayerX = tf.x
                            dragStartLayerY = tf.y
                            dragStartLayerRotation = tf.rotation
                            dragStartScaleX = tf.scaleX
                            dragStartScaleY = tf.scaleY
                            dragStartPivotX = layer.pivotX
                            dragStartPivotY = layer.pivotY
                            isDragging = true

                            when (activeTool) {
                                EditorTool.ROTATE -> {
                                    // Circular Rotation: Polar angle tracking directly around static layer origin
                                    val lx = geo.origin.x
                                    val ly = geo.origin.y
                                    dragStartTouchAngle = Math.toDegrees(atan2((startOffset.y - ly).toDouble(), (startOffset.x - lx).toDouble())).toFloat()
                                    currentHandle = TransformHandleType.ROTATION_KNOB
                                    liveTooltipText = "∠ ${String.format(java.util.Locale.US, "%.1f", tf.rotation)}°"
                                }
                                EditorTool.SELECT -> {
                                    // Exclusive Move / Translation
                                    currentHandle = TransformHandleType.BODY_MOVE
                                    liveTooltipText = "X: ${tf.x.toInt()}px, Y: ${tf.y.toInt()}px"
                                }
                                EditorTool.SCALE -> {
                                    // 4 Corner scale handles
                                    if (hypot(local.x - geo.pTL.x, local.y - geo.pTL.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.CORNER_TL
                                    } else if (hypot(local.x - geo.pTR.x, local.y - geo.pTR.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.CORNER_TR
                                    } else if (hypot(local.x - geo.pBR.x, local.y - geo.pBR.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.CORNER_BR
                                    } else if (hypot(local.x - geo.pBL.x, local.y - geo.pBL.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.CORNER_BL
                                    } else if (hypot(local.x - geo.midTop.x, local.y - geo.midTop.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.EDGE_TOP
                                    } else if (hypot(local.x - geo.midRight.x, local.y - geo.midRight.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.EDGE_RIGHT
                                    } else if (hypot(local.x - geo.midBottom.x, local.y - geo.midBottom.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.EDGE_BOTTOM
                                    } else if (hypot(local.x - geo.midLeft.x, local.y - geo.midLeft.y) <= hitRadius) {
                                        currentHandle = TransformHandleType.EDGE_LEFT
                                    } else {
                                        currentHandle = TransformHandleType.NONE
                                    }
                                    liveTooltipText = "Scale: ${(abs(tf.scaleX) * 100).toInt()}%"
                                }
                                EditorTool.PIVOT -> {
                                    currentHandle = TransformHandleType.PIVOT_KNOB
                                    liveTooltipText = "Pivot: (${(layer.pivotX * 100).toInt()}%, ${(layer.pivotY * 100).toInt()}%)"
                                }
                                else -> {
                                    currentHandle = TransformHandleType.NONE
                                }
                            }
                        },
                        onDragEnd = {
                            currentHandle = TransformHandleType.NONE
                            isDragging = false
                            snapGuideX = null
                            snapGuideY = null
                            liveTooltipText = ""
                        },
                        onDragCancel = {
                            currentHandle = TransformHandleType.NONE
                            isDragging = false
                            snapGuideX = null
                            snapGuideY = null
                            liveTooltipText = ""
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val currentZoom = viewModel.viewportZoom.value
                            val isSnap = viewModel.snapToGrid.value

                            when (activeTool) {
                                // 1. ROTATE TOOL (Zero position drift, exact polar angle)
                                EditorTool.ROTATE -> {
                                    val lx = dragStartOrigin.x
                                    val ly = dragStartOrigin.y
                                    val currentTouchAngle = Math.toDegrees(atan2((change.position.y - ly).toDouble(), (change.position.x - lx).toDouble())).toFloat()
                                    var angleDelta = currentTouchAngle - dragStartTouchAngle
                                    while (angleDelta > 180f) angleDelta -= 360f
                                    while (angleDelta < -180f) angleDelta += 360f

                                    var targetAngle = (dragStartLayerRotation + angleDelta) % 360f
                                    if (targetAngle < 0f) targetAngle += 360f

                                    if (isSnap) {
                                        val step = 15f
                                        val nearest = (Math.round(targetAngle / step) * step).toFloat()
                                        if (abs(targetAngle - nearest) < 4.5f) {
                                            targetAngle = (nearest % 360f + 360f) % 360f
                                        }
                                    }

                                    viewModel.addOrUpdateKeyframeOnCurrentFrame(rotation = targetAngle)
                                    liveTooltipText = "∠ ${String.format(java.util.Locale.US, "%.1f", targetAngle)}°"
                                }

                                // 2. MOVE / SELECT TOOL (Continuous smooth floating-point drag following user's finger)
                                EditorTool.SELECT -> {
                                    val totalDeltaX = (change.position.x - dragStartTouchPos.x) / currentZoom
                                    val totalDeltaY = (change.position.y - dragStartTouchPos.y) / currentZoom

                                    var newX = dragStartLayerX + totalDeltaX
                                    var newY = dragStartLayerY + totalDeltaY

                                    if (isSnap) {
                                        val sz = viewModel.gridSize.value.toFloat()
                                        // Soft snap only when close to grid line, otherwise smooth free motion
                                        val snapX = kotlin.math.round(newX / sz) * sz
                                        val snapY = kotlin.math.round(newY / sz) * sz
                                        if (abs(newX - snapX) < 4f) newX = snapX
                                        if (abs(newY - snapY) < 4f) newY = snapY
                                    }

                                    val nearZeroX = abs(newX) < 6f
                                    val nearZeroY = abs(newY) < 6f
                                    snapGuideX = if (nearZeroX) 0f else null
                                    snapGuideY = if (nearZeroY) 0f else null

                                    viewModel.addOrUpdateKeyframeOnCurrentFrame(x = newX, y = newY)
                                    liveTooltipText = "X: ${String.format(java.util.Locale.US, "%.1f", newX)}, Y: ${String.format(java.util.Locale.US, "%.1f", newY)}"
                                }

                                // 3. SCALE TOOL (Exact handle-guided scaling without drift)
                                EditorTool.SCALE -> {
                                    val totalDeltaX = (change.position.x - dragStartTouchPos.x)
                                    val totalDeltaY = (change.position.y - dragStartTouchPos.y)
                                    val currentLayer = viewModel.getSelectedLayer()
                                    if (currentLayer != null) {
                                        val baseW = max(currentLayer.width * currentZoom, 20f)
                                        val baseH = max(currentLayer.height * currentZoom, 20f)
                                        val rotRad = Math.toRadians(dragStartLayerRotation.toDouble())
                                        val cosA = cos(rotRad).toFloat()
                                        val sinA = sin(rotRad).toFloat()
                                        val localDeltaX = totalDeltaX * cosA + totalDeltaY * sinA
                                        val localDeltaY = -totalDeltaX * sinA + totalDeltaY * cosA

                                        val signX = if (dragStartScaleX < 0f) -1f else 1f
                                        val signY = if (dragStartScaleY < 0f) -1f else 1f

                                        when (currentHandle) {
                                            TransformHandleType.CORNER_BR -> {
                                                val sX = 1f + (localDeltaX / baseW)
                                                val sY = 1f + (localDeltaY / baseH)
                                                val factor = ((sX + sY) / 2f).coerceIn(0.1f, 10f)
                                                val targetSx = (abs(dragStartScaleX) * factor).coerceIn(0.02f, 15f) * signX
                                                val targetSy = (abs(dragStartScaleY) * factor).coerceIn(0.02f, 15f) * signY
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = targetSx, scaleY = targetSy)
                                                liveTooltipText = "Scale: ${(abs(targetSx) * 100).toInt()}%"
                                            }
                                            TransformHandleType.CORNER_TR -> {
                                                val sX = 1f + (localDeltaX / baseW)
                                                val sY = 1f - (localDeltaY / baseH)
                                                val factor = ((sX + sY) / 2f).coerceIn(0.1f, 10f)
                                                val targetSx = (abs(dragStartScaleX) * factor).coerceIn(0.02f, 15f) * signX
                                                val targetSy = (abs(dragStartScaleY) * factor).coerceIn(0.02f, 15f) * signY
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = targetSx, scaleY = targetSy)
                                                liveTooltipText = "Scale: ${(abs(targetSx) * 100).toInt()}%"
                                            }
                                            TransformHandleType.CORNER_TL -> {
                                                val sX = 1f - (localDeltaX / baseW)
                                                val sY = 1f - (localDeltaY / baseH)
                                                val factor = ((sX + sY) / 2f).coerceIn(0.1f, 10f)
                                                val targetSx = (abs(dragStartScaleX) * factor).coerceIn(0.02f, 15f) * signX
                                                val targetSy = (abs(dragStartScaleY) * factor).coerceIn(0.02f, 15f) * signY
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = targetSx, scaleY = targetSy)
                                                liveTooltipText = "Scale: ${(abs(targetSx) * 100).toInt()}%"
                                            }
                                            TransformHandleType.CORNER_BL -> {
                                                val sX = 1f - (localDeltaX / baseW)
                                                val sY = 1f + (localDeltaY / baseH)
                                                val factor = ((sX + sY) / 2f).coerceIn(0.1f, 10f)
                                                val targetSx = (abs(dragStartScaleX) * factor).coerceIn(0.02f, 15f) * signX
                                                val targetSy = (abs(dragStartScaleY) * factor).coerceIn(0.02f, 15f) * signY
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = targetSx, scaleY = targetSy)
                                                liveTooltipText = "Scale: ${(abs(targetSx) * 100).toInt()}%"
                                            }
                                            TransformHandleType.EDGE_RIGHT -> {
                                                val factorX = (1f + localDeltaX / baseW).coerceIn(0.1f, 10f)
                                                val targetSx = (abs(dragStartScaleX) * factorX).coerceIn(0.02f, 15f) * signX
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = targetSx)
                                                liveTooltipText = "Scale X: ${(abs(targetSx) * 100).toInt()}%"
                                            }
                                            TransformHandleType.EDGE_LEFT -> {
                                                val factorX = (1f - localDeltaX / baseW).coerceIn(0.1f, 10f)
                                                val targetSx = (abs(dragStartScaleX) * factorX).coerceIn(0.02f, 15f) * signX
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = targetSx)
                                                liveTooltipText = "Scale X: ${(abs(targetSx) * 100).toInt()}%"
                                            }
                                            TransformHandleType.EDGE_BOTTOM -> {
                                                val factorY = (1f + localDeltaY / baseH).coerceIn(0.1f, 10f)
                                                val targetSy = (abs(dragStartScaleY) * factorY).coerceIn(0.02f, 15f) * signY
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleY = targetSy)
                                                liveTooltipText = "Scale Y: ${(abs(targetSy) * 100).toInt()}%"
                                            }
                                            TransformHandleType.EDGE_TOP -> {
                                                val factorY = (1f - localDeltaY / baseH).coerceIn(0.1f, 10f)
                                                val targetSy = (abs(dragStartScaleY) * factorY).coerceIn(0.02f, 15f) * signY
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleY = targetSy)
                                                liveTooltipText = "Scale Y: ${(abs(targetSy) * 100).toInt()}%"
                                            }
                                            else -> {
                                                val dragDiag = (totalDeltaX - totalDeltaY) * 0.006f
                                                val factor = (1f + dragDiag).coerceIn(0.1f, 10f)
                                                val targetSx = (abs(dragStartScaleX) * factor).coerceIn(0.02f, 15f) * signX
                                                val targetSy = (abs(dragStartScaleY) * factor).coerceIn(0.02f, 15f) * signY
                                                viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = targetSx, scaleY = targetSy)
                                                liveTooltipText = "Scale: ${(abs(targetSx) * 100).toInt()}%"
                                            }
                                        }
                                    }
                                }

                                // 4. PIVOT TOOL
                                EditorTool.PIVOT -> {
                                    val totalDeltaX = (change.position.x - dragStartTouchPos.x)
                                    val totalDeltaY = (change.position.y - dragStartTouchPos.y)
                                    val currentLayer = viewModel.getSelectedLayer()
                                    if (currentLayer != null) {
                                        val rotRad = Math.toRadians(dragStartLayerRotation.toDouble())
                                        val cosA = cos(rotRad).toFloat()
                                        val sinA = sin(rotRad).toFloat()
                                        val localDeltaX = totalDeltaX * cosA + totalDeltaY * sinA
                                        val localDeltaY = -totalDeltaX * sinA + totalDeltaY * cosA

                                        val newPx = (dragStartPivotX + localDeltaX / (currentLayer.width * currentZoom)).coerceIn(0f, 1f)
                                        val newPy = (dragStartPivotY + localDeltaY / (currentLayer.height * currentZoom)).coerceIn(0f, 1f)
                                        viewModel.updateLayerPivot(currentLayer.id, newPx, newPy)
                                        liveTooltipText = "Pivot: (${(newPx * 100).toInt()}%, ${(newPy * 100).toInt()}%)"
                                    }
                                }
                                else -> {}
                            }
                        }
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val currentProject = viewModel.project.value
                        val currentF = viewModel.currentFrame.value
                        val currentZoom = viewModel.viewportZoom.value
                        val currentPan = viewModel.viewportPan.value
                        val currentTool = viewModel.activeTool.value

                        if (currentTool != EditorTool.HAND_PAN) {
                            val stageCenterX = size.width / 2f + currentPan.x
                            val stageCenterY = size.height / 2f + currentPan.y
                            val sortedLayers = currentProject.layers.filter { it.isVisible && !it.isLocked }.sortedByDescending { it.zIndex }

                            for (layer in sortedLayers) {
                                val transform = EasingFunctions.evaluateLayerAtFrame(layer, currentF)
                                val geo = computeViewportGeometry(layer, transform, stageCenterX, stageCenterY, currentZoom)
                                val local = geo.worldToLocal(tapOffset)
                                if (geo.isInsideBody(local)) {
                                    viewModel.selectLayer(layer.id)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
                }
        ) {
            val stageCenterX = size.width / 2f + pan.x
            val stageCenterY = size.height / 2f + pan.y

            // Resolution canvas bounds
            val resWidth = (project.resolution.width.toFloat() * 0.5f) * zoom
            val resHeight = (project.resolution.height.toFloat() * 0.5f) * zoom
            val resLeft = stageCenterX - resWidth / 2f
            val resTop = stageCenterY - resHeight / 2f

            // 1. Draw Viewport Canvas Surface
            when (canvasBgMode) {
                0 -> {
                    // Project Background Color
                    drawRect(
                        color = Color(project.backgroundColor),
                        topLeft = Offset(resLeft, resTop),
                        size = Size(resWidth, resHeight)
                    )
                }
                1 -> {
                    // Transparent Checkerboard
                    drawRect(color = Color(0xFF1B202C), topLeft = Offset(resLeft, resTop), size = Size(resWidth, resHeight))
                    val checkSize = 16f * zoom
                    var cx = resLeft
                    var row = 0
                    while (cx < resLeft + resWidth) {
                        var cy = resTop
                        var col = row
                        while (cy < resTop + resHeight) {
                            if (col % 2 == 0) {
                                drawRect(
                                    color = Color(0xFF262D3D),
                                    topLeft = Offset(cx, cy),
                                    size = Size(min(checkSize, resLeft + resWidth - cx), min(checkSize, resTop + resHeight - cy))
                                )
                            }
                            cy += checkSize
                            col++
                        }
                        cx += checkSize
                        row++
                    }
                }
                2 -> {
                    // Obsidian Dark Canvas
                    drawRect(color = Color(0xFF0F1117), topLeft = Offset(resLeft, resTop), size = Size(resWidth, resHeight))
                }
                else -> {
                    // Studio Light Canvas
                    drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(resLeft, resTop), size = Size(resWidth, resHeight))
                }
            }

            // 2. Studio Grid Matrix
            if (gridVisible) {
                val gridSize = 32f * zoom
                var gx = resLeft
                while (gx <= resLeft + resWidth) {
                    drawLine(
                        color = StudioBorder.copy(alpha = 0.35f),
                        start = Offset(gx, resTop),
                        end = Offset(gx, resTop + resHeight),
                        strokeWidth = 1f
                    )
                    gx += gridSize
                }
                var gy = resTop
                while (gy <= resTop + resHeight) {
                    drawLine(
                        color = StudioBorder.copy(alpha = 0.35f),
                        start = Offset(resLeft, gy),
                        end = Offset(resLeft + resWidth, gy),
                        strokeWidth = 1f
                    )
                    gy += gridSize
                }
            }

            // 3. Cinematic Rule of Thirds
            if (showRuleOfThirds) {
                val thirdW = resWidth / 3f
                val thirdH = resHeight / 3f
                val guideColor = StudioCyan.copy(alpha = 0.45f)
                drawLine(guideColor, Offset(resLeft + thirdW, resTop), Offset(resLeft + thirdW, resTop + resHeight), strokeWidth = 1f)
                drawLine(guideColor, Offset(resLeft + thirdW * 2f, resTop), Offset(resLeft + thirdW * 2f, resTop + resHeight), strokeWidth = 1f)
                drawLine(guideColor, Offset(resLeft, resTop + thirdH), Offset(resLeft + resWidth, resTop + thirdH), strokeWidth = 1f)
                drawLine(guideColor, Offset(resLeft, resTop + thirdH * 2f), Offset(resLeft + resWidth, resTop + thirdH * 2f), strokeWidth = 1f)

                val crossColor = StudioCyan.copy(alpha = 0.85f)
                listOf(
                    Offset(resLeft + thirdW, resTop + thirdH),
                    Offset(resLeft + thirdW * 2f, resTop + thirdH),
                    Offset(resLeft + thirdW, resTop + thirdH * 2f),
                    Offset(resLeft + thirdW * 2f, resTop + thirdH * 2f)
                ).forEach { pt ->
                    drawCircle(crossColor, radius = 3.5f, center = pt)
                }
            }

            // 4. Safe Zones (Action 90% & Title 80%)
            if (showSafeZones) {
                val actionW = resWidth * 0.90f
                val actionH = resHeight * 0.90f
                drawRect(
                    color = StudioOrange.copy(alpha = 0.45f),
                    topLeft = Offset(stageCenterX - actionW / 2f, stageCenterY - actionH / 2f),
                    size = Size(actionW, actionH),
                    style = Stroke(width = 1f, pathEffect = ViewportPathEngine.dashEffect)
                )

                val titleW = resWidth * 0.80f
                val titleH = resHeight * 0.80f
                drawRect(
                    color = StudioCyan.copy(alpha = 0.55f),
                    topLeft = Offset(stageCenterX - titleW / 2f, stageCenterY - titleH / 2f),
                    size = Size(titleW, titleH),
                    style = Stroke(width = 1f, pathEffect = ViewportPathEngine.dashEffect)
                )
            }

            // 5. Origin Crosshair (0,0)
            drawLine(
                color = KorvaVioletLight.copy(alpha = 0.45f),
                start = Offset(stageCenterX - 20f, stageCenterY),
                end = Offset(stageCenterX + 20f, stageCenterY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = KorvaVioletLight.copy(alpha = 0.45f),
                start = Offset(stageCenterX, stageCenterY - 20f),
                end = Offset(stageCenterX, stageCenterY + 20f),
                strokeWidth = 1.5f
            )

            // 6. Magnetic Snap Visual Lines
            if (snapGuideX != null) {
                drawLine(
                    color = StudioPink,
                    start = Offset(stageCenterX, resTop),
                    end = Offset(stageCenterX, resTop + resHeight),
                    strokeWidth = 2f
                )
            }
            if (snapGuideY != null) {
                drawLine(
                    color = StudioPink,
                    start = Offset(resLeft, stageCenterY),
                    end = Offset(resLeft + resWidth, stageCenterY),
                    strokeWidth = 2f
                )
            }

            // 7. Motion Trajectory Path
            if (showMotionTrajectory && selectedLayer != null && selectedLayer.keyframes.size > 1 && !isPlaying) {
                val path = ViewportPathEngine.tempPath1
                path.reset()
                var first = true

                for (f in 0 until project.totalFrames step 1) {
                    val tf = EasingFunctions.evaluateLayerAtFrame(selectedLayer, f.toFloat())
                    val px = stageCenterX + tf.x * zoom
                    val py = stageCenterY + tf.y * zoom
                    if (first) {
                        path.moveTo(px, py)
                        first = false
                    } else {
                        path.lineTo(px, py)
                    }
                }

                drawPath(
                    path = path,
                    color = KorvaVioletPrimary.copy(alpha = 0.75f),
                    style = Stroke(width = 2.2f, pathEffect = ViewportPathEngine.dashEffect)
                )

                selectedLayer.keyframes.forEach { kf ->
                    val kfX = stageCenterX + kf.x * zoom
                    val kfY = stageCenterY + kf.y * zoom
                    drawCircle(color = StudioCyan, radius = 4f, center = Offset(kfX, kfY))
                    drawCircle(color = Color.White, radius = 2f, center = Offset(kfX, kfY))
                }

                if (selectedTransform != null) {
                    val curX = stageCenterX + selectedTransform.x * zoom
                    val curY = stageCenterY + selectedTransform.y * zoom
                    drawCircle(color = StudioPink, radius = 6f, center = Offset(curX, curY))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(curX, curY))
                }
            }

            // 8. Ghosting Onion Skinning
            if (onionSkinEnabled && !isPlaying) {
                for (step in 1..pastOnionCount) {
                    val pastF = currentFrame - step * 2f
                    if (pastF >= 0) {
                        val ghostAlpha = 0.28f / step
                        drawProjectLayers(
                            layers = project.layers,
                            frame = pastF,
                            stageCenterX = stageCenterX,
                            stageCenterY = stageCenterY,
                            zoom = zoom,
                            tintOverride = StudioCyan.copy(alpha = ghostAlpha)
                        )
                    }
                }
                for (step in 1..futureOnionCount) {
                    val futureF = currentFrame + step * 2f
                    if (futureF < project.totalFrames) {
                        val ghostAlpha = 0.28f / step
                        drawProjectLayers(
                            layers = project.layers,
                            frame = futureF,
                            stageCenterX = stageCenterX,
                            stageCenterY = stageCenterY,
                            zoom = zoom,
                            tintOverride = StudioOrange.copy(alpha = ghostAlpha)
                        )
                    }
                }
            }

            // 9. Core Frame Layer Rendering
            drawProjectLayers(
                layers = project.layers,
                frame = currentFrame,
                stageCenterX = stageCenterX,
                stageCenterY = stageCenterY,
                zoom = zoom,
                tintOverride = null
            )

            // 9.5 Render Live 60fps VFX Particle Physics System
            drawVFXParticles(
                vfxEngine = viewModel.vfxEngine,
                stageCenterX = stageCenterX,
                stageCenterY = stageCenterY,
                zoom = zoom
            )

            // 10. Frame Outline
            drawRect(
                color = StudioBorderLight.copy(alpha = 0.7f),
                topLeft = Offset(resLeft, resTop),
                size = Size(resWidth, resHeight),
                style = Stroke(width = 1.5f)
            )

            // 11. World-Matrix Bound Interactive Transform Gizmo
            if (selectedLayer != null && selectedTransform != null && !isPlaying) {
                val geo = computeViewportGeometry(
                    layer = selectedLayer,
                    transform = selectedTransform,
                    stageCenterX = stageCenterX,
                    stageCenterY = stageCenterY,
                    zoom = zoom
                )

                when (activeTool) {
                    EditorTool.ROTATE -> {
                        // 🔵 AXIAL SYNCHRONIZED CIRCULAR ROTATION DIAL GIZMO 🔵
                        // Compute exact maximum radial extent from rotation axis (pivot) to all corners
                        val maxCornerDist = maxOf(
                            hypot(geo.pTL.x, geo.pTL.y),
                            hypot(geo.pTR.x, geo.pTR.y),
                            hypot(geo.pBR.x, geo.pBR.y),
                            hypot(geo.pBL.x, geo.pBL.y)
                        )
                        val rotRadius = max(maxCornerDist + 32f, 58f)

                        // 1. Semi-transparent disc backdrop centering the element inside the dial
                        drawCircle(
                            color = StudioCyan.copy(alpha = 0.04f),
                            radius = rotRadius,
                            center = geo.origin
                        )

                        // 2. Synchronized Rotating Element Bounding Box
                        withTransform({
                            translate(geo.origin.x, geo.origin.y)
                            rotate(geo.rotationDeg, Offset.Zero)
                        }) {
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pTL, geo.pTR, strokeWidth = 1.8f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pTR, geo.pBR, strokeWidth = 1.8f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pBR, geo.pBL, strokeWidth = 1.8f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pBL, geo.pTL, strokeWidth = 1.8f, pathEffect = ViewportPathEngine.dashEffect)

                            // Corner bounding markers
                            val cLen = 10f
                            drawLine(StudioCyan, geo.pTL, Offset(geo.pTL.x + cLen, geo.pTL.y), strokeWidth = 2.5f)
                            drawLine(StudioCyan, geo.pTL, Offset(geo.pTL.x, geo.pTL.y + cLen), strokeWidth = 2.5f)
                            drawLine(StudioCyan, geo.pTR, Offset(geo.pTR.x - cLen, geo.pTR.y), strokeWidth = 2.5f)
                            drawLine(StudioCyan, geo.pTR, Offset(geo.pTR.x, geo.pTR.y + cLen), strokeWidth = 2.5f)
                            drawLine(StudioCyan, geo.pBR, Offset(geo.pBR.x - cLen, geo.pBR.y), strokeWidth = 2.5f)
                            drawLine(StudioCyan, geo.pBR, Offset(geo.pBR.x, geo.pBR.y - cLen), strokeWidth = 2.5f)
                            drawLine(StudioCyan, geo.pBL, Offset(geo.pBL.x + cLen, geo.pBL.y), strokeWidth = 2.5f)
                            drawLine(StudioCyan, geo.pBL, Offset(geo.pBL.x, geo.pBL.y - cLen), strokeWidth = 2.5f)
                        }

                        // 3. Outer glowing circular track
                        drawCircle(
                            color = StudioCyan.copy(alpha = 0.16f),
                            radius = rotRadius + 4f,
                            center = geo.origin,
                            style = Stroke(width = 6f)
                        )

                        // 4. Main Circular Orbit Track (Concentric around layer axis)
                        drawCircle(
                            color = StudioCyan.copy(alpha = 0.85f),
                            radius = rotRadius,
                            center = geo.origin,
                            style = Stroke(width = 2.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                        )

                        // 5. Degree Sweep Arc (from top 0° to current angle)
                        val startAngleArc = -90f
                        val sweepAngleArc = (geo.rotationDeg % 360f)
                        drawArc(
                            color = StudioCyan.copy(alpha = 0.18f),
                            startAngle = startAngleArc,
                            sweepAngle = sweepAngleArc,
                            useCenter = true,
                            topLeft = Offset(geo.origin.x - rotRadius, geo.origin.y - rotRadius),
                            size = Size(rotRadius * 2f, rotRadius * 2f)
                        )
                        drawArc(
                            color = StudioCyan,
                            startAngle = startAngleArc,
                            sweepAngle = sweepAngleArc,
                            useCenter = false,
                            topLeft = Offset(geo.origin.x - rotRadius, geo.origin.y - rotRadius),
                            size = Size(rotRadius * 2f, rotRadius * 2f),
                            style = Stroke(width = 3f)
                        )

                        // 6. Cardinal & Diagonal Compass Ticks (every 30° and 45°)
                        for (deg in 0 until 360 step 30) {
                            val rad = Math.toRadians(deg.toDouble() - 90.0).toFloat()
                            val isCardinal = deg % 90 == 0
                            val tLen = if (isCardinal) 10f else 5f
                            val p1 = Offset(geo.origin.x + (rotRadius - tLen) * cos(rad), geo.origin.y + (rotRadius - tLen) * sin(rad))
                            val p2 = Offset(geo.origin.x + (rotRadius + tLen) * cos(rad), geo.origin.y + (rotRadius + tLen) * sin(rad))
                            drawLine(
                                color = if (isCardinal) StudioCyan else StudioCyan.copy(alpha = 0.45f),
                                start = p1,
                                end = p2,
                                strokeWidth = if (isCardinal) 2.5f else 1.2f
                            )
                        }

                        // 7. Radial pointer line connecting rotation axis to outer knob
                        val curRad = Math.toRadians(geo.rotationDeg.toDouble() - 90.0).toFloat()
                        val knobPos = Offset(geo.origin.x + rotRadius * cos(curRad), geo.origin.y + rotRadius * sin(curRad))
                        drawLine(
                            color = StudioCyan,
                            start = geo.origin,
                            end = knobPos,
                            strokeWidth = 2.4f
                        )

                        // 8. Blue Circular Dial Control Knob 🔵
                        drawCircle(color = Color(0x660284C7), radius = 20f, center = knobPos)
                        drawCircle(color = Color(0xFF0284C7), radius = 12f, center = knobPos)
                        drawCircle(color = StudioCyan, radius = 12f, center = knobPos, style = Stroke(2.5f))
                        drawCircle(color = Color.White, radius = 4f, center = knobPos)

                        // 9. Precision Rotation Axis Reticle / Crosshair (محور الدوران)
                        val retLen = 14f
                        drawLine(StudioCyan.copy(alpha = 0.8f), Offset(geo.origin.x - retLen, geo.origin.y), Offset(geo.origin.x + retLen, geo.origin.y), strokeWidth = 1.5f)
                        drawLine(StudioCyan.copy(alpha = 0.8f), Offset(geo.origin.x, geo.origin.y - retLen), Offset(geo.origin.x, geo.origin.y + retLen), strokeWidth = 1.5f)
                        drawCircle(color = StudioCyan, radius = 6f, center = geo.origin, style = Stroke(2f))
                        drawCircle(color = Color.White, radius = 2.5f, center = geo.origin)
                    }

                    EditorTool.SELECT -> {
                        // EXCLUSIVE MOVE & SELECTION GIZMO
                        withTransform({
                            translate(geo.origin.x, geo.origin.y)
                            rotate(geo.rotationDeg, Offset.Zero)
                        }) {
                            // Primary Bounding Box
                            drawLine(KorvaVioletPrimary, geo.pTL, geo.pTR, strokeWidth = 2.2f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(KorvaVioletPrimary, geo.pTR, geo.pBR, strokeWidth = 2.2f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(KorvaVioletPrimary, geo.pBR, geo.pBL, strokeWidth = 2.2f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(KorvaVioletPrimary, geo.pBL, geo.pTL, strokeWidth = 2.2f, pathEffect = ViewportPathEngine.dashEffect)

                            // 4 Corner Brackets
                            val cLen = 12f
                            listOf(
                                Pair(geo.pTL, Offset(1f, 1f)),
                                Pair(geo.pTR, Offset(-1f, 1f)),
                                Pair(geo.pBR, Offset(-1f, -1f)),
                                Pair(geo.pBL, Offset(1f, -1f))
                            ).forEach { (corner, dir) ->
                                drawLine(Color.White, corner, Offset(corner.x + dir.x * cLen, corner.y), strokeWidth = 2.5f)
                                drawLine(Color.White, corner, Offset(corner.x, corner.y + dir.y * cLen), strokeWidth = 2.5f)
                            }

                            // Center Move Pin
                            drawCircle(color = KorvaVioletPrimary, radius = 6.5f, center = Offset.Zero)
                            drawCircle(color = Color.White, radius = 2.5f, center = Offset.Zero)
                        }
                    }

                    EditorTool.SCALE -> {
                        // EXCLUSIVE SCALE GIZMO
                        withTransform({
                            translate(geo.origin.x, geo.origin.y)
                            rotate(geo.rotationDeg, Offset.Zero)
                        }) {
                            drawLine(StudioOrange, geo.pTL, geo.pTR, strokeWidth = 2f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioOrange, geo.pTR, geo.pBR, strokeWidth = 2f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioOrange, geo.pBR, geo.pBL, strokeWidth = 2f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioOrange, geo.pBL, geo.pTL, strokeWidth = 2f, pathEffect = ViewportPathEngine.dashEffect)

                            val handleRadius = 8f
                            listOf(geo.pTL, geo.pTR, geo.pBR, geo.pBL).forEach { cornerPt ->
                                drawCircle(color = Color.White, radius = handleRadius, center = cornerPt)
                                drawCircle(color = StudioOrange, radius = handleRadius, center = cornerPt, style = Stroke(2.5f))
                            }

                            listOf(geo.midTop, geo.midRight, geo.midBottom, geo.midLeft).forEach { midPt ->
                                drawCircle(color = StudioOrange, radius = 5.5f, center = midPt)
                                drawCircle(color = Color.White, radius = 2.2f, center = midPt)
                            }
                        }
                    }

                    EditorTool.PIVOT -> {
                        // PIVOT ANCHOR GIZMO
                        withTransform({
                            translate(geo.origin.x, geo.origin.y)
                            rotate(geo.rotationDeg, Offset.Zero)
                        }) {
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pTL, geo.pTR, strokeWidth = 1.5f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pTR, geo.pBR, strokeWidth = 1.5f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pBR, geo.pBL, strokeWidth = 1.5f, pathEffect = ViewportPathEngine.dashEffect)
                            drawLine(StudioCyan.copy(alpha = 0.5f), geo.pBL, geo.pTL, strokeWidth = 1.5f, pathEffect = ViewportPathEngine.dashEffect)

                            drawCircle(color = Color(0x4406B6D4), radius = 18f, center = Offset.Zero)
                            drawCircle(color = StudioCyan, radius = 9f, center = Offset.Zero, style = Stroke(2f))
                            drawCircle(color = Color.White, radius = 3.5f, center = Offset.Zero)
                            drawLine(color = StudioCyan, start = Offset(-16f, 0f), end = Offset(16f, 0f), strokeWidth = 2f)
                            drawLine(color = StudioCyan, start = Offset(0f, -16f), end = Offset(0f, 16f), strokeWidth = 2f)
                        }
                    }

                    EditorTool.HAND_PAN -> {
                        // HAND PAN: Minimal subtle outline
                        withTransform({
                            translate(geo.origin.x, geo.origin.y)
                            rotate(geo.rotationDeg, Offset.Zero)
                        }) {
                            drawLine(Color.White.copy(alpha = 0.25f), geo.pTL, geo.pTR, strokeWidth = 1f)
                            drawLine(Color.White.copy(alpha = 0.25f), geo.pTR, geo.pBR, strokeWidth = 1f)
                            drawLine(Color.White.copy(alpha = 0.25f), geo.pBR, geo.pBL, strokeWidth = 1f)
                            drawLine(Color.White.copy(alpha = 0.25f), geo.pBL, geo.pTL, strokeWidth = 1f)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // TOP VIEWPORT SETTINGS BUTTON & POPUP (قائمة إعدادات منفذ العرض المنبثقة فقط)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            Surface(
                color = if (showViewportSettingsMenu || activeOverlaysCount > 0) KorvaVioletDark.copy(alpha = 0.85f) else StudioPanelDark.copy(alpha = 0.88f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (showViewportSettingsMenu || activeOverlaysCount > 0) KorvaVioletPrimary else StudioBorder
                ),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .testTag("viewport_settings_popup_button")
                    .clickable { showViewportSettingsMenu = !showViewportSettingsMenu }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Viewport Settings",
                        tint = if (activeOverlaysCount > 0) KorvaVioletLight else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Settings",
                        color = if (activeOverlaysCount > 0) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (activeOverlaysCount > 0) {
                        Surface(
                            color = KorvaVioletPrimary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = activeOverlaysCount.toString(),
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 📋 POPUP MENU FOR VIEWPORT SETTINGS (قائمة إعدادات منفذ العرض المنبثقة)
            DropdownMenu(
                expanded = showViewportSettingsMenu,
                onDismissRequest = { showViewportSettingsMenu = false },
                modifier = Modifier
                    .background(StudioPanelDark)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .width(260.dp)
            ) {
                // Menu Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = KorvaVioletLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Viewport Guides & Overlays",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

                // 1. Onion Skinning (Onion)
                ViewportMenuItem(
                    icon = Icons.Default.Layers,
                    title = "Onion Skinning",
                    subtitle = "Ghost past & future animation frames",
                    isActive = onionSkinEnabled,
                    onClick = { viewModel.toggleOnionSkin() }
                )

                // 2. Motion Trajectory Path (Path)
                ViewportMenuItem(
                    icon = Icons.Default.Timeline,
                    title = "Motion Path",
                    subtitle = "Spatial trajectory arcs & keyframe pins",
                    isActive = showMotionTrajectory,
                    onClick = { viewModel.toggleMotionTrajectory() }
                )

                // 3. Grid Alignment (Grid)
                ViewportMenuItem(
                    icon = Icons.Default.GridOn,
                    title = "Grid Alignment",
                    subtitle = "Precision canvas coordinate grid",
                    isActive = gridVisible,
                    onClick = { viewModel.toggleGrid() }
                )

                // 4. Snap to Grid (Snap)
                ViewportMenuItem(
                    icon = Icons.Default.CenterFocusStrong,
                    title = "Snap to Grid",
                    subtitle = "15° angular steps & smooth increments",
                    isActive = snapToGrid,
                    onClick = { viewModel.toggleSnapToGrid() }
                )

                // 5. Safe Zones (Safe)
                ViewportMenuItem(
                    icon = Icons.Default.AspectRatio,
                    title = "Safe Zones",
                    subtitle = "Action safe (90%) & Title safe (80%)",
                    isActive = showSafeZones,
                    onClick = { viewModel.toggleSafeZones() }
                )

                // 6. Rule of Thirds (3rds)
                ViewportMenuItem(
                    icon = Icons.Default.Apps,
                    title = "Rule of Thirds",
                    subtitle = "3x3 cinematic golden ratio guides",
                    isActive = showRuleOfThirds,
                    onClick = { viewModel.toggleRuleOfThirds() }
                )

                HorizontalDivider(color = StudioBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

                // 7. Canvas Background Mode (Canvas BG)
                val bgSubtitle = when (canvasBgMode) {
                    1 -> "Checkerboard (Alpha Transparency)"
                    2 -> "Studio Light"
                    else -> "Obsidian Dark (Default)"
                }
                ViewportMenuItem(
                    icon = Icons.Default.Palette,
                    title = "Canvas Background",
                    subtitle = bgSubtitle,
                    isActive = canvasBgMode != 0,
                    onClick = { viewModel.cycleCanvasBg() }
                )
            }
        }

        // -------------------------------------------------------------
        // LIVE DRAG READOUT PILL
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isDragging && liveTooltipText.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Surface(
                color = StudioSurfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KorvaVioletPrimary.copy(alpha = 0.7f)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = liveTooltipText,
                    color = KorvaVioletLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewportMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isActive) KorvaVioletDark.copy(alpha = 0.35f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (isActive) KorvaVioletPrimary.copy(alpha = 0.2f) else StudioSurfaceDark,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isActive) KorvaVioletPrimary else StudioBorder
                    )
                ) {
                    Box(modifier = Modifier.padding(5.dp)) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isActive) KorvaVioletLight else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        color = if (isActive) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = TextMuted,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Custom toggle indicator pill
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(18.dp)
                    .background(
                        if (isActive) KorvaVioletPrimary else StudioSurfaceDark,
                        RoundedCornerShape(9.dp)
                    )
                    .border(0.5.dp, if (isActive) KorvaVioletPrimary else StudioBorder, RoundedCornerShape(9.dp))
                    .padding(2.dp),
                contentAlignment = if (isActive) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(if (isActive) Color.White else TextMuted, RoundedCornerShape(7.dp))
                )
            }
        }
    }
}

@Composable
private fun ViewportToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isActive) KorvaVioletDark.copy(alpha = 0.5f) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = if (isActive) androidx.compose.foundation.BorderStroke(0.5.dp, KorvaVioletPrimary) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) KorvaVioletLight else TextMuted,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                color = if (isActive) TextPrimary else TextMuted,
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * High-Speed Layer Stack Renderer.
 */
private fun DrawScope.drawProjectLayers(
    layers: List<AnimationLayer>,
    frame: Float,
    stageCenterX: Float,
    stageCenterY: Float,
    zoom: Float,
    tintOverride: Color?
) {
    val sortedLayers = layers.filter { it.isVisible }.sortedBy { it.zIndex }

    for (layer in sortedLayers) {
        val transform = EasingFunctions.evaluateLayerAtFrame(layer, frame)
        val geo = computeViewportGeometry(layer, transform, stageCenterX, stageCenterY, zoom)

        val layerW = layer.width * zoom
        val layerH = layer.height * zoom
        val layerLeft = geo.l0
        val layerTop = geo.t0

        withTransform({
            translate(geo.origin.x, geo.origin.y)
            rotate(geo.rotationDeg, Offset.Zero)
            scale(transform.scaleX, transform.scaleY, Offset.Zero)
        }) {
            val drawAlpha = if (tintOverride != null) tintOverride.alpha else transform.opacity
            val fillColor = tintOverride ?: Color(layer.shapeStyle.fillColor).copy(alpha = drawAlpha)

            when (layer.shapeKind) {
                ShapeKind.RECTANGLE -> {
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(layerLeft, layerTop),
                        size = Size(layerW, layerH)
                    )
                }
                ShapeKind.ROUNDED_RECT -> {
                    val cr = (layer.shapeStyle.cornerRadius * zoom).coerceAtLeast(4f)
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(layerLeft, layerTop),
                        size = Size(layerW, layerH),
                        cornerRadius = CornerRadius(cr, cr)
                    )
                }
                ShapeKind.CIRCLE -> {
                    drawOval(
                        color = fillColor,
                        topLeft = Offset(layerLeft, layerTop),
                        size = Size(layerW, layerH)
                    )
                }
                ShapeKind.STAR -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    val cx = layerLeft + layerW / 2f
                    val cy = layerTop + layerH / 2f
                    val rOuter = min(layerW, layerH) / 2f
                    val rInner = rOuter * 0.45f
                    val points = 5
                    for (i in 0 until points * 2) {
                        val r = if (i % 2 == 0) rOuter else rInner
                        val angle = (i * PI / points - PI / 2).toFloat()
                        val px = cx + r * cos(angle)
                        val py = cy + r * sin(angle)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
                ShapeKind.TRIANGLE -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    path.moveTo(layerLeft + layerW / 2f, layerTop)
                    path.lineTo(layerLeft + layerW, layerTop + layerH)
                    path.lineTo(layerLeft, layerTop + layerH)
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
                ShapeKind.DIAMOND -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    val midX = layerLeft + layerW / 2f
                    val midY = layerTop + layerH / 2f
                    path.moveTo(midX, layerTop)
                    path.lineTo(layerLeft + layerW, midY)
                    path.lineTo(midX, layerTop + layerH)
                    path.lineTo(layerLeft, midY)
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
                ShapeKind.ARROW -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    val midX = layerLeft + layerW / 2f
                    val stemW = layerW * 0.25f
                    path.moveTo(midX, layerTop)
                    path.lineTo(layerLeft + layerW, layerTop + layerH * 0.45f)
                    path.lineTo(midX + stemW, layerTop + layerH * 0.45f)
                    path.lineTo(midX + stemW, layerTop + layerH)
                    path.lineTo(midX - stemW, layerTop + layerH)
                    path.lineTo(midX - stemW, layerTop + layerH * 0.45f)
                    path.lineTo(layerLeft, layerTop + layerH * 0.45f)
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
                ShapeKind.SWORD -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    val midX = layerLeft + layerW / 2f
                    val bladeW = layerW * 0.2f
                    path.moveTo(midX, layerTop)
                    path.lineTo(midX + bladeW, layerTop + layerH * 0.2f)
                    path.lineTo(midX + bladeW, layerTop + layerH * 0.7f)
                    path.lineTo(layerLeft + layerW * 0.85f, layerTop + layerH * 0.7f)
                    path.lineTo(layerLeft + layerW * 0.85f, layerTop + layerH * 0.77f)
                    path.lineTo(midX + bladeW * 0.6f, layerTop + layerH * 0.77f)
                    path.lineTo(midX + bladeW * 0.6f, layerTop + layerH * 0.95f)
                    path.lineTo(midX - bladeW * 0.6f, layerTop + layerH * 0.95f)
                    path.lineTo(midX - bladeW * 0.6f, layerTop + layerH * 0.77f)
                    path.lineTo(layerLeft + layerW * 0.15f, layerTop + layerH * 0.77f)
                    path.lineTo(layerLeft + layerW * 0.15f, layerTop + layerH * 0.7f)
                    path.lineTo(midX - bladeW, layerTop + layerH * 0.7f)
                    path.lineTo(midX - bladeW, layerTop + layerH * 0.2f)
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
                ShapeKind.SHIELD -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    val midX = layerLeft + layerW / 2f
                    path.moveTo(midX, layerTop + layerH)
                    path.lineTo(layerLeft, layerTop + layerH * 0.35f)
                    path.lineTo(layerLeft, layerTop)
                    path.lineTo(layerLeft + layerW, layerTop)
                    path.lineTo(layerLeft + layerW, layerTop + layerH * 0.35f)
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
                ShapeKind.SLIME -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    val cx = layerLeft + layerW / 2f
                    val cy = layerTop + layerH * 0.6f
                    val rx = layerW * 0.48f
                    val ry = layerH * 0.38f
                    path.moveTo(cx - rx, cy)
                    path.cubicTo(cx - rx, layerTop, cx, layerTop * 0.8f, cx, layerTop + layerH * 0.1f)
                    path.cubicTo(cx, layerTop * 0.8f, cx + rx, layerTop, cx + rx, cy)
                    path.cubicTo(cx + rx, layerTop + layerH, cx - rx, layerTop + layerH, cx - rx, cy)
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
                ShapeKind.COIN -> {
                    drawOval(
                        color = fillColor,
                        topLeft = Offset(layerLeft, layerTop),
                        size = Size(layerW, layerH)
                    )
                    drawOval(
                        color = StudioYellow,
                        topLeft = Offset(layerLeft + layerW * 0.15f, layerTop + layerH * 0.15f),
                        size = Size(layerW * 0.7f, layerH * 0.7f),
                        style = Stroke(width = 2f * zoom)
                    )
                }
                ShapeKind.SLASH_FX -> {
                    val path = ViewportPathEngine.tempPath1
                    path.reset()
                    path.moveTo(layerLeft, layerTop + layerH)
                    path.cubicTo(layerLeft + layerW * 0.3f, layerTop + layerH * 0.2f, layerLeft + layerW * 0.7f, layerTop, layerLeft + layerW, layerTop)
                    path.cubicTo(layerLeft + layerW * 0.6f, layerTop + layerH * 0.4f, layerLeft + layerW * 0.2f, layerTop + layerH * 0.8f, layerLeft, layerTop + layerH)
                    path.close()
                    drawPath(path = path, color = fillColor)
                }
            }
        }
    }
}

/**
 * Real-Time 60/120fps Particle VFX Renderer.
 * Evaluates individual particle states from VFXSimulationEngine with Additive Blending.
 */
private fun DrawScope.drawVFXParticles(
    vfxEngine: com.example.engine.vfx.VFXSimulationEngine,
    stageCenterX: Float,
    stageCenterY: Float,
    zoom: Float
) {
    val particles = vfxEngine.pool.particles
    val isAdditive = vfxEngine.effect.blendMode == com.example.model.vfx.BlendMode.ADDITIVE

    for (p in particles) {
        if (!p.isActive) continue

        val px = stageCenterX + p.position.x * zoom
        val py = stageCenterY + p.position.y * zoom
        val sw = max(p.scale.x * zoom, 1.5f)
        val sh = max(p.scale.y * zoom, 1.5f)

        val alpha = (p.alpha * p.color.a).coerceIn(0f, 1f)
        if (alpha <= 0.01f) continue

        val particleColor = Color(
            red = p.color.r / 255f,
            green = p.color.g / 255f,
            blue = p.color.b / 255f,
            alpha = alpha
        )

        withTransform({
            translate(px, py)
            rotate(p.rotation, Offset.Zero)
        }) {
            if (isAdditive) {
                // Luminous additive glow halo
                drawCircle(
                    color = particleColor.copy(alpha = alpha * 0.35f),
                    radius = max(sw, sh) * 0.9f,
                    center = Offset.Zero
                )
            }
            drawOval(
                color = particleColor,
                topLeft = Offset(-sw / 2f, -sh / 2f),
                size = Size(sw, sh)
            )
        }
    }
}
