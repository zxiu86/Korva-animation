package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun ExportDialog(
    viewModel: KorvaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    var exportTab by remember { mutableStateOf(0) } // 0: .kor Project, 1: Sprite Sheet PNG, 2: Godot/Unity 2D, 3: .korv VFX

    // Sprite sheet settings
    var sheetColumns by remember { mutableStateOf(4) }
    var sheetStep by remember { mutableStateOf(1) }
    var frameSize by remember { mutableStateOf(128) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
                            text = "Choose custom export path (مسار الحفظ) or system file picker",
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
                        text = { Text(".kor Project", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = exportTab == 1,
                        onClick = { exportTab = 1 },
                        text = { Text("Sprite Sheet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = exportTab == 2,
                        onClick = { exportTab = 2 },
                        text = { Text("Godot / Unity", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = exportTab == 3,
                        onClick = { exportTab = 3 },
                        text = { Text("⚡ .korv VFX", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioCyan) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents
                Box(modifier = Modifier.weight(1f)) {
                    when (exportTab) {
                        0 -> KorProjectExportTab(viewModel, context, onDismiss)
                        1 -> SpriteSheetExportTab(
                            viewModel = viewModel,
                            context = context,
                            previewBitmap = previewBitmap,
                            sheetColumns = sheetColumns,
                            sheetStep = sheetStep,
                            frameSize = frameSize,
                            onColumnsChange = { sheetColumns = it },
                            onStepChange = { sheetStep = it },
                            onFrameSizeChange = { frameSize = it }
                        )
                        2 -> EngineExportTab(viewModel, context)
                        3 -> KorvBinaryExportTab(viewModel, context)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDestinationSelector(
    context: Context,
    defaultFilename: String,
    extension: String,
    mimeType: String,
    onPathExport: (targetDirectory: File, customFilename: String) -> String?,
    onUriExport: (Uri) -> Boolean
) {
    var customFilename by remember(defaultFilename) { mutableStateOf(defaultFilename) }
    var selectedPresetIndex by remember { mutableStateOf(0) } // 0: Downloads, 1: Documents, 2: Pictures, 3: App Private, 4: Custom Path
    var customFolderPath by remember { mutableStateOf("/storage/emulated/0/Download") }
    var exportedPathResult by remember { mutableStateOf<String?>(null) }
    var copyNotice by remember { mutableStateOf(false) }

    // SAF Document Picker Launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = onUriExport(uri)
            if (ok) {
                exportedPathResult = uri.path ?: uri.toString()
            }
        }
    }

    val presetDirs = remember(context) {
        listOf(
            "Downloads" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir),
            "Documents" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir),
            "Pictures" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) ?: context.filesDir),
            "App Storage" to context.filesDir
        )
    }

    val currentTargetDir = remember(selectedPresetIndex, customFolderPath, presetDirs) {
        if (selectedPresetIndex in 0..3) {
            presetDirs[selectedPresetIndex].second
        } else {
            File(customFolderPath.ifBlank { "/storage/emulated/0/Download" })
        }
    }

    Surface(
        color = StudioObsidianDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📂 Target Export Path & Name (تحديد مسار الحفظ)",
                    color = KorvaVioletLight,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Custom Directory Supported",
                    color = StudioCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Filename input
            OutlinedTextField(
                value = customFilename,
                onValueChange = { customFilename = it },
                label = { Text("Custom File Name", fontSize = 10.sp) },
                singleLine = true,
                trailingIcon = { Text(".$extension", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KorvaVioletLight,
                    unfocusedBorderColor = StudioBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Destination Presets
            Text("CHOOSE TARGET DIRECTORY:", color = TextSecondary, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presetDirs.forEachIndexed { idx, (label, _) ->
                    FilterChip(
                        selected = selectedPresetIndex == idx,
                        onClick = { selectedPresetIndex = idx },
                        label = { Text(label, fontSize = 9.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterChip(
                    selected = selectedPresetIndex == 4,
                    onClick = { selectedPresetIndex = 4 },
                    label = { Text("Custom...", fontSize = 9.5.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom folder path field if index == 4
            if (selectedPresetIndex == 4) {
                OutlinedTextField(
                    value = customFolderPath,
                    onValueChange = { customFolderPath = it },
                    label = { Text("Enter Directory Path", fontSize = 10.sp) },
                    placeholder = { Text("/storage/emulated/0/Download", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Path preview label
            Text(
                text = "Full Target: ${currentTargetDir.absolutePath}/${customFilename.ifBlank { defaultFilename }}.$extension",
                color = TextMuted,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace
            )

            // Action Buttons: Direct Save to Directory vs System File Picker (SAF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val fullPath = onPathExport(currentTargetDir, customFilename)
                        exportedPathResult = fullPath
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Selected Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val extName = if (customFilename.endsWith(".$extension")) customFilename else "$customFilename.$extension"
                        safLauncher.launch(extName)
                    },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("System File Picker...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Output feedback result
            if (exportedPathResult != null) {
                Surface(
                    color = StudioGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("✓ Saved successfully to:", color = StudioGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(exportedPathResult!!, color = TextPrimary, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                        }
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Export Path", exportedPathResult))
                                copyNotice = true
                            }
                        ) {
                            Text(if (copyNotice) "Copied Path!" else "Copy Path", fontSize = 10.sp, color = StudioGreen)
                        }
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
    val project by viewModel.project.collectAsState()
    var copyNotice by remember { mutableStateOf(false) }

    val defaultName = remember(project.name) {
        project.name.ifBlank { "project" }.replace("\\s+".toRegex(), "_").lowercase()
    }

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
            text = "The .kor format encapsulates all animation layers, keyframes, transforms, resolution, and scene settings. Select your preferred export destination path below.",
            color = TextSecondary,
            fontSize = 11.sp
        )

        // Custom Destination Path Selector
        ExportDestinationSelector(
            context = context,
            defaultFilename = defaultName,
            extension = "kor",
            mimeType = "application/json",
            onPathExport = { dir, filename ->
                viewModel.saveKorFileToPath(dir, filename)
            },
            onUriExport = { uri ->
                viewModel.exportStringToUri(context, uri, jsonStr)
            }
        )

        // Copy JSON Button
        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Korva Animation .kor", jsonStr)
                clipboard.setPrimaryClip(clip)
                copyNotice = true
            },
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copyNotice) "JSON Copied to Clipboard!" else "Copy .kor JSON to Clipboard", fontSize = 12.sp)
        }

        // Preview of JSON
        Text("Raw JSON Preview:", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Surface(
            color = StudioObsidianDark,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().height(140.dp)
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
    viewModel: KorvaViewModel,
    context: Context,
    previewBitmap: Bitmap?,
    sheetColumns: Int,
    sheetStep: Int,
    frameSize: Int,
    onColumnsChange: (Int) -> Unit,
    onStepChange: (Int) -> Unit,
    onFrameSizeChange: (Int) -> Unit
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
                .height(160.dp)
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

        // Custom Path Destination Selector
        ExportDestinationSelector(
            context = context,
            defaultFilename = "spritesheet_${System.currentTimeMillis()}",
            extension = "png",
            mimeType = "image/png",
            onPathExport = { dir, filename ->
                viewModel.saveSpriteSheetToPath(dir, filename, sheetColumns, sheetStep, frameSize)
            },
            onUriExport = { uri ->
                val bitmap = viewModel.generateSpriteSheetBitmap(sheetColumns, sheetStep, frameSize)
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                val bytes = bos.toByteArray()
                viewModel.exportBytesToUri(context, uri, bytes)
            }
        )
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

        ExportDestinationSelector(
            context = context,
            defaultFilename = "godot_track_data",
            extension = "json",
            mimeType = "application/json",
            onPathExport = { dir, filename ->
                try {
                    if (!dir.exists()) dir.mkdirs()
                    val fName = if (filename.endsWith(".json")) filename else "$filename.json"
                    val file = File(dir, fName)
                    file.writeText(godotJson)
                    file.absolutePath
                } catch (e: Exception) {
                    null
                }
            },
            onUriExport = { uri ->
                viewModel.exportStringToUri(context, uri, godotJson)
            }
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
            Text(if (copied) "Animation Data Copied!" else "Copy Engine Track Data to Clipboard", fontSize = 12.sp)
        }

        Surface(
            color = StudioObsidianDark,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().height(140.dp)
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

@Composable
private fun KorvBinaryExportTab(
    viewModel: KorvaViewModel,
    context: Context
) {
    val effect by viewModel.currentVFXEffect.collectAsState()
    val binaryBytes = remember(effect) { viewModel.exportKorvBinary() }
    val validation = remember(binaryBytes) { com.example.engine.vfx.KorvBinarySerializer.validate(binaryBytes) }
    val hexDump = remember(binaryBytes) { com.example.engine.vfx.KorvBinarySerializer.formatHexDump(binaryBytes, 12) }
    var copiedHex by remember { mutableStateOf(false) }

    val isNativeLoaded = com.korva.engine.VFXNativeBridge.isNativeLoaded || com.example.engine.vfx.VFXNativeBridge.isNativeLoaded

    val defaultEffectName = remember(effect.name) {
        effect.name.ifBlank { "vfx_effect" }.replace("\\s+".toRegex(), "_").lowercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Native Engine Status Banner
        Surface(
            color = if (isNativeLoaded) StudioGreen.copy(alpha = 0.15f) else StudioCyan.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isNativeLoaded) StudioGreen else StudioCyan)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isNativeLoaded) Icons.Default.CheckCircle else Icons.Default.Memory,
                        contentDescription = null,
                        tint = if (isNativeLoaded) StudioGreen else StudioCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (isNativeLoaded) "Native Engine Connected (libkorva_vfx.so)" else "Korva VFX Engine 1.9.5 (Ready for libkorva_vfx.so)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isNativeLoaded) "Target ABI: arm64-v8a • Direct C++ Simulation" else "Place libkorva_vfx.so into /app/src/main/jniLibs/arm64-v8a/",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    color = if (isNativeLoaded) StudioGreen else StudioCyan,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isNativeLoaded) "C++ JNI" else "READY",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Binary File Specs Card
        Surface(
            color = StudioObsidianDark,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Effect: ${effect.name}", color = KorvaVioletLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("ID: ${effect.effectId}", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("File Size: ${binaryBytes.size} bytes", color = TextPrimary, fontSize = 11.sp)
                    Text("Emitters: ${effect.emitters.size}", color = TextPrimary, fontSize = 11.sp)
                    Text("Duration: ${effect.duration}s", color = TextPrimary, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Magic Header: KORV", color = StudioGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("End Marker: 0xDEADBEEF", color = StudioGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = validation.message,
                    color = if (validation.isValid) StudioGreen else StudioRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Custom Export Path Selection
        ExportDestinationSelector(
            context = context,
            defaultFilename = defaultEffectName,
            extension = "korv",
            mimeType = "application/octet-stream",
            onPathExport = { dir, filename ->
                viewModel.saveKorvFileToPath(dir, filename)
            },
            onUriExport = { uri ->
                viewModel.exportBytesToUri(context, uri, binaryBytes)
            }
        )

        // Copy Base64 Button
        OutlinedButton(
            onClick = {
                val base64 = android.util.Base64.encodeToString(binaryBytes, android.util.Base64.NO_WRAP)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(".korv Base64", base64))
                copiedHex = true
            },
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copiedHex) "Copied Base64!" else "Copy .korv Binary as Base64 String", fontSize = 11.sp)
        }

        // Hex Dump View
        Text(
            text = "Binary Stream Hex Inspection (.korv v1.9.5):",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Surface(
            color = StudioObsidianDark,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            Text(
                text = hexDump,
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
