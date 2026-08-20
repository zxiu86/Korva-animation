package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.KorvaViewModel
import kotlinx.coroutines.delay

@Composable
fun KorvaStudioScreen(
    viewModel: KorvaViewModel,
    modifier: Modifier = Modifier
) {
    val isTopBarCollapsed by viewModel.isTopBarCollapsed.collectAsState()
    val showSpriteLibrary by viewModel.showSpriteLibrary.collectAsState()
    val showEasingDialog by viewModel.showEasingDialog.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val showProjectDialog by viewModel.showProjectDialog.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Auto-dismiss status toast after 2.5s
    var currentToast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            currentToast = statusMessage
            delay(2500)
            currentToast = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidianDark)
    ) {
        // Main Workspace Structure: Left Tools | Center (Header + Canvas + Timeline) | Right Inspector
        Row(modifier = Modifier.fillMaxSize()) {
            // 1. Left Tools & Shapes Panel (spans full height on the left)
            LeftToolsPanel(
                viewModel = viewModel
            )

            // 2. Center Column (Header, Canvas Viewport, and Central Timeline filling the remaining space)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Collapsible Studio Top Bar Header
                AnimatedVisibility(
                    visible = !isTopBarCollapsed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StudioTopBar(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Sleek collapsed header strip when top bar is closed
                if (isTopBarCollapsed) {
                    CollapsedHeaderStrip(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Main Stage / Canvas Viewport (takes remaining center vertical space)
                CanvasViewport(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Central Multi-Track Timeline (directly in center with full width)
                BottomTimelinePanel(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Right Inspector & Layers Side Panel (Collapsible with Properties & Layers tabs)
            RightInspectorPanel(
                viewModel = viewModel
            )
        }

        // Status Toast Notification Pill
        AnimatedVisibility(
            visible = currentToast != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 130.dp)
        ) {
            Surface(
                color = StudioSurfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KorvaVioletPrimary.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = currentToast ?: "",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Dialogs
        if (showSpriteLibrary) {
            SpriteLibraryDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openSpriteLibrary(false) }
            )
        }

        if (showEasingDialog) {
            EasingGraphDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openEasingDialog(false) }
            )
        }

        if (showExportDialog) {
            ExportDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openExportDialog(false) }
            )
        }

        if (showProjectDialog) {
            ProjectManagerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openProjectDialog(false) }
            )
        }
    }
}
