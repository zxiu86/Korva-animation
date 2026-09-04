package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.viewmodel.KorvaViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Blender-inspired signature dark studio colors
private val BlenderDarkBg = Color(0xFF161719)
private val BlenderPanelBg = Color(0xFF202125)
private val BlenderSurface = Color(0xFF2B2C32)
private val BlenderSurfaceHighlight = Color(0xFF383A43)
private val BlenderBorder = Color(0xFF35373F)
private val BlenderPlayheadBlue = Color(0xFF3B82F6)
private val BlenderPlayheadBeam = Color(0xFF60A5FA)
private val BlenderKeyframeGold = Color(0xFFFBBF24)
private val BlenderKeyframeSelected = Color(0xFFFFFFFF)
private val BlenderPlayheadHead = Color(0xFF60A5FA)

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
    val coroutineScope = rememberCoroutineScope()

    val panelHeight by animateDpAsState(
        targetValue = if (isCollapsed) 36.dp else timelineHeightDp.coerceAtLeast(140f).dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TimelinePanelHeight"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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

                // 1. Drag Handle to adjust timeline height
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

                // 2. Transport Controls & Options Toolbar
                BlenderTransportToolbar(
                    project = project,
                    currentFrame = currentFrame,
                    isPlaying = isPlaying,
                    isCollapsed = isCollapsed,
                    currentKfExists = currentKfExists,
                    timelineFitToScreen = timelineFitToScreen,
                    timelineZoom = timelineZoom,
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

                // 3. Multi-Track Timeline Canvas with Smooth Scrolling & Free Playhead
                if (!isCollapsed) {
                    BlenderTimelineTracks(
                        project = project,
                        currentFrame = currentFrame,
                        isPlaying = isPlaying,
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
    timelineZoom: Float,
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
    val secondsElapsed = if (project.fps > 0) currentFrame / project.fps.toFloat() else 0f
    val timecodeStr = String.format(java.util.Locale.US, "%02d:%04.1fs", (secondsElapsed / 60).toInt(), secondsElapsed % 60)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
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

        // Current Frame Display Box
        Surface(
            color = BlenderSurface,
            shape = RoundedCornerShape(3.dp),
            border = BorderStroke(1.dp, BlenderPlayheadBlue.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${currentFrame.toInt()}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "($timecodeStr)",
                    color = BlenderPlayheadBlue.copy(alpha = 0.9f),
                    fontSize = 9.sp,
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

        // Transport Controls (|<  ⏮  <  PLAY/PAUSE  >  ⏭  >|)
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
                color = if (isPlaying) Color(0xFFDC2626) else BlenderPlayheadBlue,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (isPlaying) Color(0xFFEF4444) else Color(0xFF60A5FA)),
                modifier = Modifier
                    .clickable { onTogglePlay() }
                    .testTag("timeline_play_pause_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
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
            val isFitActive = timelineFitToScreen && project.totalFrames <= 30
            Surface(
                color = if (isFitActive) BlenderPlayheadBlue.copy(alpha = 0.25f) else BlenderSurface,
                shape = RoundedCornerShape(3.dp),
                border = BorderStroke(0.5.dp, if (isFitActive) BlenderPlayheadBlue else BlenderBorder),
                modifier = Modifier.clickable { onToggleFit() }
            ) {
                Text(
                    text = if (project.totalFrames > 30) "SCROLL" else "FIT",
                    color = if (isFitActive) BlenderPlayheadBlue else Color(0xFF38BDF8),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp)
                )
            }

            // Zoom In / Out
            IconButton(onClick = onZoomOut, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.LightGray, modifier = Modifier.size(13.dp))
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.LightGray, modifier = Modifier.size(13.dp))
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
                    listOf(12, 24, 30, 48, 60).forEach { fps ->
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
    isPlaying: Boolean,
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
    val coroutineScope = rememberCoroutineScope()

    val rulerHeightDp = 24.dp
    val trackHeightDp = 26.dp
    val leftHeaderWidthDp = 95.dp

    val startPaddingDp = 20.dp
    val endPaddingDp = 36.dp

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Summary",
                        tint = BlenderKeyframeGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Summary",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (layer.type == LayerType.PARTICLE_FX) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(10.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(shapeColor)
                                )
                            }
                            Text(
                                text = layer.name,
                                color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Visibility & Lock Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (layer.isVisible) Color.Gray else Color(0xFFFF5252),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onToggleVisibility(layer.id) }
                            )
                            Icon(
                                imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (layer.isLocked) BlenderKeyframeGold else Color(0xFF4A4B54),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onToggleLock(layer.id) }
                            )
                        }
                    }

                    HorizontalDivider(color = BlenderBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }

        // RIGHT COLUMN: Scrollable Timeline Tracks Canvas & Horizontal Navigation
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(BlenderDarkBg)
        ) {
            val totalFrames = project.totalFrames
            val availableWidthDp = maxWidth

            // Calculate spacing: If totalFrames > 30, ALWAYS provide wide spacing so scrolling works smoothly!
            val isFitMode = timelineFitToScreen && totalFrames <= 30
            val frameSpacingDp: Dp
            val canvasWidthDp: Dp

            if (isFitMode) {
                val usableWidthDp = (availableWidthDp - startPaddingDp - endPaddingDp).coerceAtLeast(60.dp)
                frameSpacingDp = (usableWidthDp / (totalFrames - 1).coerceAtLeast(1)).coerceIn(8.dp, 60.dp)
                canvasWidthDp = availableWidthDp
            } else {
                // When frames > 30 or user zoomed/unfitted, ensure at least 22dp per frame
                frameSpacingDp = (24f * timelineZoom).coerceIn(12f, 90f).dp
                val totalTrackWidthDp = startPaddingDp + (frameSpacingDp * (totalFrames - 1).coerceAtLeast(1)) + endPaddingDp + 60.dp
                canvasWidthDp = maxOf(totalTrackWidthDp, availableWidthDp)
            }

            // Smooth playhead auto-tracking during playback only (does not fight user manual scrolling!)
            LaunchedEffect(isPlaying, currentFrame) {
                if (isPlaying && !isFitMode && !scrollState.isScrollInProgress) {
                    val frameSpacingPx = with(density) { frameSpacingDp.toPx() }
                    val startPaddingPx = with(density) { startPaddingDp.toPx() }
                    val playheadPx = startPaddingPx + currentFrame * frameSpacingPx
                    val viewportWidthPx = with(density) { availableWidthDp.toPx() }

                    // Only adjust scroll if needle moves out of the middle 60% of viewport
                    val currentScroll = scrollState.value
                    val relativeX = playheadPx - currentScroll
                    if (relativeX < viewportWidthPx * 0.15f || relativeX > viewportWidthPx * 0.85f) {
                        val targetScroll = (playheadPx - viewportWidthPx / 2f).toInt().coerceIn(0, scrollState.maxValue)
                        scrollState.scrollTo(targetScroll)
                    }
                }
            }

            // Scrollable Timeline Canvas container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
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
                    onMoveKeyframe = onMoveKeyframe,
                    onAutoScrollBy = { deltaPx ->
                        coroutineScope.launch {
                            scrollState.scrollBy(deltaPx)
                        }
                    }
                )
            }

            // Floating Navigation Overlay for Horizontal Scrolling (> 30 frames)
            if (scrollState.maxValue > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Center playhead in view button
                    Surface(
                        color = BlenderSurface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, BlenderPlayheadBlue.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    val frameSpacingPx = with(density) { frameSpacingDp.toPx() }
                                    val startPaddingPx = with(density) { startPaddingDp.toPx() }
                                    val playheadPx = startPaddingPx + currentFrame * frameSpacingPx
                                    val viewportWidthPx = with(density) { availableWidthDp.toPx() }
                                    val targetScroll = (playheadPx - viewportWidthPx / 2f).toInt().coerceIn(0, scrollState.maxValue)
                                    scrollState.animateScrollTo(targetScroll)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.FilterCenterFocus, contentDescription = "Focus", tint = BlenderPlayheadBlue, modifier = Modifier.size(11.dp))
                            Text("Focus", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Left Scroll Button
                    Surface(
                        color = BlenderSurface.copy(alpha = 0.85f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, BlenderBorder),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                coroutineScope.launch {
                                    scrollState.animateScrollBy(-240f)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Scroll Left", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Right Scroll Button
                    Surface(
                        color = BlenderSurface.copy(alpha = 0.85f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, BlenderBorder),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                coroutineScope.launch {
                                    scrollState.animateScrollBy(240f)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Scroll Right", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
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
    onMoveKeyframe: (String, Int, Int) -> Unit,
    onAutoScrollBy: (Float) -> Unit
) {
    val totalFrames = project.totalFrames
    val density = LocalDensity.current

    val trackHeightPx = with(density) { trackHeightDp.toPx() }
    val rulerHeightPx = with(density) { rulerHeightDp.toPx() }
    val startPaddingPx = with(density) { startPaddingDp.toPx() }
    val frameSpacingPx = with(density) { frameSpacingDp.toPx() }

    var draggingKf by remember { mutableStateOf<Triple<String, Int, Float>?>(null) } // layerId, origFrame, currentDragX
    var isDraggingPlayhead by remember { mutableStateOf(false) }

    fun frameToX(f: Float): Float = startPaddingPx + f * frameSpacingPx
    fun xToFrame(x: Float): Float {
        val rawF = (x - startPaddingPx) / frameSpacingPx
        return rawF.coerceIn(0f, (totalFrames - 1).toFloat())
    }

    // Step interval for ruler frame numbers
    val step = when {
        frameSpacingPx > 35f -> 1
        frameSpacingPx > 18f -> 5
        frameSpacingPx > 8f -> 10
        else -> 20
    }

    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(220, 210, 220, 235)
            textSize = with(density) { 9.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val badgePaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 8.5.sp.toPx() }
            isAntiAlias = true
            isFakeBoldText = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val kfHitRadiusPx = maxOf(frameSpacingPx * 0.7f, with(density) { 16.dp.toPx() })

    Canvas(
        modifier = Modifier
            .width(canvasWidthDp)
            .fillMaxHeight()
            .pointerInput(totalFrames, frameSpacingPx, startPaddingPx, project.layers) {
                detectTapGestures { tapOffset ->
                    val tapX = tapOffset.x
                    val tapY = tapOffset.y

                    if (tapY <= rulerHeightPx) {
                        // Tapped on Ruler: Scrub playhead directly to frame
                        onScrub(xToFrame(tapX).roundToInt().toFloat())
                    } else {
                        // Tapped on Tracks: Check if tapped a keyframe
                        val trackIndex = ((tapY - rulerHeightPx) / trackHeightPx).toInt()
                        if (trackIndex in project.layers.indices) {
                            val layer = project.layers[trackIndex]
                            onSelectLayer(layer.id)

                            val clickedKf = layer.keyframes.find { kf ->
                                val kfX = frameToX(kf.frame.toFloat())
                                abs(tapX - kfX) <= kfHitRadiusPx
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
                                    abs(startX - kfX) <= kfHitRadiusPx
                                }
                                if (hitKf != null) {
                                    draggingKf = Triple(layer.id, hitKf.frame, startX)
                                    onSelectKeyframe(layer.id, hitKf.frame)
                                    return@detectDragGestures
                                }
                            }
                        }
                        // Default to smooth scrubbing playhead
                        isDraggingPlayhead = true
                        onScrub(xToFrame(startX))
                    },
                    onDrag = { change, dragAmount ->
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
                        isDraggingPlayhead = false
                    },
                    onDragCancel = {
                        draggingKf = null
                        isDraggingPlayhead = false
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Background Grid & Tracks alternating stripes
        drawRect(color = BlenderDarkBg, size = size)

        val trackCount = maxOf(3, project.layers.size)
        for (index in 0 until trackCount) {
            val trackTop = rulerHeightPx + index * trackHeightPx
            val layer = project.layers.getOrNull(index)
            val isLayerSelected = layer != null && layer.id == selectedLayerId

            val trackColor = if (isLayerSelected) {
                BlenderSurfaceHighlight.copy(alpha = 0.55f)
            } else if (index % 2 == 0) {
                BlenderPanelBg.copy(alpha = 0.45f)
            } else {
                BlenderDarkBg
            }

            drawRect(
                color = trackColor,
                topLeft = Offset(0f, trackTop),
                size = Size(width, trackHeightPx)
            )

            // Track dividing line
            drawLine(
                color = BlenderBorder.copy(alpha = 0.35f),
                start = Offset(0f, trackTop + trackHeightPx),
                end = Offset(width, trackTop + trackHeightPx),
                strokeWidth = 1f
            )
        }

        // 2. Vertical Frame Grid Lines
        for (f in 0 until totalFrames) {
            val lineX = frameToX(f.toFloat())
            val isMajor = f % 5 == 0

            val gridColor = if (isMajor) {
                BlenderBorder.copy(alpha = 0.35f)
            } else {
                BlenderBorder.copy(alpha = 0.12f)
            }

            drawLine(
                color = gridColor,
                start = Offset(lineX, rulerHeightPx),
                end = Offset(lineX, height),
                strokeWidth = if (isMajor) 1f else 0.5f
            )
        }

        // 3. Playhead Luminous Column (Glow column under cursor)
        val playheadX = frameToX(currentFrame)
        val colWidth = maxOf(frameSpacingPx, 12f)

        // Soft ambient blue glow on active column
        drawRect(
            color = BlenderPlayheadBlue.copy(alpha = 0.10f),
            topLeft = Offset(playheadX - colWidth / 2f, rulerHeightPx),
            size = Size(colWidth, height - rulerHeightPx)
        )

        // 4. Summary Master Keyframes on Header Area
        val allKeyframeFrames = project.layers.flatMap { it.keyframes.map { kf -> kf.frame } }.toSet()
        allKeyframeFrames.forEach { kfFrame ->
            val kfX = frameToX(kfFrame.toFloat())
            val masterDiamondRadius = 3.5f
            val summaryPath = Path().apply {
                moveTo(kfX, rulerHeightPx - 4f - masterDiamondRadius)
                lineTo(kfX + masterDiamondRadius, rulerHeightPx - 4f)
                lineTo(kfX, rulerHeightPx - 4f + masterDiamondRadius)
                lineTo(kfX - masterDiamondRadius, rulerHeightPx - 4f)
                close()
            }
            drawPath(
                path = summaryPath,
                color = BlenderKeyframeGold.copy(alpha = 0.70f)
            )
        }

        // 5. Draw Keyframes on Tracks (Blender Gold Diamonds ◆ with Span Connectors)
        project.layers.forEachIndexed { index, layer ->
            val trackCenterY = rulerHeightPx + index * trackHeightPx + trackHeightPx / 2f
            val isLayerSelected = layer.id == selectedLayerId

            // Span connector line between keyframes
            val sortedKfs = layer.keyframes.sortedBy { it.frame }
            if (sortedKfs.size >= 2) {
                val firstX = frameToX(sortedKfs.first().frame.toFloat())
                val lastX = frameToX(sortedKfs.last().frame.toFloat())

                drawLine(
                    color = BlenderKeyframeGold.copy(alpha = if (isLayerSelected) 0.35f else 0.18f),
                    start = Offset(firstX, trackCenterY),
                    end = Offset(lastX, trackCenterY),
                    strokeWidth = 4f
                )
                drawLine(
                    color = BlenderKeyframeGold.copy(alpha = if (isLayerSelected) 0.85f else 0.45f),
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

                val diamondRadius = if (isBeingDragged) 6.5f else if (isKfSelected) 5.5f else 4.2f

                if (isKfSelected || isBeingDragged) {
                    drawCircle(
                        color = BlenderPlayheadBlue.copy(alpha = 0.35f),
                        radius = diamondRadius + 4f,
                        center = Offset(kfX, trackCenterY)
                    )
                }

                val diamondPath = Path().apply {
                    moveTo(kfX, trackCenterY - diamondRadius)
                    lineTo(kfX + diamondRadius, trackCenterY)
                    lineTo(kfX, trackCenterY + diamondRadius)
                    lineTo(kfX - diamondRadius, trackCenterY)
                    close()
                }

                drawPath(
                    path = diamondPath,
                    color = if (isBeingDragged) Color(0xFFFFD700) else if (isKfSelected) BlenderKeyframeSelected else BlenderKeyframeGold
                )

                drawPath(
                    path = diamondPath,
                    color = if (isBeingDragged) Color.White else if (isKfSelected) BlenderPlayheadBlue else Color(0xFF5A3A00),
                    style = Stroke(width = 1.3f)
                )

                if (isBeingDragged) {
                    val draggedFrameNumber = xToFrame(kfX).roundToInt()
                    drawContext.canvas.nativeCanvas.drawText(
                        draggedFrameNumber.toString(),
                        kfX,
                        trackCenterY - diamondRadius - 6f,
                        badgePaint
                    )
                }
            }
        }

        // 6. Ruler Header Area
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

        // 7. Signature Blender Playhead Needle & Luminous Column
        // Vertical Needle Line through entire timeline
        drawLine(
            color = BlenderPlayheadBeam.copy(alpha = 0.35f),
            start = Offset(playheadX, rulerHeightPx),
            end = Offset(playheadX, height),
            strokeWidth = 4f
        )
        drawLine(
            color = BlenderPlayheadBlue,
            start = Offset(playheadX, rulerHeightPx),
            end = Offset(playheadX, height),
            strokeWidth = 2f
        )

        // Playhead Cap on the Ruler
        val headPath = Path().apply {
            moveTo(playheadX - 7f, 0f)
            lineTo(playheadX + 7f, 0f)
            lineTo(playheadX + 7f, rulerHeightPx - 6f)
            lineTo(playheadX, rulerHeightPx)
            lineTo(playheadX - 7f, rulerHeightPx - 6f)
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

        // Floating Frame Number on Playhead Head when dragging or active
        drawContext.canvas.nativeCanvas.drawText(
            currentFrame.toInt().toString(),
            playheadX,
            rulerHeightPx - 7f,
            badgePaint
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
