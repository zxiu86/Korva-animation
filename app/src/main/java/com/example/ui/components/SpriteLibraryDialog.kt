package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SpriteAsset
import com.example.data.SpriteLibrary
import com.example.model.ShapeKind
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

@Composable
fun SpriteLibraryDialog(
    viewModel: KorvaViewModel,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredAssets = remember(selectedCategory) {
        if (selectedCategory == "All") SpriteLibrary.ASSETS
        else SpriteLibrary.ASSETS.filter { it.category == selectedCategory }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .testTag("sprite_library_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "2D Game Sprite Library",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select game assets, character limbs, weapons, or particles to add",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpriteLibrary.CATEGORIES.forEach { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KorvaVioletDark,
                                selectedLabelColor = Color.White,
                                containerColor = StudioSurfaceDark,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) KorvaVioletPrimary else StudioBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Assets Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredAssets, key = { it.id }) { asset ->
                        SpriteAssetCard(
                            asset = asset,
                            onSelect = {
                                viewModel.addSpriteFromLibrary(asset)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpriteAssetCard(
    asset: SpriteAsset,
    onSelect: () -> Unit
) {
    Surface(
        color = StudioSurfaceDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Preview Canvas
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioObsidianDark),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(54.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val fillColor = Color(asset.defaultFill)
                    val strokeColor = Color(asset.defaultStroke)
                    val w = size.width * 0.8f
                    val h = size.height * 0.8f
                    val left = cx - w / 2f
                    val top = cy - h / 2f

                    when (asset.shapeKind) {
                        ShapeKind.ROUNDED_RECT -> {
                            drawRoundRect(
                                color = fillColor,
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = strokeColor,
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                                style = Stroke(1.5f)
                            )
                        }
                        ShapeKind.CIRCLE, ShapeKind.COIN -> {
                            val r = kotlin.math.min(w, h) / 2f
                            drawCircle(color = fillColor, radius = r, center = Offset(cx, cy))
                            drawCircle(color = strokeColor, radius = r, center = Offset(cx, cy), style = Stroke(1.5f))
                        }
                        ShapeKind.STAR -> {
                            val path = Path()
                            val rOut = kotlin.math.min(w, h) / 2f
                            val rIn = rOut * 0.45f
                            for (i in 0 until 10) {
                                val r = if (i % 2 == 0) rOut else rIn
                                val a = i * Math.PI.toFloat() / 5f - Math.PI.toFloat() / 2f
                                val px = cx + r * kotlin.math.cos(a)
                                val py = cy + r * kotlin.math.sin(a)
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            path.close()
                            drawPath(path, fillColor)
                            drawPath(path, strokeColor, style = Stroke(1.5f))
                        }
                        ShapeKind.SWORD -> {
                            val path = Path().apply {
                                moveTo(cx, top)
                                lineTo(cx + w * 0.35f, top + h * 0.25f)
                                lineTo(cx + w * 0.2f, top + h * 0.75f)
                                lineTo(cx - w * 0.2f, top + h * 0.75f)
                                lineTo(cx - w * 0.35f, top + h * 0.25f)
                                close()
                            }
                            drawPath(path, fillColor)
                            drawPath(path, strokeColor, style = Stroke(1.5f))
                            drawRect(fillColor, topLeft = Offset(cx - w * 0.4f, top + h * 0.75f), size = Size(w * 0.8f, h * 0.1f))
                        }
                        ShapeKind.SHIELD -> {
                            val path = Path().apply {
                                moveTo(left, top)
                                lineTo(left + w, top)
                                lineTo(left + w, top + h * 0.5f)
                                quadraticTo(cx, top + h, left, top + h * 0.5f)
                                close()
                            }
                            drawPath(path, fillColor)
                            drawPath(path, strokeColor, style = Stroke(1.5f))
                        }
                        ShapeKind.SLIME -> {
                            val path = Path().apply {
                                moveTo(cx, top)
                                cubicTo(left + w, top + h * 0.2f, left + w, top + h, cx, top + h)
                                cubicTo(left, top + h, left, top + h * 0.2f, cx, top)
                                close()
                            }
                            drawPath(path, fillColor)
                            drawPath(path, strokeColor, style = Stroke(1.5f))
                        }
                        else -> {
                            drawRect(fillColor, topLeft = Offset(left, top), size = Size(w, h))
                            drawRect(strokeColor, topLeft = Offset(left, top), size = Size(w, h), style = Stroke(1.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = asset.name,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = asset.category,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}
