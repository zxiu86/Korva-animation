package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 1. TOP RESPONSIVE HEADER BAR
            HomeTopHeaderBar(
                currentProject = currentProject,
                onResumeClicked = { viewModel.navigateToStudio() },
                onImportClicked = { showImportDialog = true }
            )

            Spacer(Modifier.height(10.dp))

            // 2. DYNAMIC CONTENT AREA (ADAPTS TO ANY SCREEN SIZE & ASPECT RATIO)
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isWide = maxWidth >= 680.dp

                if (isWide) {
                    // Wide / Landscape Layout: Dual-Column Split
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // LEFT PANEL: Interactive Guides & Tips (42%)
                        Surface(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp)),
                            color = StudioPanelDark,
                            tonalElevation = 2.dp
                        ) {
                            LeftInstructionsGuidePanel(
                                onOpenQuickSample = { sample ->
                                    viewModel.loadAndOpenProject(sample)
                                }
                            )
                        }

                        // RIGHT PANEL: Actions & Templates Gallery (58%)
                        Surface(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp)),
                            color = StudioPanelDark,
                            tonalElevation = 2.dp
                        ) {
                            RightMainActionsPanel(
                                currentProject = currentProject,
                                onCreateNewClicked = { showNewProjectDialog = true },
                                onLoadTemplateClicked = { project -> viewModel.loadAndOpenProject(project) },
                                onImportClicked = { showImportDialog = true },
                                onQuickPreset = { res, fps, frames ->
                                    viewModel.createAndOpenNewProject(
                                        name = "Quick ${res.label}",
                                        resolution = res,
                                        fps = fps,
                                        totalFrames = frames,
                                        starterShape = ShapeKind.SWORD
                                    )
                                }
                            )
                        }
                    }
                } else {
                    // Compact Layout (Phone / Small Screens): Interactive Tabbed View
                    CompactHomeTabView(
                        currentProject = currentProject,
                        onCreateNewClicked = { showNewProjectDialog = true },
                        onLoadTemplateClicked = { project -> viewModel.loadAndOpenProject(project) },
                        onImportClicked = { showImportDialog = true },
                        onOpenQuickSample = { sample -> viewModel.loadAndOpenProject(sample) }
                    )
                }
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

// =============================================================================
// 1. TOP HEADER BAR
// =============================================================================
@Composable
private fun HomeTopHeaderBar(
    currentProject: KorProject,
    onResumeClicked: () -> Unit,
    onImportClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioPanelDark)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity & Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(KorvaVioletPrimary, StudioCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Animation,
                    contentDescription = "Korva 2D",
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
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.2.sp
                    )
                    Surface(
                        color = KorvaVioletPrimary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, KorvaVioletLight.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "2D STUDIO PRO",
                            color = KorvaVioletLight,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
                Text(
                    text = "استوديو تحريك وتصميم الشخصيات والألعاب ثنائية الأبعاد",
                    color = TextSecondary,
                    fontSize = 10.5.sp
                )
            }
        }

        // Active Project Info & Quick Jump
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Import button
            OutlinedButton(
                onClick = onImportClicked,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StudioBorderLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp).testTag("home_import_kor_button")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("استيراد .kor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            // Resume Studio button
            Button(
                onClick = onResumeClicked,
                colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp).testTag("home_resume_studio_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("دخول الاستوديو", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =============================================================================
// 2. LEFT PANEL: INTERACTIVE GUIDES & GAME ENGINE DOCS
// =============================================================================
@Composable
private fun LeftInstructionsGuidePanel(
    onOpenQuickSample: (KorProject) -> Unit
) {
    var selectedGuideTab by remember { mutableStateOf(0) }
    val guideTabs = listOf("خطوات التحريك", "أسرار الألعاب", "التصدير")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = KorvaVioletLight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "دليل الاستخدام والتحريك",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }

            // Mini Tabs
            Surface(
                color = StudioSurfaceDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, StudioBorder)
            ) {
                Row(modifier = Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    guideTabs.forEachIndexed { index, title ->
                        val isSel = selectedGuideTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) KorvaVioletPrimary.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable { selectedGuideTab = index }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (isSel) KorvaVioletLight else TextMuted,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(vertical = 8.dp))

        // Tab Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedGuideTab) {
                0 -> {
                    // Core Animation Workflow
                    InstructionCard(
                        stepNumber = "1",
                        title = "الطبقات والكائنات (Layers & Sprites)",
                        description = "أضف أسلحة، وحوش، وتأثيرات ضربات FX أو استورد صورك PNG من جهازك ورتب الطبقات بحرية.",
                        icon = Icons.Default.Category,
                        accentColor = StudioCyan
                    )
                    InstructionCard(
                        stepNumber = "2",
                        title = "الكي فريم (Keyframes & Timeline)",
                        description = "حرّك مؤشر الوقت الأزرق ثم حرّك أو دوّر الكائن على الكانفاس لتسجيل الإطار أو اضغط زر +KF.",
                        icon = Icons.Default.LinearScale,
                        accentColor = StudioOrange
                    )
                    InstructionCard(
                        stepNumber = "3",
                        title = "منحنيات التسارع (Easing Curves)",
                        description = "استخدم Bounce للارتداد، Elastic للمرونة، و Anticipation للتراجع الحركي قبل الضربات القوية.",
                        icon = Icons.Default.ShowChart,
                        accentColor = KorvaVioletLight
                    )
                    InstructionCard(
                        stepNumber = "4",
                        title = "قشرة البصل (Onion Skinning)",
                        description = "شاهد شبح الإطارات السابقة (أزرق) واللاحقة (برتقالي) لرسم ومطابقة انسيابية الحركة.",
                        icon = Icons.Default.Layers,
                        accentColor = StudioYellow
                    )
                }
                1 -> {
                    // Game Physics & Animation Secrets
                    GameDevTipCard(
                        title = "قاعدة الـ 12 مبدأ للتحريك",
                        description = "احرص على المبالغة (Exaggeration) في تمدد وانضغاط الكائنات (Squash & Stretch) عند الهبوط.",
                        icon = Icons.Default.AutoFixHigh,
                        accentColor = StudioGreen
                    )
                    GameDevTipCard(
                        title = "نقطة الارتكاز (Pivot Point)",
                        description = "ضع نقطة ارتكاز السيف عند المقبض ومفصل الركبة عند المنتصف لتدوير طبيعي وواقعي.",
                        icon = Icons.Default.CenterFocusStrong,
                        accentColor = StudioOrange
                    )
                    GameDevTipCard(
                        title = "تكرار الحركات (Loop Cycles)",
                        description = "تأكد من أن فريم البداية والنهاية متطابقان تماماً لحركات المشي (Walk Cycle) والوقوف الخامل (Idle).",
                        icon = Icons.Default.Repeat,
                        accentColor = StudioCyan
                    )
                }
                2 -> {
                    // Engine Exports
                    GameDevTipCard(
                        title = "تصدير لوحة الإطارات (Sprite Sheet)",
                        description = "قم بتصدير الحركة كـ PNG شفاف جاهز للاستيراد الفوري في Unity 2D و Godot 4 و Unreal Engine.",
                        icon = Icons.Default.GridOn,
                        accentColor = KorvaVioletLight
                    )
                    GameDevTipCard(
                        title = "ملفات المشروع (.kor)",
                        description = "احفظ مشاريعك بتنسيق .kor لمشاركتها أو استئناف العمل عليها على أي جهاز آخر بنقرة واحدة.",
                        icon = Icons.Default.Code,
                        accentColor = StudioCyan
                    )
                    GameDevTipCard(
                        title = "تصدير كود الحركة (Code Snippet)",
                        description = "انسخ كود الرسوم المتحركة الجاهز لـ GDScript أو C# لتطبيق الحركة برمجياً في لعبتك.",
                        icon = Icons.Default.Terminal,
                        accentColor = StudioYellow
                    )
                }
            }
        }
    }
}

// =============================================================================
// 3. RIGHT PANEL: MAIN ACTIONS & GAME TEMPLATES GALLERY
// =============================================================================
@Composable
private fun RightMainActionsPanel(
    currentProject: KorProject,
    onCreateNewClicked: () -> Unit,
    onLoadTemplateClicked: (KorProject) -> Unit,
    onImportClicked: () -> Unit,
    onQuickPreset: (ResolutionPreset, Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =========================================================
        // A. HERO PRIMARY ACTION CARD: CREATE NEW PROJECT
        // =========================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCreateNewClicked() }
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(KorvaVioletPrimary, StudioCyan)
                        )
                    ),
                    RoundedCornerShape(12.dp)
                )
                .testTag("home_create_new_project_card"),
            color = StudioSurfaceDark
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                                contentDescription = "Create",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "إنشاء مشروع أنيميشن جديد",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                            Text(
                                text = "تخصيص الكانفاس، الدقة (Retro 240p إلى 2K)، ومعدل الفريمات",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = onCreateNewClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = KorvaVioletPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("مشروع جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                    }
                }

                // Quick Launch Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بدء سريع:", color = TextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)

                    QuickPresetChip(label = "🎮 Retro Pixel (12 FPS)", onClick = { onQuickPreset(ResolutionPreset.RES_RETRO, 12, 24) })
                    QuickPresetChip(label = "⚡ 720p HD (24 FPS)", onClick = { onQuickPreset(ResolutionPreset.RES_720P, 24, 32) })
                    QuickPresetChip(label = "🚀 1080p Smooth (60 FPS)", onClick = { onQuickPreset(ResolutionPreset.RES_1080P, 60, 60) })
                }
            }
        }

        // =========================================================
        // B. OR SEPARATOR
        // =========================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = StudioBorder)
            Text(
                text = "أو اختر قالباً تفاعلياً جاهزاً",
                color = KorvaVioletLight,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = StudioBorder)
        }

        // =========================================================
        // C. READY GAME TEMPLATES CARDS (GRID)
        // =========================================================
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
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val (icon, iconColor, badgeText) = when {
                            sample.name.contains("Knight", ignoreCase = true) -> Triple(Icons.Default.Shield, KorvaVioletLight, "هجوم البطل")
                            sample.name.contains("Slime", ignoreCase = true) -> Triple(Icons.Default.Pets, StudioGreen, "قفز الوحش")
                            sample.name.contains("Coin", ignoreCase = true) -> Triple(Icons.Default.MonetizationOn, StudioYellow, "دوران عملة")
                            else -> Triple(Icons.Default.FlashOn, StudioOrange, "تأثير سحري")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(iconColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
                            }

                            Surface(
                                color = StudioSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${sample.totalFrames} F",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = sample.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${sample.layers.size} طبقات • ${sample.fps} FPS",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = badgeText,
                                color = iconColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
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

// =============================================================================
// 4. COMPACT TABBED VIEW FOR SMALLER SCREENS / PHONES
// =============================================================================
@Composable
private fun CompactHomeTabView(
    currentProject: KorProject,
    onCreateNewClicked: () -> Unit,
    onLoadTemplateClicked: (KorProject) -> Unit,
    onImportClicked: () -> Unit,
    onOpenQuickSample: (KorProject) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioPanelDark,
            contentColor = KorvaVioletLight,
            divider = { HorizontalDivider(color = StudioBorder) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("المشاريع والقوالب", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("دليل التحريك والتلميحات", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(StudioPanelDark)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
        ) {
            if (selectedTab == 0) {
                RightMainActionsPanel(
                    currentProject = currentProject,
                    onCreateNewClicked = onCreateNewClicked,
                    onLoadTemplateClicked = onLoadTemplateClicked,
                    onImportClicked = onImportClicked,
                    onQuickPreset = { res, fps, frames ->
                        // Quick starter
                    }
                )
            } else {
                LeftInstructionsGuidePanel(
                    onOpenQuickSample = onOpenQuickSample
                )
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTS: Instruction Card, Tip Card, Quick Chip
// =============================================================================
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
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(0.8.dp, StudioBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$stepNumber.",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GameDevTipCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurfaceDark,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(0.8.dp, StudioBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.5.sp
                )
            }
        }
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = StudioSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, StudioBorderLight),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
        )
    }
}

// =============================================================================
// CREATE NEW PROJECT DIALOG
// =============================================================================
@Composable
private fun CreateNewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, res: ResolutionPreset, fps: Int, frames: Int, starterShape: ShapeKind) -> Unit
) {
    var projectName by remember { mutableStateOf("Hero Action 2D") }
    var selectedRes by remember { mutableStateOf(ResolutionPreset.RES_720P) }
    var selectedFps by remember { mutableStateOf(24) }
    var selectedFrames by remember { mutableStateOf(32) }
    var selectedShape by remember { mutableStateOf(ShapeKind.SWORD) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp)),
            color = StudioPanelDark
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = KorvaVioletPrimary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "إنشاء مشروع أنيميشن جديد",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = StudioBorder)

                // Project Name Input
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("اسم المشروع", color = TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
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

                // Resolution Preset Selection (Retro up to 2K)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("دقة الكانفاس (Resolution Preset)", color = TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    val presets = listOf(
                        ResolutionPreset.RES_RETRO,
                        ResolutionPreset.RES_440P,
                        ResolutionPreset.RES_720P,
                        ResolutionPreset.RES_1080P,
                        ResolutionPreset.RES_2K
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = selectedRes == preset
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedRes = preset }
                                    .border(
                                        1.dp,
                                        if (isSelected) KorvaVioletPrimary else StudioBorder,
                                        RoundedCornerShape(6.dp)
                                    ),
                                color = if (isSelected) KorvaVioletPrimary.copy(alpha = 0.2f) else StudioSurfaceDark
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = preset.label,
                                        color = if (isSelected) KorvaVioletLight else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "${preset.width}x${preset.height}",
                                        color = TextMuted,
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // FPS & Total Frames Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // FPS
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("معدل الإطارات (FPS)", color = TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
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
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Total Frames
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("عدد الإطارات (Frames)", color = TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
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
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Starter Shape
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("عنصر البداية (Starter Sprite)", color = TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(ShapeKind.SWORD, ShapeKind.SHIELD, ShapeKind.SLIME, ShapeKind.COIN, ShapeKind.SLASH_FX).forEach { shape ->
                            val isSel = selectedShape == shape
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedShape = shape }
                                    .border(1.dp, if (isSel) KorvaVioletLight else StudioBorder, RoundedCornerShape(6.dp)),
                                color = if (isSel) KorvaVioletPrimary.copy(alpha = 0.25f) else StudioSurfaceDark
                            ) {
                                Text(
                                    text = shape.displayName,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 9.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
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
                        Text("إلغاء", fontSize = 11.sp)
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
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("بدء المشروع", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// IMPORT PROJECT DIALOG
// =============================================================================
@Composable
private fun ImportProjectDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp)),
            color = StudioPanelDark
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "استيراد مشروع Korva (.kor)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = "الصق محتوى كود ملف .kor أو JSON هنا لفتحه مباشرة في الاستوديو:",
                    color = TextSecondary,
                    fontSize = 10.5.sp
                )

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("import_json_input"),
                    placeholder = { Text("{\n  \"version\": 1,\n  \"name\": \"Project\", ...\n}", color = TextMuted, fontSize = 10.sp) },
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
                        Text("إلغاء", color = TextSecondary, fontSize = 11.sp)
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
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تحميل المشروع", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
