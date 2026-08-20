package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorTool
import com.example.model.ShapeKind
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

@Composable
fun LeftToolsPanel(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTool by viewModel.activeTool.collectAsState()
    val isCollapsed by viewModel.isLeftToolbarCollapsed.collectAsState()
    val onionSkinEnabled by viewModel.onionSkinEnabled.collectAsState()
    val gridVisible by viewModel.gridVisible.collectAsState()
    val snapToGrid by viewModel.snapToGrid.collectAsState()

    var showShapesMenu by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addCustomImageLayer(uri, context)
        }
    }

    val panelWidth by animateDpAsState(
        targetValue = if (isCollapsed) 28.dp else 52.dp,
        label = "LeftPanelWidth"
    )

    Surface(
        color = StudioPanelDark,
        tonalElevation = 4.dp,
        modifier = modifier
            .width(panelWidth)
            .fillMaxHeight()
            .border(width = 1.dp, color = StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Collapse / Expand Toggle Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clickable { viewModel.toggleLeftToolbar() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                    contentDescription = "Collapse Tools",
                    tint = if (isCollapsed) KorvaVioletLight else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (!isCollapsed) {
                HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // -------------------------------------------------
                    // SECTION 1: TRANSFORM TOOLS
                    // -------------------------------------------------
                    ToolIconButton(
                        icon = Icons.Default.NearMe,
                        tooltip = "Select & Move (V)",
                        isSelected = activeTool == EditorTool.SELECT,
                        onClick = { viewModel.setActiveTool(EditorTool.SELECT) },
                        tag = "tool_select"
                    )

                    ToolIconButton(
                        icon = Icons.Default.RotateRight,
                        tooltip = "Rotate Tool (R)",
                        isSelected = activeTool == EditorTool.ROTATE,
                        onClick = { viewModel.setActiveTool(EditorTool.ROTATE) },
                        tag = "tool_rotate"
                    )

                    ToolIconButton(
                        icon = Icons.Default.OpenInFull,
                        tooltip = "Scale / Resize (S)",
                        isSelected = activeTool == EditorTool.SCALE,
                        onClick = { viewModel.setActiveTool(EditorTool.SCALE) },
                        tag = "tool_scale"
                    )

                    ToolIconButton(
                        icon = Icons.Default.Adjust,
                        tooltip = "Pivot Anchor (P)",
                        isSelected = activeTool == EditorTool.PIVOT,
                        onClick = { viewModel.setActiveTool(EditorTool.PIVOT) },
                        tag = "tool_pivot"
                    )

                    ToolIconButton(
                        icon = Icons.Default.PanTool,
                        tooltip = "Hand Pan Stage (H)",
                        isSelected = activeTool == EditorTool.HAND_PAN,
                        onClick = { viewModel.setActiveTool(EditorTool.HAND_PAN) },
                        tag = "tool_pan"
                    )

                    HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))

                    // -------------------------------------------------
                    // SECTION 2: CREATE & ASSETS
                    // -------------------------------------------------
                    // Shape Creator Dropdown
                    Box {
                        ToolIconButton(
                            icon = Icons.Default.Category,
                            tooltip = "Add 2D Shape / Weapon",
                            isSelected = showShapesMenu,
                            tint = KorvaVioletLight,
                            onClick = { showShapesMenu = true },
                            tag = "add_shape_button"
                        )

                        DropdownMenu(
                            expanded = showShapesMenu,
                            onDismissRequest = { showShapesMenu = false },
                            modifier = Modifier.background(StudioSurfaceDark)
                        ) {
                            Text(
                                "Add Shape / Game Weapon",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            ShapeKind.entries.forEach { shape ->
                                DropdownMenuItem(
                                    text = { Text(shape.displayName, color = TextPrimary, fontSize = 11.sp) },
                                    onClick = {
                                        viewModel.addShapeLayer(shape)
                                        showShapesMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Game Sprite Library Opener
                    ToolIconButton(
                        icon = Icons.Default.Games,
                        tooltip = "Game Sprite Library",
                        isSelected = false,
                        tint = StudioCyan,
                        onClick = { viewModel.openSpriteLibrary(true) },
                        tag = "sprite_library_button"
                    )

                    // Import Custom Image from Device
                    ToolIconButton(
                        icon = Icons.Default.AddPhotoAlternate,
                        tooltip = "Import PNG/JPG",
                        isSelected = false,
                        tint = StudioGreen,
                        onClick = { imagePickerLauncher.launch("image/*") },
                        tag = "import_image_button"
                    )

                    HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))

                    // -------------------------------------------------
                    // SECTION 3: VIEWPORT TOGGLES
                    // -------------------------------------------------
                    // Onion Skinning Toggle
                    ToolIconButton(
                        icon = Icons.Default.Layers,
                        tooltip = "Onion Skinning",
                        isSelected = onionSkinEnabled,
                        tint = if (onionSkinEnabled) StudioCyan else TextMuted,
                        onClick = { viewModel.toggleOnionSkin() },
                        tag = "onion_skin_toggle"
                    )

                    // Grid Overlay Toggle
                    ToolIconButton(
                        icon = Icons.Default.GridOn,
                        tooltip = "Grid View",
                        isSelected = gridVisible,
                        tint = if (gridVisible) KorvaVioletLight else TextMuted,
                        onClick = { viewModel.toggleGrid() },
                        tag = "grid_toggle"
                    )

                    // Snap to Grid Toggle
                    ToolIconButton(
                        icon = Icons.Default.Grid4x4,
                        tooltip = "Snap to Grid",
                        isSelected = snapToGrid,
                        tint = if (snapToGrid) StudioYellow else TextMuted,
                        onClick = { viewModel.toggleSnapToGrid() },
                        tag = "snap_grid_toggle"
                    )

                    HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))

                    // Zoom In
                    ToolIconButton(
                        icon = Icons.Default.ZoomIn,
                        tooltip = "Zoom In",
                        isSelected = false,
                        onClick = { viewModel.updateViewportZoom(1.2f) }
                    )

                    // Zoom Out
                    ToolIconButton(
                        icon = Icons.Default.ZoomOut,
                        tooltip = "Zoom Out",
                        isSelected = false,
                        onClick = { viewModel.updateViewportZoom(0.8f) }
                    )

                    // Reset Center View
                    ToolIconButton(
                        icon = Icons.Default.CenterFocusStrong,
                        tooltip = "Reset View",
                        isSelected = false,
                        onClick = { viewModel.resetViewport() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    tooltip: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tint: Color = if (isSelected) KorvaVioletLight else TextSecondary,
    tag: String? = null
) {
    val bg = if (isSelected) KorvaVioletDark.copy(alpha = 0.5f) else Color.Transparent
    val borderCol = if (isSelected) KorvaVioletPrimary else Color.Transparent

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .let { if (tag != null) it.testTag(tag) else it },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
