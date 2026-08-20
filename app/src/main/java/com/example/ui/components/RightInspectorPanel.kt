package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimationLayer
import com.example.model.EasingFunctions
import com.example.model.EasingType
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RightInspectorPanel(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.project.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val isCollapsed by viewModel.isRightInspectorCollapsed.collectAsState()
    val inspectorTab by viewModel.inspectorTab.collectAsState()

    val selectedLayer = project.layers.find { it.id == selectedLayerId }

    val panelWidth by animateDpAsState(
        targetValue = if (isCollapsed) 28.dp else 245.dp,
        label = "RightPanelWidth"
    )

    Surface(
        color = StudioPanelDark,
        tonalElevation = 4.dp,
        modifier = modifier
            .width(panelWidth)
            .fillMaxHeight()
            .border(width = 1.dp, color = StudioBorder)
            .testTag("right_inspector_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Collapse Bar & Tab Switcher Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(StudioSurfaceDark)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.toggleRightInspector() },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = "Collapse Inspector",
                        tint = if (isCollapsed) KorvaVioletLight else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (!isCollapsed) {
                    // Modern Tab Pills: PROPERTIES vs LAYERS
                    Row(
                        modifier = Modifier
                            .background(StudioPanelDark, RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TabPillButton(
                            text = "PROPERTIES",
                            icon = Icons.Default.Tune,
                            isSelected = inspectorTab == 0,
                            onClick = { viewModel.setInspectorTab(0) }
                        )
                        TabPillButton(
                            text = "LAYERS",
                            icon = Icons.Default.Layers,
                            isSelected = inspectorTab == 1,
                            onClick = { viewModel.setInspectorTab(1) }
                        )
                    }
                }
            }

            if (!isCollapsed) {
                HorizontalDivider(color = StudioBorder)

                if (inspectorTab == 0) {
                    PropertiesTabContent(
                        viewModel = viewModel,
                        selectedLayer = selectedLayer,
                        currentFrame = currentFrame
                    )
                } else {
                    LayersTabContent(
                        viewModel = viewModel,
                        layers = project.layers,
                        selectedLayerId = selectedLayerId
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) KorvaVioletDark.copy(alpha = 0.6f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(0.5.dp, KorvaVioletPrimary) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) KorvaVioletLight else TextMuted,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                color = if (isSelected) TextPrimary else TextMuted,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PropertiesTabContent(
    viewModel: KorvaViewModel,
    selectedLayer: AnimationLayer?,
    currentFrame: Float
) {
    if (selectedLayer == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select a layer on canvas", color = TextSecondary, fontSize = 11.sp)
            }
        }
        return
    }

    val transform = EasingFunctions.evaluateLayerAtFrame(selectedLayer, currentFrame)
    val currentKf = selectedLayer.keyframes.find { it.frame == currentFrame.toInt() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // -------------------------------------------------------------
        // Layer Header with Status Badge & Reset Button
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedLayer.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${selectedLayer.shapeKind.displayName} • ${selectedLayer.keyframes.size} KFs",
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }

            Surface(
                color = if (currentKf != null) KorvaVioletDark else StudioSurfaceVariant,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (currentKf != null) KorvaVioletPrimary else StudioBorder
                )
            ) {
                Text(
                    text = if (currentKf != null) "KF SET" else "TWEEN",
                    color = if (currentKf != null) Color.White else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

        // -------------------------------------------------------------
        // 1. POSITION (X & Y)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("POSITION (px)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Center (0,0)",
                color = KorvaVioletLight,
                fontSize = 9.sp,
                modifier = Modifier
                    .clickable { viewModel.addOrUpdateKeyframeOnCurrentFrame(x = 0f, y = 0f) }
                    .padding(2.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberPropertyBox(
                label = "X",
                value = transform.x.toInt(),
                onValueChange = { viewModel.addOrUpdateKeyframeOnCurrentFrame(x = it.toFloat()) },
                modifier = Modifier.weight(1f)
            )
            NumberPropertyBox(
                label = "Y",
                value = transform.y.toInt(),
                onValueChange = { viewModel.addOrUpdateKeyframeOnCurrentFrame(y = it.toFloat()) },
                modifier = Modifier.weight(1f)
            )
        }

        // -------------------------------------------------------------
        // 2. ROTATION & SCALE (مع أزرار تحكم سريعة وتكبير سلس)
        // -------------------------------------------------------------
        Text("ROTATION & SCALE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NumberPropertyBox(
                label = "R°",
                value = transform.rotation.toInt(),
                onValueChange = { viewModel.addOrUpdateKeyframeOnCurrentFrame(rotation = it.toFloat()) },
                modifier = Modifier.weight(1f)
            )
            NumberPropertyBox(
                label = "SX%",
                value = (transform.scaleX * 100).toInt(),
                onValueChange = { viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleX = it / 100f) },
                modifier = Modifier.weight(1f)
            )
            NumberPropertyBox(
                label = "SY%",
                value = (transform.scaleY * 100).toInt(),
                onValueChange = { viewModel.addOrUpdateKeyframeOnCurrentFrame(scaleY = it / 100f) },
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Scale Presets (0.5x, 1x, 1.5x, 2x, Flip X, Flip Y)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(0.5f to "0.5x", 1.0f to "1.0x", 1.5f to "1.5x", 2.0f to "2.0x").forEach { (sc, label) ->
                Surface(
                    color = StudioSurfaceDark,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setScaleDirectly(sc, sc) }
                ) {
                    Box(modifier = Modifier.padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Flip Horizontal & Vertical
            Surface(
                color = StudioSurfaceDark,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier.clickable { viewModel.flipLayerHorizontal() }
            ) {
                Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip H", tint = TextSecondary, modifier = Modifier.size(12.dp))
                }
            }
            Surface(
                color = StudioSurfaceDark,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier.clickable { viewModel.flipLayerVertical() }
            ) {
                Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Flip V", tint = TextSecondary, modifier = Modifier.size(12.dp))
                }
            }
        }

        HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

        // -------------------------------------------------------------
        // 3. PIVOT ANCHOR (نقطة الارتكاز مع شبكة 3x3)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PIVOT ANCHOR", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "(${((selectedLayer.pivotX) * 100).toInt()}%, ${((selectedLayer.pivotY) * 100).toInt()}%)",
                color = StudioCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3x3 Anchor Preset Box
            Surface(
                color = StudioSurfaceDark,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder),
                modifier = Modifier.size(54.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val targetPx = col * 0.5f
                                val targetPy = row * 0.5f
                                val isSelected = kotlin.math.abs(selectedLayer.pivotX - targetPx) < 0.1f &&
                                        kotlin.math.abs(selectedLayer.pivotY - targetPy) < 0.1f

                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) StudioCyan else StudioBorder.copy(alpha = 0.6f))
                                        .clickable {
                                            viewModel.updateLayerPivot(selectedLayer.id, targetPx, targetPy)
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Pivot Coordinate Inputs
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NumberPropertyBox(
                        label = "PX%",
                        value = (selectedLayer.pivotX * 100).toInt(),
                        onValueChange = {
                            val v = (it / 100f).coerceIn(0f, 1f)
                            viewModel.updateLayerPivot(selectedLayer.id, v, selectedLayer.pivotY)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NumberPropertyBox(
                        label = "PY%",
                        value = (selectedLayer.pivotY * 100).toInt(),
                        onValueChange = {
                            val v = (it / 100f).coerceIn(0f, 1f)
                            viewModel.updateLayerPivot(selectedLayer.id, selectedLayer.pivotX, v)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = StudioSurfaceDark,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateLayerPivot(selectedLayer.id, 0.5f, 0.5f) }
                ) {
                    Box(modifier = Modifier.padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                        Text("Reset Pivot Center (50%, 50%)", color = KorvaVioletLight, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

        // -------------------------------------------------------------
        // 4. OPACITY SLIDER
        // -------------------------------------------------------------
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("OPACITY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${(transform.opacity * 100).toInt()}%", color = KorvaVioletLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = transform.opacity,
                onValueChange = { viewModel.addOrUpdateKeyframeOnCurrentFrame(opacity = it) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = KorvaVioletPrimary,
                    activeTrackColor = KorvaVioletPrimary,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.height(22.dp)
            )
        }

        HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

        // -------------------------------------------------------------
        // 4. EASING CURVE SELECTOR
        // -------------------------------------------------------------
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("EASING CURVE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { viewModel.openEasingDialog(true) },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = "Easing Graph", tint = StudioCyan, modifier = Modifier.size(13.dp))
                }
            }

            var showEasingDropdown by remember { mutableStateOf(false) }
            val currentEasing = currentKf?.easing ?: EasingType.EASE_IN_OUT_CUBIC

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showEasingDropdown = true },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceDark, contentColor = TextPrimary),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentEasing.displayName, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                DropdownMenu(
                    expanded = showEasingDropdown,
                    onDismissRequest = { showEasingDropdown = false },
                    modifier = Modifier.background(StudioSurfaceDark)
                ) {
                    EasingType.entries.forEach { easing ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(easing.displayName, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(easing.description, color = TextMuted, fontSize = 9.sp)
                                }
                            },
                            onClick = {
                                viewModel.setKeyframeEasing(easing)
                                showEasingDropdown = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = StudioBorder, thickness = 0.5.dp)

        // -------------------------------------------------------------
        // 5. COLOR TINT PALETTE
        // -------------------------------------------------------------
        Column {
            Text("COLOR TINT", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            val palette = listOf(
                0xFFA855F7, 0xFF38BDF8, 0xFF22C55E, 0xFFFACC15,
                0xFFEF4444, 0xFFEC4899, 0xFF6366F1, 0xFFFFFFFF
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                palette.forEach { colLong ->
                    val color = Color(colLong)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.5.dp, if (selectedLayer.shapeStyle.fillColor == colLong) Color.White else StudioBorder, CircleShape)
                            .clickable {
                                viewModel.updateLayerStyle(
                                    selectedLayer.id,
                                    selectedLayer.shapeStyle.copy(fillColor = colLong)
                                )
                            }
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 6. ACTIONS (Reset, Duplicate, Delete)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetTransform() },
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Reset", fontSize = 9.sp)
            }

            OutlinedButton(
                onClick = { viewModel.duplicateSelectedLayer() },
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Copy", fontSize = 9.sp)
            }

            Button(
                onClick = { viewModel.deleteSelectedLayer() },
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StudioRed),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Del", fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun LayersTabContent(
    viewModel: KorvaViewModel,
    layers: List<AnimationLayer>,
    selectedLayerId: String?
) {
    val sortedLayers = layers.sortedByDescending { it.zIndex }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(sortedLayers, key = { it.id }) { layer ->
            val isSelected = layer.id == selectedLayerId

            Surface(
                color = if (isSelected) KorvaVioletDark.copy(alpha = 0.45f) else StudioSurfaceDark,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) KorvaVioletPrimary else StudioBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectLayer(layer.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Visibility Eye
                    IconButton(
                        onClick = { viewModel.toggleLayerVisibility(layer.id) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility",
                            tint = if (layer.isVisible) TextPrimary else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Lock Button
                    IconButton(
                        onClick = { viewModel.toggleLayerLock(layer.id) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Lock",
                            tint = if (layer.isLocked) StudioYellow else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Layer Name & Type
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = layer.name,
                            color = if (isSelected) KorvaVioletLight else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${layer.shapeKind.displayName} (${layer.keyframes.size} KFs)",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }

                    // Reorder Up / Down
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        IconButton(
                            onClick = { viewModel.moveLayerZ(layer.id, 1) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { viewModel.moveLayerZ(layer.id, -1) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPropertyBox(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurfaceDark,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder),
        modifier = modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)

            IconButton(
                onClick = { onValueChange(value - 1) },
                modifier = Modifier.size(15.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(9.dp))
            }

            Text(
                text = "$value",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(15.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(9.dp))
            }
        }
    }
}
