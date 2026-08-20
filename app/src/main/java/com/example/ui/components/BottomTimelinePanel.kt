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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
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
    val timelineSnap by viewModel.timelineSnapToKeyframes.collectAsState()
    val selectedKeyframe by viewModel.selectedKeyframe.collectAsState()
    val timelineViewMode by viewModel.timelineViewMode.collectAsState()
    val showSubTracks by viewModel.timelineShowSubTracks.collectAsState()
    val workAreaEnabled by viewModel.workAreaEnabled.collectAsState()
    val workAreaStart by viewModel.workAreaStart.collectAsState()
    val workAreaEnd by viewModel.workAreaEnd.collectAsState()
    val timeFormat by viewModel.timeDisplayFormat.collectAsState()
    val onionSkinEnabled by viewModel.onionSkinEnabled.collectAsState()

    val selectedLayer = project.layers.find { it.id == selectedLayerId }
    val currentKfExists = selectedLayer?.keyframes?.any { it.frame == currentFrame.toInt() } == true

    val density = LocalDensity.current

    val panelHeight by animateDpAsState(
        targetValue = if (isCollapsed) 32.dp else timelineHeightDp.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TimelinePanelHeight"
    )

    Surface(
        color = StudioPanelDark,
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight)
            .border(width = 1.dp, color = StudioBorder)
            .testTag("bottom_timeline_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // =========================================================================
            // 1. DYNAMIC GRAB HANDLE & RESIZE BAR
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(StudioSurfaceVariant, StudioSurfaceDark)
                        )
                    )
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(if (isCollapsed) KorvaVioletLight else StudioBorderLight)
                    )
                    Box(
                        modifier = Modifier
                            .size(2.5.dp)
                            .clip(CircleShape)
                            .background(if (isCollapsed) KorvaVioletPrimary else StudioBorder)
                    )
                }
            }

            // =========================================================================
            // 2. MASTER RESPONSIVE TIMELINE CONTROL TOOLBAR
            // =========================================================================
            TimelineControlHeader(
                project = project,
                currentFrame = currentFrame,
                isPlaying = isPlaying,
                isCollapsed = isCollapsed,
                timelineZoom = timelineZoom,
                timelineSnap = timelineSnap,
                timelineViewMode = timelineViewMode,
                showSubTracks = showSubTracks,
                workAreaEnabled = workAreaEnabled,
                timeFormat = timeFormat,
                onionSkinEnabled = onionSkinEnabled,
                currentKfExists = currentKfExists,
                onToggleCollapse = { viewModel.toggleBottomTimeline() },
                onTogglePlay = { viewModel.togglePlay() },
                onJumpStart = { viewModel.jumpToStart() },
                onJumpEnd = { viewModel.jumpToEnd() },
                onStepBackward = { viewModel.stepBackward() },
                onStepForward = { viewModel.stepForward() },
                onJumpPrevKeyframe = { viewModel.jumpToPrevKeyframe() },
                onJumpNextKeyframe = { viewModel.jumpToNextKeyframe() },
                onAddOrUpdateKeyframe = { viewModel.addOrUpdateKeyframeOnCurrentFrame() },
                onDeleteKeyframe = { viewModel.deleteKeyframeOnCurrentFrame() },
                onZoomIn = { viewModel.zoomTimelineIn() },
                onZoomOut = { viewModel.zoomTimelineOut() },
                onToggleSnap = { viewModel.toggleTimelineSnap() },
                onToggleViewMode = { viewModel.toggleTimelineViewMode() },
                onToggleSubTracks = { viewModel.toggleSubTracks() },
                onToggleWorkArea = { viewModel.toggleWorkArea() },
                onCycleTimeFormat = { viewModel.cycleTimeDisplayFormat() },
                onToggleOnionSkin = { viewModel.toggleOnionSkin() },
                onCycleLoopMode = {
                    val nextMode = when (project.loopMode) {
                        LoopMode.REPEAT -> LoopMode.PING_PONG
                        LoopMode.PING_PONG -> LoopMode.ONCE
                        LoopMode.ONCE -> LoopMode.REPEAT
                    }
                    viewModel.setLoopMode(nextMode)
                },
                onSetFps = { fps -> viewModel.setFps(fps) },
                onSetTotalFrames = { total -> viewModel.setTotalFrames(total) }
            )

            // =========================================================================
            // 3. KEYFRAME QUICK ACTION HUD (When on keyframe / selected)
            // =========================================================================
            if (!isCollapsed && (currentKfExists || selectedKeyframe != null)) {
                val activeKfLayerId = selectedKeyframe?.first ?: selectedLayerId
                val activeKfFrame = selectedKeyframe?.second ?: currentFrame.toInt()
                val targetLayer = project.layers.find { it.id == activeKfLayerId }
                val targetKf = targetLayer?.keyframes?.find { it.frame == activeKfFrame }

                if (targetLayer != null && targetKf != null) {
                    KeyframeQuickActionHUD(
                        layerName = targetLayer.name,
                        keyframe = targetKf,
                        onNudgeLeft = { viewModel.nudgeKeyframe(targetLayer.id, targetKf.frame, -1) },
                        onNudgeRight = { viewModel.nudgeKeyframe(targetLayer.id, targetKf.frame, 1) },
                        onDuplicate = {
                            val nextF = (targetKf.frame + 5).coerceAtMost(project.totalFrames - 1)
                            viewModel.duplicateKeyframe(targetLayer.id, targetKf.frame, nextF)
                        },
                        onChangeEasing = { easing -> viewModel.setKeyframeEasing(easing) },
                        onDelete = { viewModel.deleteKeyframeOnCurrentFrame() }
                    )
                }
            }

            // =========================================================================
            // 4. MAIN CONTENT AREA (DopeSheet Multi-Tracks OR Motion Graph)
            // =========================================================================
            if (!isCollapsed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (timelineViewMode == 0) {
                        // 4A. PRO DOPESHEET MULTI-TRACK VIEW
                        DopeSheetTimelineView(
                            project = project,
                            currentFrame = currentFrame,
                            selectedLayerId = selectedLayerId,
                            selectedKeyframe = selectedKeyframe,
                            timelineZoom = timelineZoom,
                            timelineSnap = timelineSnap,
                            showSubTracks = showSubTracks,
                            workAreaEnabled = workAreaEnabled,
                            workAreaStart = workAreaStart,
                            workAreaEnd = workAreaEnd,
                            onScrub = { f -> viewModel.scrubToFrame(f) },
                            onSelectLayer = { id -> viewModel.selectLayer(id) },
                            onSelectKeyframe = { layerId, frame -> viewModel.setSelectedKeyframe(layerId, frame) },
                            onMoveKeyframe = { layerId, oldF, newF -> viewModel.moveKeyframe(layerId, oldF, newF) },
                            onToggleVisibility = { id -> viewModel.toggleLayerVisibility(id) },
                            onToggleLock = { id -> viewModel.toggleLayerLock(id) }
                        )
                    } else {
                        // 4B. MOTION GRAPH & CURVES VIEW
                        MotionGraphTimelineView(
                            project = project,
                            currentFrame = currentFrame,
                            selectedLayerId = selectedLayerId,
                            timelineZoom = timelineZoom,
                            onScrub = { f -> viewModel.scrubToFrame(f) }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPONENT 1: MASTER TIMELINE HEADER & TRANSPORT (RESPONSIVE SCROLLABLE)
// =============================================================================
@Composable
private fun TimelineControlHeader(
    project: KorProject,
    currentFrame: Float,
    isPlaying: Boolean,
    isCollapsed: Boolean,
    timelineZoom: Float,
    timelineSnap: Boolean,
    timelineViewMode: Int,
    showSubTracks: Boolean,
    workAreaEnabled: Boolean,
    timeFormat: Int,
    onionSkinEnabled: Boolean,
    currentKfExists: Boolean,
    onToggleCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onJumpPrevKeyframe: () -> Unit,
    onJumpNextKeyframe: () -> Unit,
    onAddOrUpdateKeyframe: () -> Unit,
    onDeleteKeyframe: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleSnap: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleSubTracks: () -> Unit,
    onToggleWorkArea: () -> Unit,
    onCycleTimeFormat: () -> Unit,
    onToggleOnionSkin: () -> Unit,
    onCycleLoopMode: () -> Unit,
    onSetFps: (Int) -> Unit,
    onSetTotalFrames: (Int) -> Unit
) {
    val headerScrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "PlayGlow")
    val playGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playGlow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(StudioSurfaceDark)
            .horizontalScroll(headerScrollState)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- LEFT GROUP: Collapse, Mode Switcher, Time Badge, Zoom ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            IconButton(
                onClick = onToggleCollapse,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse Timeline",
                    tint = if (isCollapsed) KorvaVioletLight else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Timeline View Mode Toggle (Dopesheet vs Graph)
            Surface(
                color = if (timelineViewMode == 0) KorvaVioletDark.copy(alpha = 0.45f) else StudioCyan.copy(alpha = 0.25f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(
                    0.8.dp,
                    if (timelineViewMode == 0) KorvaVioletPrimary.copy(alpha = 0.8f) else StudioCyan
                ),
                modifier = Modifier.clickable { onToggleViewMode() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (timelineViewMode == 0) Icons.Default.ViewTimeline else Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = if (timelineViewMode == 0) KorvaVioletLight else StudioCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (timelineViewMode == 0) "TRACKS" else "GRAPH",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // High-Precision Timecode / Frame Badge
            val formattedTime = when (timeFormat) {
                1 -> {
                    val sec = currentFrame / project.fps.toFloat()
                    String.format("%.2fs", sec)
                }
                2 -> {
                    val pct = (currentFrame / (project.totalFrames - 1).coerceAtLeast(1).toFloat() * 100).toInt()
                    "$pct%"
                }
                else -> "F ${currentFrame.toInt()} / ${project.totalFrames}"
            }

            Surface(
                color = StudioSurfaceVariant,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.8.dp, StudioBorderLight),
                modifier = Modifier.clickable { onCycleTimeFormat() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) StudioGreen else KorvaVioletLight)
                    )
                    Text(
                        text = formattedTime,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Zoom Controls
            if (!isCollapsed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    IconButton(onClick = onZoomOut, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextMuted, modifier = Modifier.size(12.dp))
                    }
                    Text(
                        text = "${(timelineZoom * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onZoomIn, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        VerticalDivider(color = StudioBorder, modifier = Modifier.height(14.dp))

        // --- CENTER GROUP: TRANSPORT CONTROLS ---
        if (!isCollapsed) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Jump to Start (|<)
                IconButton(onClick = onJumpStart, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.FirstPage, contentDescription = "Jump Start", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }

                // Jump to Previous Keyframe (⏮)
                IconButton(onClick = onJumpPrevKeyframe, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Keyframe", tint = KorvaVioletLight, modifier = Modifier.size(15.dp))
                }

                // Step 1 Frame Back (◀)
                IconButton(onClick = onStepBackward, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.ArrowLeft, contentDescription = "Prev Frame", tint = TextPrimary, modifier = Modifier.size(17.dp))
                }

                // Master Play/Pause Glowing Luminous Capsule
                val playBgColor = if (isPlaying) StudioGreen.copy(alpha = 0.25f) else KorvaVioletPrimary
                val playBorderColor = if (isPlaying) StudioGreen.copy(alpha = playGlowAlpha) else KorvaVioletLight

                Surface(
                    color = playBgColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, playBorderColor),
                    shadowElevation = if (isPlaying) 4.dp else 1.dp,
                    modifier = Modifier
                        .clickable { onTogglePlay() }
                        .testTag("timeline_play_pause_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Step 1 Frame Forward (▶)
                IconButton(onClick = onStepForward, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.ArrowRight, contentDescription = "Next Frame", tint = TextPrimary, modifier = Modifier.size(17.dp))
                }

                // Jump to Next Keyframe (⏭)
                IconButton(onClick = onJumpNextKeyframe, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Keyframe", tint = KorvaVioletLight, modifier = Modifier.size(15.dp))
                }

                // Jump to End (>|)
                IconButton(onClick = onJumpEnd, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.LastPage, contentDescription = "Jump End", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }

                // Loop Mode Switcher
                val loopIcon = when (project.loopMode) {
                    LoopMode.REPEAT -> Icons.Default.Repeat
                    LoopMode.PING_PONG -> Icons.Default.SwapHoriz
                    LoopMode.ONCE -> Icons.Default.East
                }
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, StudioBorder),
                    modifier = Modifier
                        .clickable { onCycleLoopMode() }
                        .padding(start = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(loopIcon, contentDescription = "Loop Mode", tint = KorvaVioletLight, modifier = Modifier.size(11.dp))
                        Text(
                            text = when (project.loopMode) {
                                LoopMode.REPEAT -> "LOOP"
                                LoopMode.PING_PONG -> "P-PONG"
                                LoopMode.ONCE -> "ONCE"
                            },
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        VerticalDivider(color = StudioBorder, modifier = Modifier.height(14.dp))

        // --- RIGHT GROUP: KEYFRAME BUTTONS, SNAPPING, CHANNELS, FPS, TOTAL FRAMES ---
        if (!isCollapsed) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Keyframe Magnet Snapping Toggle
                IconButton(
                    onClick = onToggleSnap,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Grain,
                        contentDescription = "Snap to Keyframes",
                        tint = if (timelineSnap) StudioYellow else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Subtracks / Channel Splitting Toggle
                IconButton(
                    onClick = onToggleSubTracks,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = if (showSubTracks) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = "Toggle Channels",
                        tint = if (showSubTracks) KorvaVioletLight else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Onion Skin Quick Toggle
                IconButton(
                    onClick = onToggleOnionSkin,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Onion Skin",
                        tint = if (onionSkinEnabled) StudioCyan else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Work Area Loop Toggle
                Surface(
                    color = if (workAreaEnabled) StudioOrange.copy(alpha = 0.25f) else StudioSurfaceVariant,
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(0.5.dp, if (workAreaEnabled) StudioOrange else StudioBorder),
                    modifier = Modifier.clickable { onToggleWorkArea() }
                ) {
                    Text(
                        text = if (workAreaEnabled) "[IN/OUT]" else "RANGE",
                        color = if (workAreaEnabled) StudioOrange else TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Master Keyframe Add / Update Button
                Button(
                    onClick = onAddOrUpdateKeyframe,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentKfExists) KorvaVioletDark else KorvaVioletPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(21.dp)
                        .testTag("add_keyframe_button")
                ) {
                    Icon(
                        imageVector = if (currentKfExists) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (currentKfExists) "UPDATE KF" else "+ KEYFRAME",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete Keyframe if present on active frame
                if (currentKfExists) {
                    IconButton(
                        onClick = onDeleteKeyframe,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete KF", tint = StudioRed, modifier = Modifier.size(12.dp))
                    }
                }

                // FPS Selector
                var showFpsDropdown by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(0.5.dp, StudioBorder),
                        modifier = Modifier.clickable { showFpsDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text("${project.fps} FPS", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(10.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showFpsDropdown,
                        onDismissRequest = { showFpsDropdown = false },
                        modifier = Modifier.background(StudioSurfaceDark)
                    ) {
                        listOf(12, 24, 30, 60).forEach { fpsVal ->
                            DropdownMenuItem(
                                text = { Text("$fpsVal FPS", color = TextPrimary, fontSize = 10.sp) },
                                onClick = {
                                    onSetFps(fpsVal)
                                    showFpsDropdown = false
                                }
                            )
                        }
                    }
                }

                // Total Frames Selector (Supports 12, 24, 30, 48, 60, 90, 120, 180, 240)
                var showFramesDropdown by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(0.5.dp, StudioBorder),
                        modifier = Modifier.clickable { showFramesDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text("${project.totalFrames}F", color = KorvaVioletLight, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showFramesDropdown,
                        onDismissRequest = { showFramesDropdown = false },
                        modifier = Modifier.background(StudioSurfaceDark)
                    ) {
                        listOf(12, 16, 24, 30, 48, 60, 90, 120, 180, 240).forEach { count ->
                            DropdownMenuItem(
                                text = { Text("$count frames", color = TextPrimary, fontSize = 10.sp) },
                                onClick = {
                                    onSetTotalFrames(count)
                                    showFramesDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPONENT 2: KEYFRAME QUICK ACTION HUD & EASING CAROUSEL
// =============================================================================
@Composable
private fun KeyframeQuickActionHUD(
    layerName: String,
    keyframe: Keyframe,
    onNudgeLeft: () -> Unit,
    onNudgeRight: () -> Unit,
    onDuplicate: () -> Unit,
    onChangeEasing: (EasingType) -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(
        color = StudioSurfaceVariant.copy(alpha = 0.98f),
        border = BorderStroke(0.8.dp, KorvaVioletLight.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left: Active Keyframe Info Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = StudioYellow,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "KF @ F${keyframe.frame}",
                    color = StudioYellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• $layerName",
                    color = TextSecondary,
                    fontSize = 8.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Quick Nudge Frame Buttons
                IconButton(onClick = onNudgeLeft, modifier = Modifier.size(18.dp)) {
                    Text("-1F", color = KorvaVioletLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onNudgeRight, modifier = Modifier.size(18.dp)) {
                    Text("+1F", color = KorvaVioletLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            VerticalDivider(color = StudioBorder, modifier = Modifier.height(12.dp))

            // Center: Easing Curve Selectors Quick Strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = "EASING:", color = TextMuted, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)

                val quickEasings = listOf(
                    EasingType.LINEAR to "LIN",
                    EasingType.EASE_IN_QUAD to "IN",
                    EasingType.EASE_OUT_QUAD to "OUT",
                    EasingType.EASE_IN_OUT_CUBIC to "SMOOTH",
                    EasingType.BOUNCE_OUT to "BOUNCE",
                    EasingType.ELASTIC_OUT to "ELASTIC"
                )

                quickEasings.forEach { (easing, label) ->
                    val isSelected = keyframe.easing == easing
                    Surface(
                        color = if (isSelected) KorvaVioletPrimary else StudioPanelDark,
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (isSelected) KorvaVioletLight else StudioBorder
                        ),
                        modifier = Modifier.clickable { onChangeEasing(easing) }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 7.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            VerticalDivider(color = StudioBorder, modifier = Modifier.height(12.dp))

            // Right: Duplicate & Delete KF
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = KorvaVioletDark.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(0.5.dp, KorvaVioletPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onDuplicate() }
                ) {
                    Text(
                        text = "+ CLONE KF",
                        color = KorvaVioletLight,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete KF", tint = StudioRed, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

// =============================================================================
// COMPONENT 3: DOPESHEET MULTI-TRACK VIEW
// =============================================================================
@Composable
private fun DopeSheetTimelineView(
    project: KorProject,
    currentFrame: Float,
    selectedLayerId: String?,
    selectedKeyframe: Pair<String, Int>?,
    timelineZoom: Float,
    timelineSnap: Boolean,
    showSubTracks: Boolean,
    workAreaEnabled: Boolean,
    workAreaStart: Int,
    workAreaEnd: Int,
    onScrub: (Float) -> Unit,
    onSelectLayer: (String) -> Unit,
    onSelectKeyframe: (String, Int) -> Unit,
    onMoveKeyframe: (String, Int, Int) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    val rulerHeightDp = 24.dp
    val trackHeightDp = if (showSubTracks) 52.dp else 24.dp
    val leftHeaderWidthDp = 92.dp

    // Frame Spacing in DP:
    val startPaddingDp = 16.dp // Breathing room so Frame 0 is never hidden under the left sidebar
    val endPaddingDp = 32.dp   // Room after the last frame (F60, F120, etc.)
    val frameSpacingDp = (18f * timelineZoom).coerceIn(6f, 60f).dp

    val totalTrackWidthDp = startPaddingDp + (frameSpacingDp * (project.totalFrames - 1).coerceAtLeast(1)) + endPaddingDp

    // Auto scroll playhead into view when playing or scrubbing
    LaunchedEffect(currentFrame) {
        val estimatedPlayheadOffsetDp = startPaddingDp + (frameSpacingDp * currentFrame)
        // Keep in visible viewport if outside
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // --- 3A. LEFT FIXED COLUMN: TRACK HEADERS ---
        Column(
            modifier = Modifier
                .width(leftHeaderWidthDp)
                .fillMaxHeight()
                .background(StudioSurfaceDark)
                .shadow(4.dp)
                .border(width = 0.5.dp, color = StudioBorder)
        ) {
            // Track Header Cell
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rulerHeightDp)
                    .background(StudioPanelDark)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = KorvaVioletLight, modifier = Modifier.size(11.dp))
                    Text(
                        text = "LAYERS (${project.layers.size})",
                        color = TextSecondary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            HorizontalDivider(color = StudioBorder, thickness = 0.8.dp)

            // Scrollable Track Rows on the left
            Column(modifier = Modifier.weight(1f)) {
                project.layers.forEach { layer ->
                    val isSelected = layer.id == selectedLayerId
                    val shapeColor = getShapeColor(layer.shapeKind)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeightDp)
                            .background(
                                if (isSelected) KorvaVioletDark.copy(alpha = 0.35f) else StudioSurfaceDark
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(shapeColor)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                )

                                Text(
                                    text = layer.name,
                                    color = if (isSelected) KorvaVioletLight else TextPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Visibility & Lock Toggles
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Visibility",
                                    tint = if (layer.isVisible) TextMuted else StudioRed,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clickable { onToggleVisibility(layer.id) }
                                )
                                Icon(
                                    imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock",
                                    tint = if (layer.isLocked) StudioYellow else TextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clickable { onToggleLock(layer.id) }
                                )
                            }
                        }

                        // Sub-tracks label if expanded
                        if (showSubTracks) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 3.dp, start = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("POS", color = StudioCyan.copy(alpha = 0.8f), fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                                Text("ROT", color = StudioOrange.copy(alpha = 0.8f), fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                                Text("SCL", color = StudioGreen.copy(alpha = 0.8f), fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                                Text("OPAC", color = KorvaVioletLight.copy(alpha = 0.8f), fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }
        }

        // --- 3B. RIGHT SCROLLABLE CANVAS (PERFECT 1:1 PIXEL MATCH) ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
                .background(StudioPanelDark)
        ) {
            // Guarantee canvas is at least maxWidth to seamlessly fill empty space, or totalTrackWidthDp when large
            val canvasWidthDp = maxOf(totalTrackWidthDp, maxWidth)

            DopeSheetCanvas(
                project = project,
                currentFrame = currentFrame,
                selectedLayerId = selectedLayerId,
                selectedKeyframe = selectedKeyframe,
                canvasWidthDp = canvasWidthDp,
                startPaddingDp = startPaddingDp,
                frameSpacingDp = frameSpacingDp,
                trackHeightDp = trackHeightDp,
                rulerHeightDp = rulerHeightDp,
                timelineSnap = timelineSnap,
                workAreaEnabled = workAreaEnabled,
                workAreaStart = workAreaStart,
                workAreaEnd = workAreaEnd,
                onScrub = onScrub,
                onSelectLayer = onSelectLayer,
                onSelectKeyframe = onSelectKeyframe,
                onMoveKeyframe = onMoveKeyframe
            )
        }
    }
}

// =============================================================================
// COMPONENT 4: DOPESHEET CANVAS RENDERING WITH GUARANTEED COORDINATE PRECISION
// =============================================================================
@Composable
private fun DopeSheetCanvas(
    project: KorProject,
    currentFrame: Float,
    selectedLayerId: String?,
    selectedKeyframe: Pair<String, Int>?,
    canvasWidthDp: Dp,
    startPaddingDp: Dp,
    frameSpacingDp: Dp,
    trackHeightDp: Dp,
    rulerHeightDp: Dp,
    timelineSnap: Boolean,
    workAreaEnabled: Boolean,
    workAreaStart: Int,
    workAreaEnd: Int,
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

    val totalCanvasHeightDp = rulerHeightDp + (trackHeightDp * project.layers.size.coerceAtLeast(1)) + 30.dp

    // Dragging keyframe state
    var draggingKeyframe by remember { mutableStateOf<Triple<String, Int, Float>?>(null) } // layerId, originalFrame, currentDraggingX

    // Helper coordinate conversion functions
    fun frameToX(f: Float): Float = startPaddingPx + f * frameSpacingPx
    fun xToFrame(x: Float): Float = ((x - startPaddingPx) / frameSpacingPx).coerceIn(0f, (totalFrames - 1).toFloat())

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 20f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val majorTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#C084FC")
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val sharedDiamondPath = remember { Path() }
    val sharedPlayheadPath = remember { Path() }
    val kfStroke = remember { Stroke(width = 1.5f) }
    val kfSelectGlowStroke = remember { Stroke(width = 2.8f) }

    Canvas(
        modifier = Modifier
            .width(canvasWidthDp)
            .fillMaxHeight()
            .pointerInput(totalFrames, frameSpacingPx, startPaddingPx, project.layers) {
                detectTapGestures { tapOffset ->
                    val tapX = tapOffset.x
                    val tapY = tapOffset.y

                    // Check if tapped directly on a keyframe diamond
                    var tappedKf: Pair<String, Int>? = null
                    val trackIndex = ((tapY - rulerHeightPx) / trackHeightPx).toInt()

                    if (trackIndex in project.layers.indices) {
                        val layer = project.layers[trackIndex]
                        for (kf in layer.keyframes) {
                            val kfX = frameToX(kf.frame.toFloat())
                            val kfY = rulerHeightPx + trackIndex * trackHeightPx + trackHeightPx / 2f
                            if (abs(tapX - kfX) <= 14f && abs(tapY - kfY) <= 14f) {
                                tappedKf = Pair(layer.id, kf.frame)
                                break
                            }
                        }
                    }

                    if (tappedKf != null) {
                        onSelectKeyframe(tappedKf.first, tappedKf.second)
                    } else {
                        // Regular scrubbing
                        var frame = xToFrame(tapX)
                        if (timelineSnap) {
                            val roundFrame = frame.roundToInt().toFloat()
                            if (abs(frame - roundFrame) < 0.35f) {
                                frame = roundFrame
                            }
                        }
                        onScrub(frame)

                        // Select track if tapped in track area
                        if (trackIndex in project.layers.indices) {
                            onSelectLayer(project.layers[trackIndex].id)
                        }
                    }
                }
            }
            .pointerInput(totalFrames, frameSpacingPx, startPaddingPx, project.layers) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val trackIndex = ((startOffset.y - rulerHeightPx) / trackHeightPx).toInt()
                        if (trackIndex in project.layers.indices) {
                            val layer = project.layers[trackIndex]
                            for (kf in layer.keyframes) {
                                val kfX = frameToX(kf.frame.toFloat())
                                val kfY = rulerHeightPx + trackIndex * trackHeightPx + trackHeightPx / 2f
                                if (abs(startOffset.x - kfX) <= 16f && abs(startOffset.y - kfY) <= 16f) {
                                    draggingKeyframe = Triple(layer.id, kf.frame, startOffset.x)
                                    onSelectKeyframe(layer.id, kf.frame)
                                    return@detectDragGestures
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        val drag = draggingKeyframe
                        if (drag != null) {
                            val newFrame = xToFrame(drag.third).roundToInt().coerceIn(0, totalFrames - 1)
                            onMoveKeyframe(drag.first, drag.second, newFrame)
                            draggingKeyframe = null
                        }
                    },
                    onDragCancel = {
                        draggingKeyframe = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val drag = draggingKeyframe
                        if (drag != null) {
                            // Dragging keyframe
                            val minX = frameToX(0f)
                            val maxX = frameToX((totalFrames - 1).toFloat())
                            val updatedX = (drag.third + dragAmount.x).coerceIn(minX, maxX)
                            draggingKeyframe = Triple(drag.first, drag.second, updatedX)
                            val liveFrame = xToFrame(updatedX).roundToInt().toFloat()
                            onScrub(liveFrame)
                        } else {
                            // Scrubbing playhead
                            var frame = xToFrame(change.position.x)
                            if (timelineSnap) {
                                val roundFrame = frame.roundToInt().toFloat()
                                if (abs(frame - roundFrame) < 0.35f) {
                                    frame = roundFrame
                                }
                            }
                            onScrub(frame)
                        }
                    }
                )
            }
    ) {
        val totalTrackW = frameToX((totalFrames - 1).toFloat()) + startPaddingPx

        // -------------------------------------------------------------
        // 1. RULER BACKGROUND & SEAMLESS CANVAS FILL
        // -------------------------------------------------------------
        drawRect(
            color = StudioSurfaceDark,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, rulerHeightPx)
        )

        // Work Area Highlight (if enabled)
        if (workAreaEnabled) {
            val waStartX = frameToX(workAreaStart.toFloat())
            val waEndX = frameToX(workAreaEnd.toFloat())
            drawRect(
                color = StudioOrange.copy(alpha = 0.12f),
                topLeft = Offset(waStartX, 0f),
                size = Size(waEndX - waStartX, size.height)
            )
            drawLine(
                color = StudioOrange,
                start = Offset(waStartX, 0f),
                end = Offset(waStartX, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = StudioOrange,
                start = Offset(waEndX, 0f),
                end = Offset(waEndX, size.height),
                strokeWidth = 2f
            )
        }

        // -------------------------------------------------------------
        // 2. RULER TICKS & NUMBER LABELS & BACKGROUND GRID
        // -------------------------------------------------------------
        val labelStep = when {
            totalFrames >= 120 -> if (frameSpacingPx > 20f) 5 else 10
            totalFrames >= 60 -> if (frameSpacingPx > 20f) 5 else 10
            frameSpacingPx > 30f -> 1
            frameSpacingPx > 14f -> 5
            else -> 10
        }

        for (f in 0 until totalFrames) {
            val fx = frameToX(f.toFloat())
            val isMajor = f % labelStep == 0 || f == 0 || f == totalFrames - 1
            val isSemiMajor = f % 5 == 0

            val tickH = if (isMajor) rulerHeightPx * 0.58f else if (isSemiMajor) rulerHeightPx * 0.38f else rulerHeightPx * 0.22f

            drawLine(
                color = if (isMajor) KorvaVioletLight else if (isSemiMajor) StudioBorderLight else StudioBorder.copy(alpha = 0.7f),
                start = Offset(fx, rulerHeightPx - tickH),
                end = Offset(fx, rulerHeightPx),
                strokeWidth = if (isMajor) 1.5f else 1f
            )

            if (isMajor) {
                drawContext.canvas.nativeCanvas.drawText(
                    f.toString(),
                    fx,
                    rulerHeightPx - tickH - 2f,
                    if (f == currentFrame.toInt()) majorTextPaint else textPaint
                )
            }

            // Timeline Track Background Grid Lines across entire height
            drawLine(
                color = if (isMajor) StudioBorder.copy(alpha = 0.5f) else StudioBorder.copy(alpha = 0.15f),
                start = Offset(fx, rulerHeightPx),
                end = Offset(fx, size.height),
                strokeWidth = 1f
            )
        }

        // -------------------------------------------------------------
        // 3. TRACK ROWS & KEYFRAME DIAMONDS & EASING SPANS
        // -------------------------------------------------------------
        project.layers.forEachIndexed { index, layer ->
            val trackY = rulerHeightPx + index * trackHeightPx
            val isSelected = layer.id == selectedLayerId

            // Track Row Background (fills full width of canvas)
            val rowBgColor = when {
                isSelected -> KorvaVioletDark.copy(alpha = 0.22f)
                index % 2 == 0 -> StudioPanelDark
                else -> StudioSurfaceDark.copy(alpha = 0.45f)
            }

            drawRect(
                color = rowBgColor,
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackHeightPx)
            )

            // Horizontal Track Bottom Divider
            drawLine(
                color = StudioBorder.copy(alpha = 0.65f),
                start = Offset(0f, trackY + trackHeightPx),
                end = Offset(size.width, trackY + trackHeightPx),
                strokeWidth = 0.6f
            )

            // Keyframe Span Ribbon connecting keyframes
            val kfs = layer.keyframes
            if (kfs.size >= 2) {
                for (kIndex in 0 until kfs.size - 1) {
                    val kf1 = kfs[kIndex]
                    val kf2 = kfs[kIndex + 1]
                    val kf1X = frameToX(kf1.frame.toFloat())
                    val kf2X = frameToX(kf2.frame.toFloat())
                    val midY = trackY + trackHeightPx / 2f

                    val ribbonColor = when (kf1.easing) {
                        EasingType.LINEAR -> StudioCyan.copy(alpha = 0.4f)
                        EasingType.EASE_IN_OUT_CUBIC -> KorvaVioletLight.copy(alpha = 0.6f)
                        EasingType.BOUNCE_OUT -> StudioYellow.copy(alpha = 0.6f)
                        EasingType.ELASTIC_OUT -> StudioOrange.copy(alpha = 0.6f)
                        else -> KorvaVioletPrimary.copy(alpha = 0.4f)
                    }

                    // Draw connecting rounded capsule bar
                    drawRoundRect(
                        color = ribbonColor,
                        topLeft = Offset(kf1X, midY - 2.5f),
                        size = Size(kf2X - kf1X, 5f),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                }
            }

            // Draw Keyframe Diamonds on this track
            for (kf in kfs) {
                val isBeingDragged = draggingKeyframe?.first == layer.id && draggingKeyframe?.second == kf.frame
                val kfX = if (isBeingDragged) draggingKeyframe!!.third else frameToX(kf.frame.toFloat())
                val kfY = trackY + trackHeightPx / 2f

                val isCurrentKf = kf.frame == currentFrame.toInt()
                val isSelectedKf = selectedKeyframe?.first == layer.id && selectedKeyframe?.second == kf.frame

                val radius = when {
                    isSelectedKf || isBeingDragged -> 7.5f
                    isCurrentKf -> 6.5f
                    else -> 5f
                }

                sharedDiamondPath.reset()
                sharedDiamondPath.moveTo(kfX, kfY - radius)
                sharedDiamondPath.lineTo(kfX + radius, kfY)
                sharedDiamondPath.lineTo(kfX, kfY + radius)
                sharedDiamondPath.lineTo(kfX - radius, kfY)
                sharedDiamondPath.close()

                val fillColor = when {
                    isBeingDragged -> StudioGreen
                    isSelectedKf -> StudioYellow
                    isCurrentKf -> StudioOrange
                    isSelected -> KorvaVioletLight
                    else -> StudioCyan
                }

                // Outer Halo Glow for Selected / Dragged Keyframe
                if (isSelectedKf || isBeingDragged) {
                    drawPath(
                        sharedDiamondPath,
                        color = (if (isBeingDragged) StudioGreen else StudioYellow).copy(alpha = 0.45f),
                        style = kfSelectGlowStroke
                    )
                }

                // Diamond Fill
                drawPath(sharedDiamondPath, color = fillColor)

                // Diamond Crisp Outline
                drawPath(
                    sharedDiamondPath,
                    color = if (isSelectedKf || isBeingDragged) Color.White else StudioBorderLight,
                    style = kfStroke
                )
            }
        }

        // -------------------------------------------------------------
        // 4. NEXT-GEN LASER PLAYHEAD & SCRUBBER HANDLE
        // -------------------------------------------------------------
        val playheadX = frameToX(currentFrame)

        // Luminous Needle Glow
        drawLine(
            color = KorvaVioletPrimary.copy(alpha = 0.4f),
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 4.5f
        )

        // Sharp Precision Laser Needle
        drawLine(
            color = KorvaVioletLight,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 1.8f
        )

        // Playhead Scrubber Flag (Laser Arrow Head)
        val headWidth = 16f
        val headHeight = rulerHeightPx
        sharedPlayheadPath.reset()
        sharedPlayheadPath.moveTo(playheadX - headWidth / 2f, 0f)
        sharedPlayheadPath.lineTo(playheadX + headWidth / 2f, 0f)
        sharedPlayheadPath.lineTo(playheadX + headWidth / 2f, headHeight - 7f)
        sharedPlayheadPath.lineTo(playheadX, headHeight)
        sharedPlayheadPath.lineTo(playheadX - headWidth / 2f, headHeight - 7f)
        sharedPlayheadPath.close()

        drawPath(sharedPlayheadPath, color = KorvaVioletPrimary)
        drawPath(
            sharedPlayheadPath,
            color = Color.White,
            style = Stroke(width = 1.2f)
        )
    }
}

// =============================================================================
// COMPONENT 5: MOTION GRAPH & VELOCITY CURVES PREVIEW
// =============================================================================
@Composable
private fun MotionGraphTimelineView(
    project: KorProject,
    currentFrame: Float,
    selectedLayerId: String?,
    timelineZoom: Float,
    onScrub: (Float) -> Unit
) {
    val scrollState = rememberScrollState()
    val totalFrames = project.totalFrames
    val startPaddingDp = 24.dp
    val endPaddingDp = 48.dp
    val frameSpacingDp = (18f * timelineZoom).coerceIn(6f, 60f).dp
    val totalTrackWidthDp = startPaddingDp + (frameSpacingDp * (project.totalFrames - 1).coerceAtLeast(1)) + endPaddingDp

    val selectedLayer = project.layers.find { it.id == selectedLayerId } ?: project.layers.firstOrNull()
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianDark)
    ) {
        // Curve Legend & Channel Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(StudioSurfaceDark)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MOTION GRAPH: ${selectedLayer?.name ?: "No Selection"}",
                    color = TextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                // Channels Legend
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(color = StudioCyan, label = "X Pos")
                    LegendItem(color = StudioOrange, label = "Y Pos")
                    LegendItem(color = StudioYellow, label = "Rotation")
                    LegendItem(color = StudioGreen, label = "Scale")
                }
            }

            Text(
                text = "Cubic Bezier Interpolation",
                color = KorvaVioletLight,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Curves Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            val canvasWidthDp = maxOf(totalTrackWidthDp, maxWidth)
            val startPaddingPx = with(density) { startPaddingDp.toPx() }
            val frameSpacingPx = with(density) { frameSpacingDp.toPx() }

            fun frameToX(f: Float): Float = startPaddingPx + f * frameSpacingPx
            fun xToFrame(x: Float): Float = ((x - startPaddingPx) / frameSpacingPx).coerceIn(0f, (totalFrames - 1).toFloat())

            Canvas(
                modifier = Modifier
                    .width(canvasWidthDp)
                    .fillMaxHeight()
                    .pointerInput(totalFrames, frameSpacingPx, startPaddingPx) {
                        detectTapGestures { offset ->
                            onScrub(xToFrame(offset.x))
                        }
                    }
                    .pointerInput(totalFrames, frameSpacingPx, startPaddingPx) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            onScrub(xToFrame(change.position.x))
                        }
                    }
            ) {
                val midY = size.height / 2f

                // Grid lines across canvas
                drawLine(
                    color = StudioBorder,
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 1f
                )

                for (f in 0 until totalFrames step 5) {
                    val fx = frameToX(f.toFloat())
                    drawLine(
                        color = StudioBorder.copy(alpha = 0.3f),
                        start = Offset(fx, 0f),
                        end = Offset(fx, size.height),
                        strokeWidth = 1f
                    )
                }

                // Plot evaluateLayerAtFrame curves across all frames
                if (selectedLayer != null) {
                    val xPath = Path()
                    val yPath = Path()
                    val rotPath = Path()

                    for (f in 0 until totalFrames) {
                        val trans = EasingFunctions.evaluateLayerAtFrame(selectedLayer, f.toFloat())
                        val fx = frameToX(f.toFloat())

                        val mappedY_X = (midY - trans.x * 0.4f).coerceIn(10f, size.height - 10f)
                        val mappedY_Y = (midY - trans.y * 0.4f).coerceIn(10f, size.height - 10f)
                        val mappedY_Rot = (midY - (trans.rotation % 360f) * 0.2f).coerceIn(10f, size.height - 10f)

                        if (f == 0) {
                            xPath.moveTo(fx, mappedY_X)
                            yPath.moveTo(fx, mappedY_Y)
                            rotPath.moveTo(fx, mappedY_Rot)
                        } else {
                            xPath.lineTo(fx, mappedY_X)
                            yPath.lineTo(fx, mappedY_Y)
                            rotPath.lineTo(fx, mappedY_Rot)
                        }
                    }

                    // Draw Curves
                    drawPath(xPath, color = StudioCyan, style = Stroke(width = 2f))
                    drawPath(yPath, color = StudioOrange, style = Stroke(width = 2f))
                    drawPath(rotPath, color = StudioYellow, style = Stroke(width = 1.5f))

                    // Draw Keyframe Points on Curves
                    selectedLayer.keyframes.forEach { kf ->
                        val kfX = frameToX(kf.frame.toFloat())
                        val trans = EasingFunctions.evaluateLayerAtFrame(selectedLayer, kf.frame.toFloat())
                        val kfY_X = (midY - trans.x * 0.4f).coerceIn(10f, size.height - 10f)
                        val kfY_Y = (midY - trans.y * 0.4f).coerceIn(10f, size.height - 10f)

                        drawCircle(color = StudioCyan, radius = 4f, center = Offset(kfX, kfY_X))
                        drawCircle(color = Color.White, radius = 2f, center = Offset(kfX, kfY_X))

                        drawCircle(color = StudioOrange, radius = 4f, center = Offset(kfX, kfY_Y))
                        drawCircle(color = Color.White, radius = 2f, center = Offset(kfX, kfY_Y))
                    }
                }

                // Laser Playhead on Graph
                val playheadX = frameToX(currentFrame)
                drawLine(
                    color = KorvaVioletPrimary.copy(alpha = 0.5f),
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, size.height),
                    strokeWidth = 3f
                )
                drawLine(
                    color = KorvaVioletLight,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, size.height),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, color = TextMuted, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun getShapeColor(shapeKind: ShapeKind): Color {
    return when (shapeKind) {
        ShapeKind.SWORD -> StudioOrange
        ShapeKind.SLIME -> StudioGreen
        ShapeKind.SHIELD -> KorvaVioletLight
        ShapeKind.COIN -> StudioYellow
        ShapeKind.SLASH_FX -> StudioRed
        ShapeKind.RECTANGLE -> StudioCyan
        ShapeKind.ROUNDED_RECT -> KorvaVioletLight
        ShapeKind.CIRCLE -> KorvaVioletPrimary
        ShapeKind.STAR -> StudioYellow
        ShapeKind.TRIANGLE -> StudioGreen
        ShapeKind.DIAMOND -> StudioCyan
        ShapeKind.ARROW -> StudioOrange
    }
}
