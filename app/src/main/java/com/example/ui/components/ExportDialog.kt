package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

@Composable
fun ExportDialog(
    viewModel: KorvaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    var exportTab by remember { mutableStateOf(0) } // 0: .kor Project, 1: Sprite Sheet PNG, 2: Godot/Unity 2D

    // Sprite sheet settings
    var sheetColumns by remember { mutableStateOf(4) }
    var sheetStep by remember { mutableStateOf(1) }
    var frameSize by remember { mutableStateOf(128) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var exportedPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exportTab, sheetColumns, sheetStep, frameSize) {
        if (exportTab == 1) {
            previewBitmap = viewModel.generateSpriteSheetBitmap(sheetColumns, sheetStep, frameSize)
        }
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
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("export_dialog")
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
                            text = "Export 2D Animation",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Support for .kor files, Sprite Sheet PNGs, and Game Engines",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Export Format Tabs
                TabRow(
                    selectedTabIndex = exportTab,
                    containerColor = StudioSurfaceDark,
                    contentColor = KorvaVioletLight,
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = exportTab == 0,
                        onClick = { exportTab = 0 },
                        text = { Text(".kor Project File", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = exportTab == 1,
                        onClick = { exportTab = 1 },
                        text = { Text("Sprite Sheet PNG", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = exportTab == 2,
                        onClick = { exportTab = 2 },
                        text = { Text("Godot / Unity 2D", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents
                Box(modifier = Modifier.weight(1f)) {
                    when (exportTab) {
                        0 -> KorProjectExportTab(viewModel, context, onDismiss)
                        1 -> SpriteSheetExportTab(
                            previewBitmap = previewBitmap,
                            sheetColumns = sheetColumns,
                            sheetStep = sheetStep,
                            frameSize = frameSize,
                            onColumnsChange = { sheetColumns = it },
                            onStepChange = { sheetStep = it },
                            onFrameSizeChange = { frameSize = it },
                            onExport = {
                                val path = viewModel.saveSpriteSheetToDevice(context, sheetColumns, sheetStep, frameSize)
                                exportedPath = path
                            },
                            exportedPath = exportedPath
                        )
                        2 -> EngineExportTab(viewModel, context)
                    }
                }
            }
        }
    }
}

@Composable
private fun KorProjectExportTab(
    viewModel: KorvaViewModel,
    context: Context,
    onDismiss: () -> Unit
) {
    val jsonStr = remember { viewModel.exportKorJsonString() }
    var copyNotice by remember { mutableStateOf(false) }
    var savePath by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Native .kor Project Format",
            color = KorvaVioletLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "The .kor format encapsulates all animation layers, keyframes, transforms, resolution, and scene settings. You can re-import this file anytime.",
            color = TextSecondary,
            fontSize = 11.sp
        )

        // Actions: Save to device / Copy JSON
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val path = viewModel.saveKorFileToDevice(context)
                    savePath = path
                },
                colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save .kor to Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Korva Animation .kor", jsonStr)
                    clipboard.setPrimaryClip(clip)
                    copyNotice = true
                },
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (copyNotice) "Copied!" else "Copy .kor JSON", fontSize = 12.sp)
            }
        }

        if (savePath != null) {
            Surface(
                color = StudioGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioGreen)
            ) {
                Text(
                    text = "Saved successfully: $savePath",
                    color = StudioGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Preview of JSON
        Surface(
            color = StudioObsidianDark,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().height(160.dp)
        ) {
            Text(
                text = jsonStr,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun SpriteSheetExportTab(
    previewBitmap: Bitmap?,
    sheetColumns: Int,
    sheetStep: Int,
    frameSize: Int,
    onColumnsChange: (Int) -> Unit,
    onStepChange: (Int) -> Unit,
    onFrameSizeChange: (Int) -> Unit,
    onExport: () -> Unit,
    exportedPath: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Settings Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Columns
            Column(modifier = Modifier.weight(1f)) {
                Text("COLUMNS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(2, 4, 8).forEach { cols ->
                        FilterChip(
                            selected = sheetColumns == cols,
                            onClick = { onColumnsChange(cols) },
                            label = { Text("$cols", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Step (Every N frames)
            Column(modifier = Modifier.weight(1f)) {
                Text("FRAME STEP", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 2, 4).forEach { st ->
                        FilterChip(
                            selected = sheetStep == st,
                            onClick = { onStepChange(st) },
                            label = { Text("${st}f", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Frame Cell Size
            Column(modifier = Modifier.weight(1f)) {
                Text("CELL SIZE", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(64, 128, 256).forEach { sz ->
                        FilterChip(
                            selected = frameSize == sz,
                            onClick = { onFrameSizeChange(sz) },
                            label = { Text("${sz}px", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Sprite Sheet Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioObsidianDark)
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Sprite Sheet Preview",
                    modifier = Modifier.padding(8.dp).fillMaxSize()
                )
            } else {
                Text("Generating preview...", color = TextMuted, fontSize = 11.sp)
            }
        }

        // Export Button
        Button(
            onClick = onExport,
            colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export PNG Sprite Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (exportedPath != null) {
            Surface(
                color = StudioGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioGreen)
            ) {
                Text(
                    text = "Saved PNG to: $exportedPath",
                    color = StudioGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun EngineExportTab(
    viewModel: KorvaViewModel,
    context: Context
) {
    val godotJson = remember { viewModel.exportGodotJson() }
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Godot 4 / Unity 2D Keyframe Track Data",
            color = KorvaVioletLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Exports time-stamped transformation tracks (position, rotation, scale, easing) ready for Godot AnimationPlayer or Unity Animation Clips.",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Godot/Unity Animation JSON", godotJson)
                clipboard.setPrimaryClip(clip)
                copied = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copied) "Animation Data Copied!" else "Copy Engine Animation Track Data", fontSize = 12.sp)
        }

        Surface(
            color = StudioObsidianDark,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().height(160.dp)
        ) {
            Text(
                text = godotJson,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
            )
        }
    }
}
