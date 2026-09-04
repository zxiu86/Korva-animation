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
import com.example.model.LayerType
import com.example.model.vfx.*
import com.example.data.VFXPresets
import com.example.engine.vfx.KorvBinarySerializer
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
                    // Modern Tab Pills: PROPERTIES vs LAYERS vs VFX
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
                        TabPillButton(
                            text = "VFX",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = inspectorTab == 2,
                            onClick = { viewModel.setInspectorTab(2) }
                        )
                    }
                }
            }

            if (!isCollapsed) {
                HorizontalDivider(color = StudioBorder)

                when (inspectorTab) {
                    0 -> PropertiesTabContent(
                        viewModel = viewModel,
                        selectedLayer = selectedLayer,
                        currentFrame = currentFrame
                    )
                    1 -> LayersTabContent(
                        viewModel = viewModel,
                        layers = project.layers,
                        selectedLayerId = selectedLayerId
                    )
                    2 -> VfxTabContent(
                        viewModel = viewModel
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

        // Quick Scale Presets (0.5x, 1x, 1.5x, 2x)
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
        }

        // Dedicated Flip Direction Controls (يمين / يسار و أعلى / أسفل)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = StudioSurfaceDark,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KorvaVioletPrimary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.flipLayerHorizontal() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = KorvaVioletLight, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Flip H (↔)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                color = StudioSurfaceDark,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.flipLayerVertical() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Flip Vertical", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Flip V (↕)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun VfxTabContent(
    viewModel: KorvaViewModel
) {
    val effect by viewModel.currentVFXEffect.collectAsState()
    val isSimulating by viewModel.isVfxSimulating.collectAsState()
    val activeParticleCount by viewModel.vfxActiveParticleCount.collectAsState()
    val selectedEmitterIndex by viewModel.selectedEmitterIndex.collectAsState()
    val project by viewModel.project.collectAsState()

    val isNativeLoaded = com.korva.engine.VFXNativeBridge.isNativeLoaded || com.example.engine.vfx.VFXNativeBridge.isNativeLoaded
    var showAddModuleMenu by remember { mutableStateOf(false) }
    var showHexDump by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Engine Status & Performance Card
        Surface(
            color = if (isNativeLoaded) StudioGreen.copy(alpha = 0.12f) else KorvaVioletDark.copy(alpha = 0.35f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isNativeLoaded) StudioGreen else KorvaVioletPrimary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = if (isNativeLoaded) Icons.Default.CheckCircle else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isNativeLoaded) StudioGreen else StudioCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isNativeLoaded) "KORVA CORE v2.0 (ARM64)" else "KORVA VFX ENGINE v2.0",
                            color = if (isNativeLoaded) StudioGreen else StudioCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = KorvaVioletPrimary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$activeParticleCount pts",
                            color = KorvaVioletLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Controls Row: Play/Pause, Reset & Prewarm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleVFXSimulation() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimulating) StudioOrange else KorvaVioletPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Icon(
                            if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (isSimulating) "Pause" else "Simulate", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetVFXSimulation() },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.9f).height(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Reset", fontSize = 9.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.prewarmVFX(1.0f) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier.weight(0.8f).height(28.dp)
                    ) {
                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Warm", fontSize = 9.sp)
                    }
                }
            }
        }

        // 1.5. Timeline Animation Integration Card (تحريك التأثيرات بالتايم لاين)
        Surface(
            color = StudioSurfaceDark,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Text("TIMELINE ANIMATION", color = TextPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                    val fxCount = project.layers.count { it.type == LayerType.PARTICLE_FX }
                    Text("$fxCount Track(s)", color = Color(0xFFF59E0B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Link this VFX to a keyframed timeline track to animate its position, scale & rotation.",
                    color = TextSecondary,
                    fontSize = 8.5.sp,
                    lineHeight = 11.sp
                )

                Button(
                    onClick = { viewModel.addVFXLayer() },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Animated VFX Track", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. VFX Presets Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("VFX PRESETS (v2.0)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = StudioCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioCyan),
                modifier = Modifier.clickable { viewModel.createNewCustomVFX("Custom VFX") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(10.dp))
                    Text("New Custom", color = StudioCyan, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Preset Chips Grid
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val presets = listOf(
                "Vortex" to VFXPresets.createCosmicVortex(),
                "Meteor" to VFXPresets.createMeteorStrike(),
                "Arcs" to VFXPresets.createElectricArcStorm(),
                "Fire" to VFXPresets.createFireExplosion(),
                "Magic" to VFXPresets.createMagicSparkles(),
                "Slash" to VFXPresets.createEnergySlash(),
                "Coins" to VFXPresets.createGoldCoinBurst()
            )
            val chunkedPresets = presets.chunked(4)
            chunkedPresets.forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowPresets.forEach { (label, preset) ->
                        val isSelected = effect.effectId == preset.effectId
                        Surface(
                            color = if (isSelected) KorvaVioletDark else StudioSurfaceDark,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isSelected) KorvaVioletPrimary else StudioBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectVFXPreset(preset) }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Global Effect Configuration
        Surface(
            color = StudioSurfaceDark,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("EFFECT PROPERTIES", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Blend Mode", color = TextPrimary, fontSize = 9.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(BlendMode.ADDITIVE, BlendMode.NORMAL, BlendMode.SCREEN).forEach { mode ->
                            val isModeSelected = effect.blendMode == mode
                            Surface(
                                color = if (isModeSelected) KorvaVioletPrimary else StudioPanelDark,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable { viewModel.updateVFXBlendMode(mode) }
                            ) {
                                Text(
                                    text = mode.displayName.split(" ").first(),
                                    color = if (isModeSelected) Color.White else TextMuted,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Time Scale: ${String.format(java.util.Locale.US, "%.2f", effect.timeScale)}x", color = TextPrimary, fontSize = 9.5.sp)
                    Slider(
                        value = effect.timeScale,
                        onValueChange = { viewModel.updateVFXTimeScale(it) },
                        valueRange = 0.1f..3.0f,
                        modifier = Modifier.width(95.dp).height(18.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = StudioCyan,
                            activeTrackColor = StudioCyan,
                            inactiveTrackColor = StudioBorder
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration: ${String.format(java.util.Locale.US, "%.1f", effect.duration)}s", color = TextPrimary, fontSize = 9.5.sp)
                    Slider(
                        value = effect.duration,
                        onValueChange = { viewModel.updateVFXDuration(it) },
                        valueRange = 0.5f..8.0f,
                        modifier = Modifier.width(95.dp).height(18.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = KorvaVioletLight,
                            activeTrackColor = KorvaVioletPrimary,
                            inactiveTrackColor = StudioBorder
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Looping", color = TextPrimary, fontSize = 9.5.sp)
                    Switch(
                        checked = effect.looping,
                        onCheckedChange = { viewModel.updateVFXLooping(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KorvaVioletPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = StudioPanelDark
                        ),
                        modifier = Modifier.height(18.dp)
                    )
                }
            }
        }

        // 4. Emitters Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("EMITTERS (${effect.emitters.size})", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.addEmitterToCurrentEffect() },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add Emitter", tint = KorvaVioletLight, modifier = Modifier.size(16.dp))
            }
        }

        effect.emitters.forEachIndexed { index, emitter ->
            val isSelected = index == selectedEmitterIndex
            Surface(
                color = if (isSelected) KorvaVioletDark.copy(alpha = 0.25f) else StudioSurfaceDark,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) KorvaVioletPrimary else StudioBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { viewModel.setSelectedEmitterIndex(index) }
                        ) {
                            Icon(Icons.Default.Grain, contentDescription = null, tint = KorvaVioletLight, modifier = Modifier.size(14.dp))
                            Text(emitter.name, color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        if (effect.emitters.size > 1) {
                            IconButton(
                                onClick = {
                                    viewModel.setSelectedEmitterIndex(index)
                                    viewModel.removeSelectedEmitter()
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StudioRed, modifier = Modifier.size(13.dp))
                            }
                        }
                    }

                    if (isSelected) {
                        // Spawn Rate Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rate: ${emitter.spawnRate.toInt()}/s", color = TextSecondary, fontSize = 9.sp)
                            Slider(
                                value = emitter.spawnRate,
                                onValueChange = { viewModel.updateEmitterSpawnRate(index, it) },
                                valueRange = 0f..400f,
                                modifier = Modifier.width(95.dp).height(16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = KorvaVioletLight,
                                    activeTrackColor = KorvaVioletPrimary,
                                    inactiveTrackColor = StudioBorder
                                )
                            )
                        }

                        // Particle Lifetime Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lifetime: ${String.format(java.util.Locale.US, "%.1f", emitter.particleLifetime)}s", color = TextSecondary, fontSize = 9.sp)
                            Slider(
                                value = emitter.particleLifetime,
                                onValueChange = { viewModel.updateEmitterLifetime(index, it) },
                                valueRange = 0.2f..4.0f,
                                modifier = Modifier.width(95.dp).height(16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = KorvaVioletLight,
                                    activeTrackColor = KorvaVioletPrimary,
                                    inactiveTrackColor = StudioBorder
                                )
                            )
                        }

                        // Speed Max Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speed: ${emitter.speedMax.toInt()} px/s", color = TextSecondary, fontSize = 9.sp)
                            Slider(
                                value = emitter.speedMax,
                                onValueChange = { viewModel.updateEmitterSpeed(index, emitter.speedMin, it) },
                                valueRange = 10f..300f,
                                modifier = Modifier.width(95.dp).height(16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = KorvaVioletLight,
                                    activeTrackColor = KorvaVioletPrimary,
                                    inactiveTrackColor = StudioBorder
                                )
                            )
                        }

                        // Spread Angle Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Spread: ${emitter.spreadAngle.toInt()}°", color = TextSecondary, fontSize = 9.sp)
                            Slider(
                                value = emitter.spreadAngle,
                                onValueChange = { viewModel.updateEmitterSpread(index, it) },
                                valueRange = 0f..360f,
                                modifier = Modifier.width(95.dp).height(16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = KorvaVioletLight,
                                    activeTrackColor = KorvaVioletPrimary,
                                    inactiveTrackColor = StudioBorder
                                )
                            )
                        }

                        // Instant Burst Count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Burst: ${emitter.burstCount} pts", color = TextSecondary, fontSize = 9.sp)
                            Slider(
                                value = emitter.burstCount.toFloat(),
                                onValueChange = { viewModel.updateEmitterBurst(index, it.toInt(), emitter.burstInterval) },
                                valueRange = 0f..100f,
                                modifier = Modifier.width(95.dp).height(16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioCyan,
                                    activeTrackColor = StudioCyan,
                                    inactiveTrackColor = StudioBorder
                                )
                            )
                        }

                        // Shape Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            ShapeType.entries.forEach { shape ->
                                val isShapeSelected = emitter.shapeType == shape
                                Surface(
                                    color = if (isShapeSelected) KorvaVioletPrimary else StudioPanelDark,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateEmitterShape(index, shape) }
                                ) {
                                    Text(
                                        text = shape.displayName.take(3),
                                        color = if (isShapeSelected) Color.White else TextMuted,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Particle Geometry / Tapering (حادة من طرف وثخينة من طرف)
                        Spacer(Modifier.height(2.dp))
                        Text("PARTICLE GEOMETRY & TAPERING", color = StudioCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            ParticleGeometry.entries.forEach { geom ->
                                val isGeomSelected = emitter.particleGeometry == geom
                                Surface(
                                    color = if (isGeomSelected) StudioCyan else StudioPanelDark,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateEmitterParticleGeometry(index, geom) }
                                ) {
                                    Text(
                                        text = when (geom) {
                                            ParticleGeometry.TAPERED_NEEDLE -> "Sharp"
                                            ParticleGeometry.TEARDROP -> "Dart"
                                            ParticleGeometry.DIAMOND -> "Diam"
                                            ParticleGeometry.CRESCENT -> "Arc"
                                            ParticleGeometry.ELLIPSE -> "Oval"
                                        },
                                        color = if (isGeomSelected) Color.Black else TextMuted,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Tip Sharpness (Head Thickness)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tip Sharpness: ${String.format(java.util.Locale.US, "%.2f", emitter.headThickness)}x", color = TextSecondary, fontSize = 8.5.sp)
                            Slider(
                                value = emitter.headThickness,
                                onValueChange = { viewModel.updateEmitterHeadThickness(index, it) },
                                valueRange = 0.05f..1.5f,
                                modifier = Modifier.width(95.dp).height(16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioCyan,
                                    activeTrackColor = StudioCyan,
                                    inactiveTrackColor = StudioBorder
                                )
                            )
                        }

                        // Base Thickness (Tail Thickness)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Base Thickness: ${String.format(java.util.Locale.US, "%.1f", emitter.tailThickness)}x", color = TextSecondary, fontSize = 8.5.sp)
                            Slider(
                                value = emitter.tailThickness,
                                onValueChange = { viewModel.updateEmitterTailThickness(index, it) },
                                valueRange = 0.3f..3.0f,
                                modifier = Modifier.width(95.dp).height(16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = KorvaVioletLight,
                                    activeTrackColor = KorvaVioletPrimary,
                                    inactiveTrackColor = StudioBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Active Emitter Modules Controls (All 16 Modules)
        val selectedEmitter = effect.emitters.getOrNull(selectedEmitterIndex) ?: effect.emitters.firstOrNull()
        if (selectedEmitter != null) {
            Surface(
                color = StudioSurfaceDark,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ACTIVE MODULES (${selectedEmitter.modules.size})", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)

                        Box {
                            IconButton(
                                onClick = { showAddModuleMenu = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Module", tint = StudioCyan, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = showAddModuleMenu,
                                onDismissRequest = { showAddModuleMenu = false },
                                modifier = Modifier.background(StudioPanelDark)
                            ) {
                                val availableModules = listOf(
                                    ModuleTypeId.TURBULENCE,
                                    ModuleTypeId.VORTEX,
                                    ModuleTypeId.TRAIL,
                                    ModuleTypeId.VELOCITY_ALIGNMENT,
                                    ModuleTypeId.COLLISION,
                                    ModuleTypeId.DRAG,
                                    ModuleTypeId.ATTRACTOR,
                                    ModuleTypeId.GRAVITY,
                                    ModuleTypeId.SCALE_OVER_LIFETIME,
                                    ModuleTypeId.COLOR_OVER_LIFETIME,
                                    ModuleTypeId.ALPHA_OVER_LIFETIME,
                                    ModuleTypeId.FLIPBOOK,
                                    ModuleTypeId.COLOR_BY_SPEED
                                )
                                availableModules.forEach { type ->
                                    val isAlreadyPresent = selectedEmitter.modules.any { it.typeId == type }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                type.displayName,
                                                color = if (isAlreadyPresent) TextMuted else TextPrimary,
                                                fontSize = 11.sp
                                            )
                                        },
                                        onClick = {
                                            if (!isAlreadyPresent) {
                                                viewModel.addModuleToEmitter(selectedEmitterIndex, type)
                                            }
                                            showAddModuleMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Module: Gravity & Damping
                    val gravityMod = selectedEmitter.findGravityModule()
                    if (gravityMod != null) {
                        ModuleCard(
                            title = "Gravity & Damping",
                            icon = Icons.Default.South,
                            onRemove = { viewModel.removeModuleFromEmitter(selectedEmitterIndex, ModuleTypeId.GRAVITY) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gravity: ${gravityMod.gravity.toInt()} m/s²", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = gravityMod.gravity,
                                    onValueChange = { viewModel.updateEmitterGravity(selectedEmitterIndex, it) },
                                    valueRange = -50f..50f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                                )
                            }
                        }
                    }

                    // Module: Turbulence & Curl Noise
                    val turbMod = selectedEmitter.findTurbulenceModule()
                    if (turbMod != null) {
                        ModuleCard(
                            title = "Turbulence (Noise)",
                            icon = Icons.Default.Air,
                            onRemove = { viewModel.removeModuleFromEmitter(selectedEmitterIndex, ModuleTypeId.TURBULENCE) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Strength: ${turbMod.strength.toInt()}", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = turbMod.strength,
                                    onValueChange = { viewModel.updateTurbulenceModule(selectedEmitterIndex, it, turbMod.frequency, turbMod.scrollSpeed, turbMod.useCurlNoise) },
                                    valueRange = 0f..80f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Curl Noise", color = TextPrimary, fontSize = 9.sp)
                                Switch(
                                    checked = turbMod.useCurlNoise,
                                    onCheckedChange = { viewModel.updateTurbulenceModule(selectedEmitterIndex, turbMod.strength, turbMod.frequency, turbMod.scrollSpeed, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StudioCyan),
                                    modifier = Modifier.height(16.dp)
                                )
                            }
                        }
                    }

                    // Module: Vortex & Swirl
                    val vortexMod = selectedEmitter.findVortexModule()
                    if (vortexMod != null) {
                        ModuleCard(
                            title = "Vortex & Swirl",
                            icon = Icons.Default.RotateRight,
                            onRemove = { viewModel.removeModuleFromEmitter(selectedEmitterIndex, ModuleTypeId.VORTEX) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Swirl: ${vortexMod.vortexStrength.toInt()}", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = vortexMod.vortexStrength,
                                    onValueChange = { viewModel.updateVortexModule(selectedEmitterIndex, it, vortexMod.radialPull) },
                                    valueRange = -150f..150f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = KorvaVioletLight, activeTrackColor = KorvaVioletPrimary)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Inward Pull: ${vortexMod.radialPull.toInt()}", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = vortexMod.radialPull,
                                    onValueChange = { viewModel.updateVortexModule(selectedEmitterIndex, vortexMod.vortexStrength, it) },
                                    valueRange = 0f..60f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = KorvaVioletLight, activeTrackColor = KorvaVioletPrimary)
                                )
                            }
                        }
                    }

                    // Module: Attractor (Gravity Well)
                    val attrMod = selectedEmitter.findAttractorModule()
                    if (attrMod != null) {
                        ModuleCard(
                            title = "Attractor Field",
                            icon = Icons.Default.CenterFocusStrong,
                            onRemove = { viewModel.removeModuleFromEmitter(selectedEmitterIndex, ModuleTypeId.ATTRACTOR) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pull: ${attrMod.strength.toInt()}", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = attrMod.strength,
                                    onValueChange = { viewModel.updateAttractorModule(selectedEmitterIndex, attrMod.targetPosition.x, attrMod.targetPosition.y, it) },
                                    valueRange = 0f..150f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioYellow, activeTrackColor = StudioYellow)
                                )
                            }
                        }
                    }

                    // Module: Collision & Floor Bouncing
                    val collMod = selectedEmitter.findCollisionModule()
                    if (collMod != null) {
                        ModuleCard(
                            title = "Floor Collision",
                            icon = Icons.Default.VerticalAlignBottom,
                            onRemove = { viewModel.removeModuleFromEmitter(selectedEmitterIndex, ModuleTypeId.COLLISION) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Floor Y: ${collMod.floorY.toInt()} px", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = collMod.floorY,
                                    onValueChange = { viewModel.updateCollisionModule(selectedEmitterIndex, it, collMod.restitution, collMod.friction) },
                                    valueRange = 20f..220f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioOrange, activeTrackColor = StudioOrange)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bounce: ${(collMod.restitution * 100).toInt()}%", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = collMod.restitution,
                                    onValueChange = { viewModel.updateCollisionModule(selectedEmitterIndex, collMod.floorY, it, collMod.friction) },
                                    valueRange = 0f..1.0f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioOrange, activeTrackColor = StudioOrange)
                                )
                            }
                        }
                    }

                    // Module: Velocity Alignment (Stretch)
                    val alignMod = selectedEmitter.findVelocityAlignmentModule()
                    if (alignMod != null) {
                        ModuleCard(
                            title = "Velocity Alignment",
                            icon = Icons.Default.TrendingFlat,
                            onRemove = { viewModel.removeModuleFromEmitter(selectedEmitterIndex, ModuleTypeId.VELOCITY_ALIGNMENT) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Stretch: ${String.format(java.util.Locale.US, "%.3f", alignMod.stretchFactor)}", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = alignMod.stretchFactor,
                                    onValueChange = { viewModel.updateVelocityAlignmentModule(selectedEmitterIndex, it) },
                                    valueRange = 0.005f..0.08f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                                )
                            }
                        }
                    }

                    // Module: Ribbon Trail
                    val trailMod = selectedEmitter.findTrailModule()
                    if (trailMod != null) {
                        ModuleCard(
                            title = "Ribbon Trails",
                            icon = Icons.Default.Gesture,
                            onRemove = { viewModel.removeModuleFromEmitter(selectedEmitterIndex, ModuleTypeId.TRAIL) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Trail Life: ${String.format(java.util.Locale.US, "%.2f", trailMod.trailLifetime)}s", color = TextPrimary, fontSize = 9.sp)
                                Slider(
                                    value = trailMod.trailLifetime,
                                    onValueChange = { viewModel.updateTrailModule(selectedEmitterIndex, trailMod.segmentInterval, it, trailMod.maxPoints) },
                                    valueRange = 0.1f..0.8f,
                                    modifier = Modifier.width(90.dp).height(16.dp),
                                    colors = SliderDefaults.colors(thumbColor = KorvaVioletLight, activeTrackColor = KorvaVioletPrimary)
                                )
                            }
                        }
                    }

                    // Sub-Emitters Trigger Links
                    ModuleCard(
                        title = "Sub-Emitter Triggers",
                        icon = Icons.Default.AccountTree,
                        onRemove = null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("On Collision:", color = TextSecondary, fontSize = 9.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Surface(
                                    color = if (selectedEmitter.onCollisionSubEmitter == -1) KorvaVioletPrimary else StudioPanelDark,
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.clickable { viewModel.updateSubEmitters(selectedEmitterIndex, selectedEmitter.onBirthSubEmitter, selectedEmitter.onDeathSubEmitter, -1) }
                                ) {
                                    Text("None", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                                effect.emitters.forEachIndexed { eIdx, subE ->
                                    if (eIdx != selectedEmitterIndex) {
                                        val isSubSelected = selectedEmitter.onCollisionSubEmitter == eIdx
                                        Surface(
                                            color = if (isSubSelected) StudioOrange else StudioPanelDark,
                                            shape = RoundedCornerShape(3.dp),
                                            modifier = Modifier.clickable { viewModel.updateSubEmitters(selectedEmitterIndex, selectedEmitter.onBirthSubEmitter, selectedEmitter.onDeathSubEmitter, eIdx) }
                                        ) {
                                            Text("E$eIdx", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Binary .korv Export & Live Hex Inspector
        Surface(
            color = StudioSurfaceDark,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BINARY .KORV SPEC 1.10", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveKorvFileToDevice(viewModel.getApplication()) },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Export .korv", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showHexDump = !showHexDump },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (showHexDump) "Hide Hex" else "Inspect Hex", fontSize = 9.sp)
                    }
                }

                if (showHexDump) {
                    val bytes = viewModel.exportKorvBinary()
                    val validation = KorvBinarySerializer.validate(bytes)
                    val hexDump = KorvBinarySerializer.formatHexDump(bytes, maxLines = 10)

                    Surface(
                        color = StudioObsidianDark,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Size: ${bytes.size} bytes", color = StudioCyan, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                Text(if (validation.isValid) "CRC32 OK" else "CRC ERROR", color = if (validation.isValid) StudioGreen else StudioRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = hexDump,
                                color = TextSecondary,
                                fontSize = 7.5.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRemove: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = StudioPanelDark,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioBorder)
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(icon, contentDescription = null, tint = KorvaVioletLight, modifier = Modifier.size(12.dp))
                    Text(title, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(11.dp))
                    }
                }
            }
            content()
        }
    }
}
