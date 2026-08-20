package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.EasingFunctions
import com.example.model.EasingType
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

@Composable
fun EasingGraphDialog(
    viewModel: KorvaViewModel,
    onDismiss: () -> Unit
) {
    val selectedLayer = viewModel.getSelectedLayer()
    val currentKf = selectedLayer?.keyframes?.find { it.frame == viewModel.currentFrame.value.toInt() }
    var selectedEasing by remember { mutableStateOf(currentKf?.easing ?: EasingType.EASE_IN_OUT_CUBIC) }

    // Live continuous animation for physics ball demo
    val infiniteTransition = rememberInfiniteTransition(label = "EasingDemo")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Prog"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("easing_graph_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Easing & Interpolation Curve",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedEasing.description,
                            color = KorvaVioletLight,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Curve & Live Physics Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioObsidianDark)
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val w = size.width
                        val h = size.height

                        // Grid lines
                        for (i in 0..4) {
                            val y = h * (i / 4f)
                            val x = w * (i / 4f)
                            drawLine(color = StudioBorder.copy(alpha = 0.4f), start = Offset(0f, y), end = Offset(w, y))
                            drawLine(color = StudioBorder.copy(alpha = 0.4f), start = Offset(x, 0f), end = Offset(x, h))
                        }

                        // Plot the easing curve path
                        val path = Path()
                        val steps = 100
                        for (i in 0..steps) {
                            val t = i / steps.toFloat()
                            val value = EasingFunctions.calculate(t, selectedEasing)
                            val px = t * w
                            val py = h - (value * h)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, color = KorvaVioletPrimary, style = Stroke(width = 3f))

                        // Current animation playhead dot on curve
                        val currentVal = EasingFunctions.calculate(animationProgress, selectedEasing)
                        val dotX = animationProgress * w
                        val dotY = h - (currentVal * h)
                        drawCircle(color = StudioCyan, radius = 6f, center = Offset(dotX, dotY))

                        // Right physics demo track
                        val trackX = w - 16f
                        drawLine(color = StudioBorderLight, start = Offset(trackX, 0f), end = Offset(trackX, h), strokeWidth = 2f)
                        val ballY = h - (currentVal * h)
                        drawCircle(color = KorvaVioletLight, radius = 8f, center = Offset(trackX, ballY))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Easing Options Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(EasingType.entries) { easing ->
                        val isSelected = easing == selectedEasing
                        Surface(
                            color = if (isSelected) KorvaVioletDark else StudioSurfaceDark,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) KorvaVioletPrimary else StudioBorder
                            ),
                            modifier = Modifier.clickable { selectedEasing = easing }
                        ) {
                            Text(
                                text = easing.displayName,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Apply Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.setKeyframeEasing(selectedEasing)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary)
                    ) {
                        Text("Apply to Keyframe", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
