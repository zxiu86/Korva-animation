package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimationLayer
import com.example.model.KorProject
import com.example.model.ShapeKind
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

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

    val selectedLayer = project.layers.find { it.id == selectedLayerId }
    val currentKfExists = selectedLayer?.keyframes?.any { it.frame == currentFrame.toInt() } == true

    val density = LocalDensity.current

    val panelHeight by animateDpAsState(
        targetValue = if (isCollapsed) 28.dp else timelineHeightDp.dp,
        label = "TimelineHeightAnim"
    )

    Surface(
        color = StudioPanelDark,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight)
            .border(width = 1.dp, color = StudioBorder)
            .testTag("bottom_timeline_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // -------------------------------------------------------------
            // 1. VERTICAL RESIZE HANDLE BAR (مقبض تحكم صغير لتطويل وتقصير التايم لاين)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(StudioSurfaceDark)
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
                // Subtle grab handle indicator pill
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (isCollapsed) KorvaVioletLight else StudioBorderLight)
                )
            }

            // -------------------------------------------------------------
            // 2. TIMELINE TRANSPORT & CONTROLS HEADER BAR
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(StudioSurfaceDark)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Collapse Toggle, Title & Frame Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleBottomTimeline() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Timeline",
                            tint = if (isCollapsed) KorvaVioletLight else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "TIMELINE",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Current Frame Badge
                    Surface(
                        color = KorvaVioletDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KorvaVioletPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "F ${currentFrame.toInt()} / ${project.totalFrames}",
                            color = KorvaVioletLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    // Quick Height Preset Button (Compact / Medium / Tall)
                    if (!isCollapsed) {
                        IconButton(
                            onClick = { viewModel.toggleTimelineHeightPreset() },
                            modifier = Modifier.size(20.dp),
                            enabled = !isCollapsed
                        ) {
                            Icon(
                                imageVector = Icons.Default.Height,
                                contentDescription = "Resize Timeline",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                if (!isCollapsed) {
                    // Center: Transport Buttons (Start, Step Back, PLAY/PAUSE, Step Forward, End)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.jumpToStart() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Start", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }

                        IconButton(
                            onClick = { viewModel.stepBackward() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.ArrowLeft, contentDescription = "Prev Frame", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        // Play/Pause Main Button
                        Surface(
                            color = if (isPlaying) StudioRed.copy(alpha = 0.2f) else KorvaVioletPrimary,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlaying) StudioRed else KorvaVioletLight.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .clickable { viewModel.togglePlay() }
                                .testTag("timeline_play_pause_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.stepForward() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.ArrowRight, contentDescription = "Next Frame", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { viewModel.jumpToEnd() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "End", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Right: Add/Delete Keyframe, FPS & Frame Count Adjuster
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Add Keyframe Button
                        Button(
                            onClick = { viewModel.addOrUpdateKeyframeOnCurrentFrame() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentKfExists) KorvaVioletDark else KorvaVioletPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .height(20.dp)
                                .testTag("add_keyframe_button")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (currentKfExists) "KF UPDATED" else "+ KF", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Delete Keyframe Button (Visible when on a keyframe)
                        if (currentKfExists) {
                            IconButton(
                                onClick = { viewModel.deleteKeyframeOnCurrentFrame() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete KF", tint = StudioRed, modifier = Modifier.size(13.dp))
                            }
                        }

                        // FPS Badge
                        Surface(
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(3.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder)
                        ) {
                            Text(
                                text = "${project.fps} FPS",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        // Total Frames Adjuster Dropdown
                        var showFramesDropdown by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                color = StudioSurfaceVariant,
                                shape = RoundedCornerShape(3.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder),
                                modifier = Modifier.clickable { showFramesDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${project.totalFrames} F",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showFramesDropdown,
                                onDismissRequest = { showFramesDropdown = false },
                                modifier = Modifier.background(StudioSurfaceDark)
                            ) {
                                listOf(12, 16, 24, 32, 48, 64, 96, 120).forEach { count ->
                                    DropdownMenuItem(
                                        text = { Text("$count frames", color = TextPrimary, fontSize = 11.sp) },
                                        onClick = {
                                            viewModel.setTotalFrames(count)
                                            showFramesDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. SYNCHRONIZED TRACKS AREA (LEFT HEADERS + RIGHT SCROLLABLE RULER)
            // -------------------------------------------------------------
            if (!isCollapsed) {
                val trackHeightDp = 24.dp
                val rulerHeightDp = 20.dp
                val scrollState = rememberScrollState()
                val frameSpacingPx = 20f
                val totalRulerWidthPx = (project.totalFrames * frameSpacingPx).coerceAtLeast(650f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // ---------------------------------------------------------
                    // 3A. LEFT FIXED COLUMN: Track & Layer Headers (130dp width)
                    // ---------------------------------------------------------
                    Column(
                        modifier = Modifier
                            .width(135.dp)
                            .fillMaxHeight()
                            .background(StudioSurfaceDark)
                            .border(width = 0.5.dp, color = StudioBorder)
                    ) {
                        // Header Track Cell
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
                                Icon(Icons.Default.Layers, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "TRACKS (${project.layers.size})",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

                        // Layer Rows Headers
                        project.layers.forEach { layer ->
                            val isSelected = layer.id == selectedLayerId
                            val shapeColor = getShapeColor(layer.shapeKind)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeightDp)
                                    .background(
                                        if (isSelected) KorvaVioletDark.copy(alpha = 0.35f) else StudioSurfaceDark
                                    )
                                    .clickable { viewModel.selectLayer(layer.id) }
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Color badge dot
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(shapeColor)
                                    )

                                    // Layer name
                                    Text(
                                        text = layer.name,
                                        color = if (isSelected) KorvaVioletLight else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Quick Visibility & Lock Mini Icons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Visibility",
                                        tint = if (layer.isVisible) TextMuted else StudioRed,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.toggleLayerVisibility(layer.id) }
                                    )
                                    Icon(
                                        imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Lock",
                                        tint = if (layer.isLocked) StudioYellow else TextMuted.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.toggleLayerLock(layer.id) }
                                    )
                                }
                            }

                            HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }

                    // ---------------------------------------------------------
                    // 3B. RIGHT SCROLLABLE COLUMN: Timeline Canvas (Ruler + Tracks + Playhead)
                    // ---------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(scrollState)
                            .background(StudioPanelDark)
                    ) {
                        ModernTimelineCanvas(
                            project = project,
                            currentFrame = currentFrame,
                            selectedLayerId = selectedLayerId,
                            frameSpacingPx = frameSpacingPx,
                            totalWidthPx = totalRulerWidthPx,
                            trackHeightDp = trackHeightDp,
                            rulerHeightDp = rulerHeightDp,
                            onScrub = { frame -> viewModel.scrubToFrame(frame) },
                            onSelectLayer = { layerId -> viewModel.selectLayer(layerId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernTimelineCanvas(
    project: KorProject,
    currentFrame: Float,
    selectedLayerId: String?,
    frameSpacingPx: Float,
    totalWidthPx: Float,
    trackHeightDp: androidx.compose.ui.unit.Dp,
    rulerHeightDp: androidx.compose.ui.unit.Dp,
    onScrub: (Float) -> Unit,
    onSelectLayer: (String) -> Unit
) {
    val totalFrames = project.totalFrames
    val density = LocalDensity.current

    val trackHeightPx = with(density) { trackHeightDp.toPx() }
    val rulerHeightPx = with(density) { rulerHeightDp.toPx() }
    val totalCanvasHeightDp = rulerHeightDp + (trackHeightDp * project.layers.size.coerceAtLeast(1)) + 20.dp

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8") // TextSecondary
            textSize = 22f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val majorTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#C4B5FD") // KorvaVioletLight
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val sharedDiamondPath = remember { Path() }
    val sharedFlagPath = remember { Path() }
    val kfStroke = remember { androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f) }
    val flagStroke = remember { androidx.compose.ui.graphics.drawscope.Stroke(width = 1f) }

    Canvas(
        modifier = Modifier
            .width((totalWidthPx + 120f).dp)
            .height(totalCanvasHeightDp)
            .pointerInput(totalFrames, frameSpacingPx) {
                detectTapGestures { tapOffset ->
                    val frame = (tapOffset.x / frameSpacingPx).coerceIn(0f, (totalFrames - 1).toFloat())
                    onScrub(frame)

                    // Track row selection by tap
                    val trackIndex = ((tapOffset.y - rulerHeightPx) / trackHeightPx).toInt()
                    if (trackIndex in project.layers.indices) {
                        onSelectLayer(project.layers[trackIndex].id)
                    }
                }
            }
            .pointerInput(totalFrames, frameSpacingPx) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val frame = (change.position.x / frameSpacingPx).coerceIn(0f, (totalFrames - 1).toFloat())
                    onScrub(frame)
                }
            }
    ) {
        val totalW = totalFrames * frameSpacingPx

        // 1. Draw Ruler Background Bar
        drawRect(
            color = StudioSurfaceDark,
            topLeft = Offset(0f, 0f),
            size = Size(totalW, rulerHeightPx)
        )

        // 2. Draw Frame Ticks and Frame Number Texts
        for (f in 0 until totalFrames) {
            val fx = f * frameSpacingPx
            val isMajor = f % 5 == 0 || f == 0
            val isSemiMajor = f % 2 == 0
            val tickH = if (isMajor) rulerHeightPx * 0.55f else if (isSemiMajor) rulerHeightPx * 0.35f else rulerHeightPx * 0.2f

            // Tick Mark on ruler
            drawLine(
                color = if (isMajor) KorvaVioletLight else if (isSemiMajor) StudioBorderLight else StudioBorder,
                start = Offset(fx, rulerHeightPx - tickH),
                end = Offset(fx, rulerHeightPx),
                strokeWidth = if (isMajor) 1.5f else 1f
            )

            // Draw frame number label every 5 frames
            if (isMajor) {
                drawContext.canvas.nativeCanvas.drawText(
                    f.toString(),
                    fx,
                    rulerHeightPx - tickH - 2f,
                    if (f == currentFrame.toInt()) majorTextPaint else textPaint
                )
            }

            // Subtle vertical timeline grid lines across all tracks
            drawLine(
                color = if (isMajor) StudioBorder.copy(alpha = 0.55f) else StudioBorder.copy(alpha = 0.2f),
                start = Offset(fx, rulerHeightPx),
                end = Offset(fx, size.height),
                strokeWidth = 1f
            )
        }

        // 3. Draw Track Rows & Keyframe Diamonds
        project.layers.forEachIndexed { index, layer ->
            val trackY = rulerHeightPx + index * trackHeightPx
            val isSelected = layer.id == selectedLayerId

            // Alternate row backgrounds
            val rowBgColor = when {
                isSelected -> KorvaVioletDark.copy(alpha = 0.22f)
                index % 2 == 0 -> StudioPanelDark
                else -> StudioSurfaceDark.copy(alpha = 0.4f)
            }

            drawRect(
                color = rowBgColor,
                topLeft = Offset(0f, trackY),
                size = Size(totalW, trackHeightPx)
            )

            // Horizontal Track divider
            drawLine(
                color = StudioBorder.copy(alpha = 0.6f),
                start = Offset(0f, trackY + trackHeightPx),
                end = Offset(totalW, trackY + trackHeightPx),
                strokeWidth = 0.5f
            )

            // Keyframe connection span line (between first and last keyframes)
            val kfs = layer.keyframes
            if (kfs.size >= 2) {
                val minF = kfs.first().frame
                val maxF = kfs.last().frame
                val startX = minF * frameSpacingPx
                val endX = maxF * frameSpacingPx
                val midY = trackY + trackHeightPx / 2f

                drawLine(
                    color = if (isSelected) KorvaVioletPrimary.copy(alpha = 0.6f) else StudioBorderLight.copy(alpha = 0.4f),
                    start = Offset(startX, midY),
                    end = Offset(endX, midY),
                    strokeWidth = 2f
                )
            }

            // Keyframe Diamonds on this track (using recycled Path)
            for (kf in kfs) {
                val kfX = kf.frame * frameSpacingPx
                val kfY = trackY + trackHeightPx / 2f
                val isCurrentKf = kf.frame == currentFrame.toInt()
                val radius = if (isCurrentKf) 6.5f else 5f

                sharedDiamondPath.reset()
                sharedDiamondPath.moveTo(kfX, kfY - radius)
                sharedDiamondPath.lineTo(kfX + radius, kfY)
                sharedDiamondPath.lineTo(kfX, kfY + radius)
                sharedDiamondPath.lineTo(kfX - radius, kfY)
                sharedDiamondPath.close()

                val fillColor = when {
                    isCurrentKf -> StudioOrange
                    isSelected -> KorvaVioletLight
                    else -> StudioCyan
                }

                drawPath(sharedDiamondPath, color = fillColor)
                // Diamond stroke outline
                drawPath(
                    sharedDiamondPath,
                    color = if (isCurrentKf) Color.White else StudioBorderLight,
                    style = kfStroke
                )
            }
        }

        // 4. Draw Glowing Playhead Needle & Scrubber Head
        val playheadX = currentFrame * frameSpacingPx

        // Subtle playhead glow line
        drawLine(
            color = KorvaVioletPrimary.copy(alpha = 0.35f),
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 4f
        )

        // Sharp playhead laser line
        drawLine(
            color = KorvaVioletLight,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 1.8f
        )

        // Playhead Top Flag (Scrubber Head with Arrow)
        val headWidth = 14f
        val headHeight = rulerHeightPx
        sharedFlagPath.reset()
        sharedFlagPath.moveTo(playheadX - headWidth / 2f, 0f)
        sharedFlagPath.lineTo(playheadX + headWidth / 2f, 0f)
        sharedFlagPath.lineTo(playheadX + headWidth / 2f, headHeight - 6f)
        sharedFlagPath.lineTo(playheadX, headHeight)
        sharedFlagPath.lineTo(playheadX - headWidth / 2f, headHeight - 6f)
        sharedFlagPath.close()

        drawPath(sharedFlagPath, color = KorvaVioletPrimary)
        drawPath(
            sharedFlagPath,
            color = Color.White,
            style = flagStroke
        )
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
