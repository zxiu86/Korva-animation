package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccelerationType
import com.example.model.LoopMode
import com.example.model.ResolutionPreset
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTopBar(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.project.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLeftCollapsed by viewModel.isLeftToolbarCollapsed.collectAsState()
    val isRightCollapsed by viewModel.isRightInspectorCollapsed.collectAsState()
    val isBottomCollapsed by viewModel.isBottomTimelineCollapsed.collectAsState()

    var showResMenu by remember { mutableStateOf(false) }
    var showFpsMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAccelMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(project.name) }

    Surface(
        color = StudioPanelDark,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Brand & Project Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Back to Home Button
                IconButton(
                    onClick = { viewModel.navigateToHome() },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioSurfaceVariant)
                        .testTag("topbar_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "الرئيسية / Home",
                        tint = KorvaVioletLight,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Korva Studio Logo Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(KorvaVioletPrimary, KorvaVioletDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = "Korva Logo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // App & Project Name
                Column(
                    modifier = Modifier
                        .clickable {
                            editingName = project.name
                            showRenameDialog = true
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "korva",
                            color = KorvaVioletLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "2D",
                            color = StudioCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .background(StudioCyan.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = project.name,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(StudioBorder)
                )

                // Resolution Preset Dropdown (440p up to 2K)
                Box {
                    Button(
                        onClick = { showResMenu = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSurfaceDark,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("resolution_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Resolution",
                            modifier = Modifier.size(14.dp),
                            tint = StudioCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = project.resolution.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    DropdownMenu(
                        expanded = showResMenu,
                        onDismissRequest = { showResMenu = false },
                        modifier = Modifier.background(StudioSurfaceDark)
                    ) {
                        ResolutionPreset.entries.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(preset.label, color = TextPrimary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "${preset.width}x${preset.height}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setResolution(preset)
                                    showResMenu = false
                                },
                                leadingIcon = {
                                    if (project.resolution == preset) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = KorvaVioletLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // FPS Selector
                Box {
                    Button(
                        onClick = { showFpsMenu = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSurfaceDark,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "${project.fps} FPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = showFpsMenu,
                        onDismissRequest = { showFpsMenu = false },
                        modifier = Modifier.background(StudioSurfaceDark)
                    ) {
                        listOf(12, 24, 30, 60).forEach { fps ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "$fps FPS ${if (fps == 24) "(Standard)" else if (fps == 60) "(Smooth 2D)" else ""}",
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    viewModel.setFps(fps)
                                    showFpsMenu = false
                                },
                                leadingIcon = {
                                    if (project.fps == fps) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = KorvaVioletLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Speed Multiplier
                Box {
                    Button(
                        onClick = { showSpeedMenu = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSurfaceDark,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            modifier = Modifier.size(14.dp),
                            tint = StudioOrange
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${project.speedMultiplier}x",
                            fontSize = 11.sp
                        )
                    }

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                        modifier = Modifier.background(StudioSurfaceDark)
                    ) {
                        listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${speed}x ${if (speed == 1.0f) "(Normal)" else if (speed < 1f) "(Slow-Mo)" else "(Fast)"}",
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    viewModel.setSpeedMultiplier(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // Acceleration Curve
                Box {
                    IconButton(
                        onClick = { showAccelMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Scene Acceleration",
                            tint = if (project.accelerationType != AccelerationType.CONSTANT) KorvaVioletLight else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAccelMenu,
                        onDismissRequest = { showAccelMenu = false },
                        modifier = Modifier.background(StudioSurfaceDark)
                    ) {
                        AccelerationType.entries.forEach { accel ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(accel.label, color = TextPrimary, fontSize = 12.sp)
                                        Text(accel.multiplierDesc, color = TextMuted, fontSize = 10.sp)
                                    }
                                },
                                onClick = {
                                    viewModel.setAccelerationType(accel)
                                    showAccelMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Center Playback Trigger Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                // Loop Mode Switcher
                IconButton(
                    onClick = {
                        val next = when (project.loopMode) {
                            LoopMode.REPEAT -> LoopMode.PING_PONG
                            LoopMode.PING_PONG -> LoopMode.ONCE
                            LoopMode.ONCE -> LoopMode.REPEAT
                        }
                        viewModel.setLoopMode(next)
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = when (project.loopMode) {
                            LoopMode.REPEAT -> Icons.Default.Repeat
                            LoopMode.PING_PONG -> Icons.Default.CompareArrows
                            LoopMode.ONCE -> Icons.Default.RepeatOne
                        },
                        contentDescription = project.loopMode.label,
                        tint = KorvaVioletLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Primary Play / Pause Action
                Button(
                    onClick = { viewModel.togglePlay() },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) StudioRed else KorvaVioletPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "PAUSE" else "PLAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Right Group: Undo/Redo, Projects, Export, Panel Folders
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Undo
                IconButton(
                    onClick = { viewModel.undo() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = { viewModel.redo() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(StudioBorder)
                )

                // Project & Templates Manager
                Button(
                    onClick = { viewModel.openProjectDialog(true) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioSurfaceDark,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Projects",
                        modifier = Modifier.size(14.dp),
                        tint = KorvaVioletLight
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Projects", fontSize = 11.sp)
                }

                // Export Button (Sprite Sheet / .kor)
                Button(
                    onClick = { viewModel.openExportDialog(true) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KorvaVioletDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Export",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Zen / Fullscreen Toggle (Collapses all sidebars)
                IconButton(
                    onClick = { viewModel.toggleZenMode() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isLeftCollapsed && isRightCollapsed && isBottomCollapsed)
                            Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Zen Canvas Mode",
                        tint = StudioCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close / Collapse Header Button
                IconButton(
                    onClick = { viewModel.toggleTopBar() },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioSurfaceVariant.copy(alpha = 0.5f))
                        .testTag("close_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Close Header",
                        tint = KorvaVioletLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Project Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Project", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KorvaVioletPrimary,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editingName.isNotBlank()) {
                            viewModel.loadProject(project.copy(name = editingName))
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save", color = KorvaVioletLight)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StudioSurfaceDark
        )
    }
}

@Composable
fun CollapsedHeaderStrip(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.project.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()

    Surface(
        color = StudioPanelDark,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .border(width = 0.5.dp, color = StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Expand Button & Project info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { viewModel.toggleTopBar() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Header",
                        tint = KorvaVioletLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "korva 2D",
                    color = KorvaVioletLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "•",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Text(
                    text = project.name,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Center: Play / Pause mini button & Frame indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { viewModel.togglePlay() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) StudioRed else KorvaVioletLight,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = "F: ${currentFrame.toInt()} / ${project.totalFrames}",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Right: Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.undo() },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = TextSecondary, modifier = Modifier.size(12.dp))
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = TextSecondary, modifier = Modifier.size(12.dp))
                }

                Text(
                    text = "Export",
                    color = KorvaVioletLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.openExportDialog(true) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

