package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

// Blender-inspired signature colors
private val BlenderDarkBg = Color(0xFF1B1C1E)
private val BlenderPanelBg = Color(0xFF232428)
private val BlenderSurface = Color(0xFF2D2E34)
private val BlenderSurfaceHighlight = Color(0xFF3B3C44)
private val BlenderBorder = Color(0xFF383942)
private val BlenderPlayheadBlue = Color(0xFF478CFF)
private val BlenderKeyframeGold = Color(0xFFFFB84D)
private val BlenderKeyframeSelected = Color(0xFFFFFFFF)
private val BlenderPlayheadHead = Color(0xFF5CA0FF)

@Composable
fun BottomTimelinePanel(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.project.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isCollapsed by viewModel.isBottomTimelineCollapsed.collectAsState()
    val timelineHeightDp by viewModel.timelineHeightDp.collectAsState()
    val timelineZoom by viewModel.timelineZoom.collectAsState()
    val timelineFitToScreen by viewModel.timelineFitToScreen.collectAsState()
    val selectedKeyframe by viewModel.selectedKeyframe.collectAsState()

    val selectedLayer = project.layers.find { it.id == selectedLayerId }
    val currentKfExists = selectedLayer?.keyframes?.any { it.frame == currentFrame.toInt() } == true

    val density = LocalDensity.current

    val panelHeight by animateDpAsState(
        targetValue = if (isCollapsed) 36.dp else timelineHeightDp.coerceAtLeast(140f).dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TimelinePanelHeight"
    )

    Surface(
        color = BlenderDarkBg,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight)
            .border(width = 1.dp, color = BlenderBorder)
            .testTag("bottom_timeline_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Sleek Drag Handle to adjust height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(BlenderSurface)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = with(density) { dragAmount.y.toDp().value }
                            viewModel.adjustTimelineHeight(deltaDp)
                        }
                    }
                    .clickable { viewModel.toggleTimelineHeightPreset() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(if (isCollapsed) BlenderPlayheadBlue else BlenderBorder)
                )
            }

            // 2. Blender-Style Transport Toolbar
            BlenderTransportToolbar(
                project = project,
                currentFrame = currentFrame,
                isPlaying = isPlaying,
                isCollapsed = isCollapsed,
                currentKfExists = currentKfExists,
                timelineFitToScreen = timelineFitToScreen,
                onToggleCollapse = { viewModel.toggleBottomTimeline() },
                onTogglePlay = { viewModel.togglePlay() },
                onJumpStart = { viewModel.jumpToStart() },
                onJumpEnd = { viewModel.jumpToEnd() },
                onStepBackward = { viewModel.stepBackward() },
                onStepForward = { viewModel.stepForward() },
                onJumpPrevKf = { viewModel.jumpToPrevKeyframe() },
                onJumpNextKf = { viewModel.jumpToNextKeyframe() },
                onToggleKeyframe = { viewModel.addOrUpdateKeyframeOnCurrentFrame() },
                onDeleteKeyframe = { viewModel.deleteKeyframeOnCurrentFrame() },
                onToggleFit = { viewModel.toggleTimelineFitToScreen() },
                onZoomIn = { viewModel.zoomTimelineIn() },
                onZoomOut = { viewModel.zoomTimelineOut() },
                onCycleLoop = {
                    val next = when (project.loopMode) {
                        LoopMode.REPEAT -> LoopMode.PING_PONG
                        LoopMode.PING_PONG -> LoopMode.ONCE
                        LoopMode.ONCE -> LoopMode.REPEAT
                    }
                    viewModel.setLoopMode(next)
                },
                onSetFps = { viewModel.setFps(it) },
                onSetTotalFrames = { viewModel.setTotalFrames(it) }
            )

            // 3. Blender-Style Timeline Dopesheet (Ruler + Multi-Track Canvas)
            if (!isCollapsed) {
                BlenderTimelineTracks(
                    project = project,
                    currentFrame = currentFrame,
                    selectedLayerId = selectedLayerId,
                    selectedKeyframe = selectedKeyframe,
                    timelineZoom = timelineZoom,
                    timelineFitToScreen = timelineFitToScreen,
                    onScrub = { viewModel.scrubToFrame(it) },
                    onSelectLayer = { viewModel.selectLayer(it) },
                    onSelectKeyframe = { lId, kf -> viewModel.setSelectedKeyframe(lId, kf) },
                    onMoveKeyframe = { lId, oldF, newF -> viewModel.moveKeyframe(lId, oldF, newF) },
                    onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                    onToggleLock = { viewModel.toggleLayerLock(it) }
                )
            }
        }
    }
}

// =============================================================================
// 1. BLENDER TRANSPORT TOOLBAR
// =============================================================================
@Composable
private fun BlenderTransportToolbar(
    project: KorProject,
    currentFrame: Float,
    isPlaying: Boolean,
    isCollapsed: Boolean,
    currentKfExists: Boolean,
    timelineFitToScreen: Boolean,
    onToggleCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onJumpPrevKf: () -> Unit,
    onJumpNextKf: () -> Unit,
    onToggleKeyframe: () -> Unit,
    onDeleteKeyframe: () -> Unit,
    onToggleFit: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCycleLoop: () -> Unit,
    onSetFps: (Int) -> Unit,
    onSetTotalFrames: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(31.dp)
            .background(BlenderPanelBg)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Minimize / Expand button
        IconButton(
            onClick = onToggleCollapse,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle Timeline",
                tint = BlenderPlayheadBlue,
                modifier = Modifier.size(16.dp)
            )
        }

        // Current Frame Display Box (Blender Style input pill)
        Surface(
            color = BlenderSurface,
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(1.dp, BlenderPlayheadBlue.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${currentFrame.toInt()}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Frame Range (Start / End)
        Surface(
            color = BlenderSurface,
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(0.5.dp, BlenderBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("0", color = Color.Gray, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                Text("-", color = BlenderBorder, fontSize = 9.sp)
                Text("${project.totalFrames}", color = BlenderKeyframeGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        VerticalDivider(color = BlenderBorder, modifier = Modifier.height(14.dp))

        // Transport Controls (Blender Layout: |<  <  Play/Pause  >  >|)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Jump Start (|<)
            IconButton(onClick = onJumpStart, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.FirstPage, contentDescription = "Start", tint = Color.LightGray, modifier = Modifier.size(15.dp))
            }

            // Prev Keyframe (⏮)
            IconButton(onClick = onJumpPrevKf, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev KF", tint = BlenderKeyframeGold, modifier = Modifier.size(15.dp))
            }

            // Step Back (<)
            IconButton(onClick = onStepBackward, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ArrowLeft, contentDescription = "Step Back", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Main Play / Pause Button
            Surface(
                color = if (isPlaying) Color(0xFFE05252) else BlenderPlayheadBlue,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (isPlaying) Color(0xFFFF7878) else Color(0xFF70A8FF)),
                modifier = Modifier
                    .clickable { onTogglePlay() }
                    .testTag("timeline_play_pause_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isPlaying) "PAUSE" else "PLAY",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Step Forward (>)
            IconButton(onClick = onStepForward, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ArrowRight, contentDescription = "Step Forward", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Next Keyframe (⏭)
            IconButton(onClick = onJumpNextKf, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next KF", tint = BlenderKeyframeGold, modifier = Modifier.size(15.dp))
            }

            // Jump End (>|)
            IconButton(onClick = onJumpEnd, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.LastPage, contentDescription = "End", tint = Color.LightGray, modifier = Modifier.size(15.dp))
            }
        }

        VerticalDivider(color = BlenderBorder, modifier = Modifier.height(14.dp))

        // Keyframing Controls (Blender Diamond Icon)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Surface(
                color = if (currentKfExists) BlenderKeyframeGold.copy(alpha = 0.25f) else BlenderSurface,
                shape = RoundedCornerShape(3.dp),
                border = BorderStroke(
                    1.dp,
                    if (currentKfExists) BlenderKeyframeGold else BlenderBorder
                ),
                modifier = Modifier
                    .clickable { onToggleKeyframe() }
                    .testTag("add_keyframe_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Keyframe",
                        tint = if (currentKfExists) BlenderKeyframeGold else Color.Gray,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (currentKfExists) "KEYFRAME" else "+ KEYFRAME",
                        color = if (currentKfExists) BlenderKeyframeGold else Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (currentKfExists) {
                IconButton(onClick = onDeleteKeyframe, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete KF", tint = Color(0xFFFF6B6B), modifier = Modifier.size(13.dp))
                }
            }
        }

        VerticalDivider(color = BlenderBorder, modifier = Modifier.height(14.dp))

        // Viewport Fit & Zoom Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // FIT Button
            Surface(
                color = if (timelineFitToScreen) BlenderPlayheadBlue.copy(alpha = 0.25f) else BlenderSurface,
                shape = RoundedCornerShape(3.dp),
                border = BorderStroke(0.5.dp, if (timelineFitToScreen) BlenderPlayheadBlue else BlenderBorder),
                modifier = Modifier.clickable { onToggleFit() }
            ) {
                Text(
                    text = "FIT",
                    color = if (timelineFitToScreen) BlenderPlayheadBlue else Color.Gray,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp)
                )
            }

            // Zoom In / Out
            IconButton(onClick = onZoomOut, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.Gray, modifier = Modifier.size(12.dp))
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.Gray, modifier = Modifier.size(12.dp))
            }
        }

        VerticalDivider(color = BlenderBorder, modifier = Modifier.height(14.dp))

        // Loop Mode & FPS & Frames Dropdowns
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Loop Mode
            IconButton(onClick = onCycleLoop, modifier = Modifier.size(22.dp)) {
                Icon(
                    imageVector = when (project.loopMode) {
                        LoopMode.REPEAT -> Icons.Default.Repeat
                        LoopMode.PING_PONG -> Icons.Default.SwapHoriz
                        LoopMode.ONCE -> Icons.Default.East
                    },
                    contentDescription = "Loop Mode",
                    tint = Color.LightGray,
                    modifier = Modifier.size(13.dp)
                )
            }

            // FPS Dropdown
            var showFps by remember { mutableStateOf(false) }
            Box {
                Surface(
                    color = BlenderSurface,
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(0.5.dp, BlenderBorder),
                    modifier = Modifier.clickable { showFps = true }
                ) {
                    Text(
                        text = "${project.fps} FPS",
                        color = Color.LightGray,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                DropdownMenu(
                    expanded = showFps,
                    onDismissRequest = { showFps = false },
                    modifier = Modifier.background(BlenderDarkBg)
                ) {
                    listOf(12, 24, 30, 60).forEach { fps ->
                        DropdownMenuItem(
                            text = { Text("$fps FPS", color = Color.White, fontSize = 10.sp) },
                            onClick = {
                                onSetFps(fps)
                                showFps = false
                            }
                        )
                    }
                }
            }

            // Total Frames Dropdown
            var showTotalFrames by remember { mutableStateOf(false) }
            Box {
                Surface(
                    color = BlenderSurface,
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(0.5.dp, BlenderBorder),
                    modifier = Modifier.clickable { showTotalFrames = true }
                ) {
                    Text(
                        text = "${project.totalFrames} F",
                        color = BlenderKeyframeGold,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                DropdownMenu(
                    expanded = showTotalFrames,
                    onDismissRequest = { showTotalFrames = false },
                    modifier = Modifier.background(BlenderDarkBg)
                ) {
                    listOf(12, 24, 30, 48, 60, 90, 120, 180, 240).forEach { count ->
                        DropdownMenuItem(
                            text = { Text("$count Frames", color = Color.White, fontSize = 10.sp) },
                            onClick = {
                                onSetTotalFrames(count)
                                showTotalFrames = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. BLENDER TIMELINE TRACKS (LEFT LAYER NAMES + RIGHT SCROLLABLE CANVAS)
// =============================================================================
@Composable
private fun BlenderTimelineTracks(
    project: KorProject,
    currentFrame: Float,
    selectedLayerId: String?,
    selectedKeyframe: Pair<String, Int>?,
    timelineZoom: Float,
    timelineFitToScreen: Boolean,
    onScrub: (Float) -> Unit,
    onSelectLayer: (String) -> Unit,
    onSelectKeyframe: (String, Int) -> Unit,
    onMoveKeyframe: (String, Int, Int) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val rulerHeightDp = 22.dp
    val trackHeightDp = 24.dp
    val leftHeaderWidthDp = 90.dp

    val startPaddingDp = 16.dp
    val endPaddingDp = 20.dp

    Row(modifier = Modifier.fillMaxSize()) {
        // LEFT COLUMN: Layer Names List
        Column(
            modifier = Modifier
                .width(leftHeaderWidthDp)
                .fillMaxHeight()
                .background(BlenderPanelBg)
                .border(width = 0.5.dp, color = BlenderBorder)
        ) {
            // Header for Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rulerHeightDp)
                    .background(BlenderDarkBg)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Summary",
                    color = Color.Gray,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = BlenderBorder, thickness = 0.8.dp)

            // Layer Row Items
            Column(modifier = Modifier.weight(1f)) {
                project.layers.forEach { layer ->
                    val isSelected = layer.id == selectedLayerId
                    val shapeColor = getShapeColor(layer.shapeKind)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeightDp)
                            .background(
                                if (isSelected) BlenderSurfaceHighlight else BlenderPanelBg
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(shapeColor)
                            )
                            Text(
                                text = layer.name,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Visibility & Lock Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Icon(
                                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (layer.isVisible) Color.Gray else Color(0xFFFF5252),
                                modifier = Modifier
                                    .size(10.dp)
                                    .clickable { onToggleVisibility(layer.id) }
                            )
                            Icon(
                                imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (layer.isLocked) BlenderKeyframeGold else Color.Transparent,
                                modifier = Modifier
                                    .size(10.dp)
                                    .clickable { onToggleLock(layer.id) }
                            )
                        }
                    }

                    HorizontalDivider(color = BlenderBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }

        // RIGHT COLUMN: Ruler + Timeline Tracks Canvas
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
                .background(BlenderDarkBg)
        ) {
            val totalFrames = project.totalFrames
            val availableWidthDp = maxWidth

            val frameSpacingDp: Dp
            val canvasWidthDp: Dp

            if (timelineFitToScreen) {
                val usableWidthDp = (availableWidthDp - startPaddingDp - endPaddingDp).coerceAtLeast(60.dp)
                frameSpacingDp = (usableWidthDp / (totalFrames - 1).coerceAtLeast(1)).coerceIn(3.dp, 60.dp)
                canvasWidthDp = availableWidthDp
            } else {
                frameSpacingDp = (18f * timelineZoom).coerceIn(6f, 60f).dp
                val totalTrackWidthDp = startPaddingDp + (frameSpacingDp * (totalFrames - 1).coerceAtLeast(1)) + endPaddingDp
                canvasWidthDp = maxOf(totalTrackWidthDp, availableWidthDp)
            }

            // Auto scroll playhead when zooming or playing
            LaunchedEffect(currentFrame, timelineFitToScreen) {
                if (!timelineFitToScreen) {
                    val frameSpacingPx = with(density) { frameSpacingDp.toPx() }
                    val startPaddingPx = with(density) { startPaddingDp.toPx() }
                    val playheadPx = startPaddingPx + currentFrame * frameSpacingPx
                    val viewportWidthPx = with(density) { availableWidthDp.toPx() }

                    val targetScroll = (playheadPx - viewportWidthPx / 2f).toInt()
                    if (targetScroll in 0..scrollState.maxValue) {
                        scrollState.scrollTo(targetScroll)
                    }
                }
            }

            BlenderCanvas(
                project = project,
                currentFrame = currentFrame,
                selectedLayerId = selectedLayerId,
                selectedKeyframe = selectedKeyframe,
                canvasWidthDp = canvasWidthDp,
                startPaddingDp = startPaddingDp,
                frameSpacingDp = frameSpacingDp,
                trackHeightDp = trackHeightDp,
                rulerHeightDp = rulerHeightDp,
                onScrub = onScrub,
                onSelectLayer = onSelectLayer,
                onSelectKeyframe = onSelectKeyframe,
                onMoveKeyframe = onMoveKeyframe
            )
        }
    }
}

// =============================================================================
// 3. UNIFIED BLENDER CANVAS (CLEAN RULER + TRACKS + DIAMOND KEYFRAMES + PLAYHEAD)
// =============================================================================
@Composable
private fun BlenderCanvas(
    project: KorProject,
    currentFrame: Float,
    selectedLayerId: String?,
    selectedKeyframe: Pair<String, Int>?,
    canvasWidthDp: Dp,
    startPaddingDp: Dp,
    frameSpacingDp: Dp,
    trackHeightDp: Dp,
    rulerHeightDp: Dp,
    onScrub: (Float) -> Unit,
    onSelectLayer: (String) -> Unit,
    onSelectKeyframe: (String, Int) -> Unit,
    onMoveKeyframe: (String, Int, Int) -> Unit
) {
    val totalFrames = project.totalFrames
    val density = LocalDensity.current

    val trackHeightPx = with(density) { trackHeightDp.toPx() }
    val rulerHeightPx = with(density) { rulerHeightDp.toPx() }
    val startPaddingPx = with(density) { startPaddingDp.toPx() }
    val frameSpacingPx = with(density) { frameSpacingDp.toPx() }

    var draggingKf by remember { mutableStateOf<Triple<String, Int, Float>?>(null) } // layerId, origFrame, currentDragX

    fun frameToX(f: Float): Float = startPaddingPx + f * frameSpacingPx
    fun xToFrame(x: Float): Float {
        val rawF = (x - startPaddingPx) / frameSpacingPx
        return rawF.coerceIn(0f, (totalFrames - 1).toFloat())
    }

    // Pre-calculate step interval for ruler numbers
    val step = when {
        frameSpacingPx > 35f -> 1
        frameSpacingPx > 18f -> 5
        frameSpacingPx > 8f -> 10
        else -> 20
    }

    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(160, 200, 200, 200)
            textSize = with(density) { 8.5.sp.toPx() }
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Canvas(
        modifier = Modifier
            .width(canvasWidthDp)
            .fillMaxHeight()
            // Scrubbing & Keyframe Tap / Drag gestures
            .pointerInput(totalFrames, frameSpacingPx, startPaddingPx, project.layers) {
                detectTapGestures { tapOffset ->
                    val tapX = tapOffset.x
                    val tapY = tapOffset.y

                    if (tapY <= rulerHeightPx) {
                        // Tapped on Ruler: Scrub playhead directly
                        onScrub(xToFrame(tapX).roundToInt().toFloat())
                    } else {
                        // Tapped on Tracks: Check if tapped a keyframe
                        val trackIndex = ((tapY - rulerHeightPx) / trackHeightPx).toInt()
                        if (trackIndex in project.layers.indices) {
                            val layer = project.layers[trackIndex]
                            onSelectLayer(layer.id)

                            val clickedKf = layer.keyframes.find { kf ->
                                val kfX = frameToX(kf.frame.toFloat())
                                abs(tapX - kfX) <= (frameSpacingPx / 2f).coerceAtLeast(10f)
                            }

                            if (clickedKf != null) {
                                onSelectKeyframe(layer.id, clickedKf.frame)
                                onScrub(clickedKf.frame.toFloat())
                            } else {
                                onScrub(xToFrame(tapX).roundToInt().toFloat())
                            }
                        } else {
                            onScrub(xToFrame(tapX).roundToInt().toFloat())
                        }
                    }
                }
            }
            .pointerInput(totalFrames, frameSpacingPx, startPaddingPx, project.layers) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val startX = startOffset.x
                        val startY = startOffset.y

                        if (startY > rulerHeightPx) {
                            val trackIndex = ((startY - rulerHeightPx) / trackHeightPx).toInt()
                            if (trackIndex in project.layers.indices) {
                                val layer = project.layers[trackIndex]
                                val hitKf = layer.keyframes.find { kf ->
                                    val kfX = frameToX(kf.frame.toFloat())
                                    abs(startX - kfX) <= (frameSpacingPx / 2f).coerceAtLeast(10f)
                                }
                                if (hitKf != null) {
                                    draggingKf = Triple(layer.id, hitKf.frame, startX)
                                    onSelectKeyframe(layer.id, hitKf.frame)
                                    return@detectDragGestures
                                }
                            }
                        }
                        // Default to scrubbing playhead
                        onScrub(xToFrame(startX))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentX = change.position.x
                        val activeKf = draggingKf
                        if (activeKf != null) {
                            draggingKf = activeKf.copy(third = currentX)
                            val draggedFrame = xToFrame(currentX).roundToInt()
                            onScrub(draggedFrame.toFloat())
                        } else {
                            onScrub(xToFrame(currentX))
                        }
                    },
                    onDragEnd = {
                        val activeKf = draggingKf
                        if (activeKf != null) {
                            val newFrame = xToFrame(activeKf.third).roundToInt()
                            if (newFrame != activeKf.second) {
                                onMoveKeyframe(activeKf.first, activeKf.second, newFrame)
                            }
                            draggingKf = null
                        }
                    },
                    onDragCancel = {
                        draggingKf = null
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Background Grid & Tracks alternating stripes
        drawRect(color = BlenderDarkBg, size = size)

        project.layers.forEachIndexed { index, layer ->
            val trackTop = rulerHeightPx + index * trackHeightPx
            val isLayerSelected = layer.id == selectedLayerId

            // Track row background
            val trackColor = if (isLayerSelected) {
                BlenderSurfaceHighlight.copy(alpha = 0.45f)
            } else if (index % 2 == 0) {
                BlenderPanelBg.copy(alpha = 0.35f)
            } else {
                BlenderDarkBg
            }

            drawRect(
                color = trackColor,
                topLeft = Offset(0f, trackTop),
                size = Size(width, trackHeightPx)
            )

            // Track dividing horizontal line
            drawLine(
                color = BlenderBorder.copy(alpha = 0.3f),
                start = Offset(0f, trackTop + trackHeightPx),
                end = Offset(width, trackTop + trackHeightPx),
                strokeWidth = 1f
            )
        }

        // 2. Vertical Frame Grid Lines (Subtle Blender tick lines)
        for (f in 0 until totalFrames) {
            val lineX = frameToX(f.toFloat())
            val isMajor = f % 5 == 0

            val gridColor = if (isMajor) {
                BlenderBorder.copy(alpha = 0.35f)
            } else {
                BlenderBorder.copy(alpha = 0.12f)
            }

            // Line down entire track
            drawLine(
                color = gridColor,
                start = Offset(lineX, rulerHeightPx),
                end = Offset(lineX, height),
                strokeWidth = if (isMajor) 1f else 0.5f
            )
        }

        // 3. Draw Keyframes on Tracks (Crisp Blender Gold Diamonds ◆)
        project.layers.forEachIndexed { index, layer ->
            val trackCenterY = rulerHeightPx + index * trackHeightPx + trackHeightPx / 2f
            val isLayerSelected = layer.id == selectedLayerId

            // Draw span connection line between keyframes
            val sortedKfs = layer.keyframes.sortedBy { it.frame }
            if (sortedKfs.size >= 2) {
                val firstX = frameToX(sortedKfs.first().frame.toFloat())
                val lastX = frameToX(sortedKfs.last().frame.toFloat())
                drawLine(
                    color = BlenderKeyframeGold.copy(alpha = if (isLayerSelected) 0.5f else 0.25f),
                    start = Offset(firstX, trackCenterY),
                    end = Offset(lastX, trackCenterY),
                    strokeWidth = 2f
                )
            }

            // Draw Diamonds
            sortedKfs.forEach { kf ->
                val isBeingDragged = draggingKf?.first == layer.id && draggingKf?.second == kf.frame
                val kfX = if (isBeingDragged) draggingKf!!.third else frameToX(kf.frame.toFloat())
                val isKfSelected = selectedKeyframe?.first == layer.id && selectedKeyframe?.second == kf.frame

                val diamondRadius = if (isKfSelected) 5.5f else 4f
                val diamondPath = Path().apply {
                    moveTo(kfX, trackCenterY - diamondRadius)
                    lineTo(kfX + diamondRadius, trackCenterY)
                    lineTo(kfX, trackCenterY + diamondRadius)
                    lineTo(kfX - diamondRadius, trackCenterY)
                    close()
                }

                // Fill Diamond
                drawPath(
                    path = diamondPath,
                    color = if (isKfSelected) BlenderKeyframeSelected else BlenderKeyframeGold
                )

                // Diamond Border
                drawPath(
                    path = diamondPath,
                    color = if (isKfSelected) BlenderPlayheadBlue else Color(0xFF6B4500),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // 4. Ruler Header Area (Blender Timeline Ruler)
        drawRect(
            color = BlenderPanelBg,
            topLeft = Offset(0f, 0f),
            size = Size(width, rulerHeightPx)
        )

        drawLine(
            color = BlenderBorder,
            start = Offset(0f, rulerHeightPx),
            end = Offset(width, rulerHeightPx),
            strokeWidth = 1.5f
        )

        // Ruler Ticks and Numbers
        for (f in 0 until totalFrames) {
            val tickX = frameToX(f.toFloat())
            val isMajor = f % step == 0
            val isSubMajor = f % 5 == 0

            val tickHeight = when {
                isMajor -> rulerHeightPx * 0.5f
                isSubMajor -> rulerHeightPx * 0.3f
                else -> rulerHeightPx * 0.18f
            }

            drawLine(
                color = if (isMajor) Color.LightGray else BlenderBorder,
                start = Offset(tickX, rulerHeightPx - tickHeight),
                end = Offset(tickX, rulerHeightPx),
                strokeWidth = if (isMajor) 1f else 0.5f
            )

            if (isMajor) {
                drawContext.canvas.nativeCanvas.drawText(
                    f.toString(),
                    tickX,
                    rulerHeightPx - tickHeight - 2f,
                    textPaint
                )
            }
        }

        // 5. Blender Signature Playhead Cursor (Blue Vertical Line with Triangular Cap)
        val playheadX = frameToX(currentFrame)

        // Playhead Vertical Line through entire canvas
        drawLine(
            color = BlenderPlayheadBlue,
            start = Offset(playheadX, rulerHeightPx),
            end = Offset(playheadX, height),
            strokeWidth = 1.8f
        )

        // Playhead Head on the Ruler
        val headPath = Path().apply {
            moveTo(playheadX - 5.5f, 0f)
            lineTo(playheadX + 5.5f, 0f)
            lineTo(playheadX + 5.5f, rulerHeightPx - 5f)
            lineTo(playheadX, rulerHeightPx)
            lineTo(playheadX - 5.5f, rulerHeightPx - 5f)
            close()
        }

        drawPath(
            path = headPath,
            color = BlenderPlayheadHead
        )

        drawPath(
            path = headPath,
            color = Color.White,
            style = Stroke(width = 1f)
        )
    }
}

// Utility function to get tint color for layer shapes
private fun getShapeColor(kind: ShapeKind): Color {
    return when (kind) {
        ShapeKind.RECTANGLE -> Color(0xFF4D96FF)
        ShapeKind.ROUNDED_RECT -> Color(0xFF38BDF8)
        ShapeKind.CIRCLE -> Color(0xFFFF6B6B)
        ShapeKind.TRIANGLE -> Color(0xFFFFD93D)
        ShapeKind.STAR -> Color(0xFF6BCB77)
        ShapeKind.DIAMOND -> Color(0xFFFFB84D)
        ShapeKind.ARROW -> Color(0xFF34D399)
        ShapeKind.SWORD -> Color(0xFFF43F5E)
        ShapeKind.SHIELD -> Color(0xFF818CF8)
        ShapeKind.SLIME -> Color(0xFF10B981)
        ShapeKind.COIN -> Color(0xFFFBBF24)
        ShapeKind.SLASH_FX -> Color(0xFFA855F7)
    }
}
