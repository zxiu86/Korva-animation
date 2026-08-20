package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SampleProjects
import com.example.model.KorProject
import com.example.model.ResolutionPreset
import com.example.model.ShapeKind
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel

@Composable
fun KorvaHomeScreen(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val currentProject by viewModel.project.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidianDark)
    ) {
        // Landscape 2-Column Split Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT COLUMN: Instructions & Quick Guide (تعليمات الاستخدام)
            Surface(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
                color = StudioPanelDark,
                tonalElevation = 2.dp
            ) {
                LeftInstructionsGuidePanel(
                    onOpenQuickSample = { sample ->
                        viewModel.loadAndOpenProject(sample)
                    }
                )
            }

            // RIGHT COLUMN: Main Actions (Create New Project / OR / Load Project)
            Surface(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
                color = StudioPanelDark,
                tonalElevation = 2.dp
            ) {
                RightMainActionsPanel(
                    currentProject = currentProject,
                    onCreateNewClicked = { showNewProjectDialog = true },
                    onLoadTemplateClicked = { project -> viewModel.loadAndOpenProject(project) },
                    onImportClicked = { showImportDialog = true },
                    onResumeClicked = { viewModel.navigateToStudio() }
                )
            }
        }

        // Create New Project Dialog
        if (showNewProjectDialog) {
            CreateNewProjectDialog(
                onDismiss = { showNewProjectDialog = false },
                onCreate = { name, res, fps, frames, shape ->
                    viewModel.createAndOpenNewProject(
                        name = name,
                        resolution = res,
                        fps = fps,
                        totalFrames = frames,
                        starterShape = shape
                    )
                    showNewProjectDialog = false
                }
            )
        }

        // Import Project Dialog
        if (showImportDialog) {
            ImportProjectDialog(
                onDismiss = { showImportDialog = false },
                onImport = { json ->
                    val success = viewModel.importKorJsonString(json)
                    if (success) {
                        viewModel.navigateToStudio()
                        showImportDialog = false
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// LEFT COLUMN: Usage Instructions & Animation Guide (تعليمات الاستخدام)
// -------------------------------------------------------------
@Composable
private fun LeftInstructionsGuidePanel(
    onOpenQuickSample: (KorProject) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KorvaVioletPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Instructions",
                    tint = KorvaVioletLight,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "تعليمات الاستخدام",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "دليل البدء والتحريك لمحركات الألعاب 2D",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(vertical = 2.dp))

        // Step 1: Layers & Sprites
        InstructionCard(
            stepNumber = "1",
            title = "إضافة الأشكال والأسلحة (Sprites & Layers)",
            description = "اختر كائنات جاهزة للألعاب (سيوف، دروع، وحوش، عملات، وتأثيرات FX) أو استورد صورك بصيغة PNG/JPG من جهازك.",
            icon = Icons.Default.Category,
            accentColor = StudioCyan
        )

        // Step 2: Timeline & Keyframes
        InstructionCard(
            stepNumber = "2",
            title = "تسجيل الكي فريم (Keyframes & Timeline)",
            description = "حرّك مؤشر الإطار على الشريط الزمني، ثم حرّك أو دوّر العنصر في لوحة الرسم لتسجيل الإطار المفتاحي تلقائياً أو اضغط زر +KF.",
            icon = Icons.Default.LinearScale,
            accentColor = StudioOrange
        )

        // Step 3: Easing & Physics
        InstructionCard(
            stepNumber = "3",
            title = "منحنيات التسارع (Easing & Acceleration)",
            description = "اختر منحنيات الحركة مثل Bounce للقفز، و Elastic للمرونة، و Anticipation للتراجع المسبق قبل الضربات لتوليد فيزياء حركية واقعية.",
            icon = Icons.Default.ShowChart,
            accentColor = KorvaVioletLight
        )

        // Step 4: Onion Skinning
        InstructionCard(
            stepNumber = "4",
            title = "قشرة البصل (Onion Skinning)",
            description = "شاهد شبح الإطارات السابقة (بالأزرق) واللاحقة (بالبرتقالي) أثناء الرسم لضبط انسيابية الحركة الإطار تلو الآخر.",
            icon = Icons.Default.Layers,
            accentColor = StudioYellow
        )

        // Step 5: Exporting to Game Engines
        InstructionCard(
            stepNumber = "5",
            title = "تصدير الألعاب (Sprite Sheet & Engine Export)",
            description = "صدّر الحركة كلوحة إطارات شفافة Sprite Sheet PNG أو ملفات .kor أو كود حركي لمحركي Godot 4 و Unity 2D.",
            icon = Icons.Default.FileDownload,
            accentColor = StudioGreen
        )

        // Quick Tip Footer Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(StudioSurfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, StudioBorderLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Tip",
                    tint = StudioYellow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "نصيحة: يمكنك التنقل بالسحب والتقريب (Zoom & Pan) في مساحة الرسم لتعديل التفاصيل الدقيقة.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun InstructionCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Number / Icon Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$stepNumber.",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// RIGHT COLUMN: Main Actions (New Project / OR / Load Project)
// -------------------------------------------------------------
@Composable
private fun RightMainActionsPanel(
    currentProject: KorProject,
    onCreateNewClicked: () -> Unit,
    onLoadTemplateClicked: (KorProject) -> Unit,
    onImportClicked: () -> Unit,
    onResumeClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Studio Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "KORVA",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "2D ANIMATOR",
                            color = KorvaVioletLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(KorvaVioletPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "استوديو تحريك الألعاب ثنائية الأبعاد",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Resume Button if project loaded
            Button(
                onClick = onResumeClicked,
                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("home_resume_studio_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("فتح الاستوديو", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = StudioBorder)

        // ==========================================
        // OPTION 1: إنشاء مشروع جديد (Create New Project)
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCreateNewClicked() }
                .border(1.5.dp, KorvaVioletPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .testTag("home_create_new_project_card"),
            color = StudioSurfaceDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(KorvaVioletPrimary, KorvaVioletDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Project",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "إنشاء مشروع جديد",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "بدء أنيميشن فارغ، تخصيص الدقة (440p إلى 2K)، وعدد الإطارات وFPS",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onCreateNewClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("مشروع جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }

        // ==========================================
        // OR SEPARATOR: أو
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = StudioBorder)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurfaceVariant)
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "أو",
                    color = KorvaVioletLight,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
            HorizontalDivider(modifier = Modifier.weight(1f), color = StudioBorder)
        }

        // ==========================================
        // OPTION 2: تحميل مشروع (Load / Open Project)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "تحميل مشروع أو قوالب ألعاب جاهزة",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Import .kor file button
                OutlinedButton(
                    onClick = onImportClicked,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("home_import_kor_button")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("استيراد ملف .kor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Ready Templates List
            val sampleProjects = remember { SampleProjects.getSamples() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sampleProjects.forEach { sample ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLoadTemplateClicked(sample) }
                            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                            .testTag("home_template_${sample.name.replace(" ", "_")}"),
                        color = StudioSurfaceDark
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (icon, iconColor) = when {
                                    sample.name.contains("Knight", ignoreCase = true) -> Icons.Default.Shield to KorvaVioletLight
                                    sample.name.contains("Slime", ignoreCase = true) -> Icons.Default.Pets to StudioGreen
                                    sample.name.contains("Coin", ignoreCase = true) -> Icons.Default.MonetizationOn to StudioYellow
                                    else -> Icons.Default.FlashOn to StudioOrange
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(iconColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "${sample.totalFrames} F",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = sample.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${sample.layers.size} طبقات • ${sample.fps} FPS",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "تحميل",
                                    color = StudioCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = StudioCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CREATE NEW PROJECT DIALOG
// -------------------------------------------------------------
@Composable
private fun CreateNewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, res: ResolutionPreset, fps: Int, frames: Int, starterShape: ShapeKind) -> Unit
) {
    var projectName by remember { mutableStateOf("Game Hero Animation") }
    var selectedRes by remember { mutableStateOf(ResolutionPreset.RES_720P) }
    var selectedFps by remember { mutableStateOf(24) }
    var selectedFrames by remember { mutableStateOf(32) }
    var selectedShape by remember { mutableStateOf(ShapeKind.SWORD) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioPanelDark
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = KorvaVioletPrimary, modifier = Modifier.size(22.dp))
                        Text(
                            text = "إنشاء مشروع أنيميشن جديد",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = StudioBorder)

                // Project Name Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("اسم المشروع", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_project_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KorvaVioletPrimary,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = StudioSurfaceDark,
                            unfocusedContainerColor = StudioSurfaceDark
                        )
                    )
                }

                // Resolution Preset Selection (440p up to 2K)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("دقة الكانفاس (Resolution)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    val presets = listOf(
                        ResolutionPreset.RES_RETRO,
                        ResolutionPreset.RES_440P,
                        ResolutionPreset.RES_720P,
                        ResolutionPreset.RES_1080P,
                        ResolutionPreset.RES_2K
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = selectedRes == preset
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedRes = preset }
                                    .border(
                                        1.5.dp,
                                        if (isSelected) KorvaVioletPrimary else StudioBorder,
                                        RoundedCornerShape(8.dp)
                                    ),
                                color = if (isSelected) KorvaVioletPrimary.copy(alpha = 0.15f) else StudioSurfaceDark
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = preset.label,
                                        color = if (isSelected) KorvaVioletLight else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${preset.width}x${preset.height}",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // FPS & Total Frames Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // FPS
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("معدل الإطارات (FPS)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(12, 24, 30, 60).forEach { fpsVal ->
                                val isSel = selectedFps == fpsVal
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedFps = fpsVal }
                                        .border(1.dp, if (isSel) StudioCyan else StudioBorder, RoundedCornerShape(6.dp)),
                                    color = if (isSel) StudioCyan.copy(alpha = 0.2f) else StudioSurfaceDark
                                ) {
                                    Text(
                                        text = "$fpsVal",
                                        color = if (isSel) StudioCyan else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Total Frames
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("عدد الإطارات (Total Frames)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(16, 24, 32, 48).forEach { fCount ->
                                val isSel = selectedFrames == fCount
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedFrames = fCount }
                                        .border(1.dp, if (isSel) StudioOrange else StudioBorder, RoundedCornerShape(6.dp)),
                                    color = if (isSel) StudioOrange.copy(alpha = 0.2f) else StudioSurfaceDark
                                ) {
                                    Text(
                                        text = "$fCount",
                                        color = if (isSel) StudioOrange else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Starter Shape
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("عنصر البداية (Starter Sprite)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(ShapeKind.SWORD, ShapeKind.SLIME, ShapeKind.COIN, ShapeKind.SLASH_FX).forEach { shape ->
                            val isSel = selectedShape == shape
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedShape = shape }
                                    .border(1.dp, if (isSel) KorvaVioletLight else StudioBorder, RoundedCornerShape(8.dp)),
                                color = if (isSel) KorvaVioletPrimary.copy(alpha = 0.2f) else StudioSurfaceDark
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = shape.displayName,
                                        color = if (isSel) TextPrimary else TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            onCreate(projectName, selectedRes, selectedFps, selectedFrames, selectedShape)
                        },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("submit_create_project_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary)
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("بدء المشروع في الاستوديو", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// IMPORT PROJECT DIALOG
// -------------------------------------------------------------
@Composable
private fun ImportProjectDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioPanelDark
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = StudioCyan)
                        Text(
                            text = "استيراد مشروع Korva (.kor)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "الصق محتوى كود ملف .kor أو JSON هنا لفتحه مباشرة في الاستوديو:",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("import_json_input"),
                    placeholder = { Text("{\n  \"version\": 1,\n  \"name\": \"Project\", ...\n}", color = TextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = StudioSurfaceDark,
                        unfocusedContainerColor = StudioSurfaceDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إلغاء", color = TextSecondary)
                    }

                    Button(
                        onClick = { onImport(jsonText) },
                        enabled = jsonText.isNotBlank(),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_import_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تحميل المشروع", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
