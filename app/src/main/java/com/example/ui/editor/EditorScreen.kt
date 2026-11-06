package com.example.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ColorGradeSettings
import com.example.ui.editor.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(state.projectTitle) }

    BackHandler {
        if (state.showApiKeyDialog) {
            viewModel.setShowApiKeyDialog(false)
        } else if (state.activeSheet != EditorSheet.NONE) {
            viewModel.setActiveSheet(EditorSheet.NONE)
        } else if (state.pendingPreviewPlan != null) {
            viewModel.rejectPreviewPlan()
        } else {
            onNavigateBack()
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.importVideoUri(it) }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                title = state.projectTitle,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                hasCustomApiKey = state.customAiConfig.apiKey.isNotBlank(),
                onBack = onNavigateBack,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onOpenRename = {
                    renameInput = state.projectTitle
                    showRenameDialog = true
                },
                onOpenApiKey = { viewModel.setShowApiKeyDialog(true) },
                onOpenAiPrompt = { viewModel.setActiveSheet(EditorSheet.AI_PROMPT) },
                onOpenExport = { viewModel.setActiveSheet(EditorSheet.EXPORT) }
            )
        },
        containerColor = VibeBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive AI Live Preview Approval Banner (Shows when user is previewing AI edits)
            state.pendingPreviewPlan?.let { pendingPlan ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14242B)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("live_preview_banner")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI LIVE PREVIEW: KYA YE EDIT APPLY KARNI HAI?",
                                    color = EmeraldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Before / After Toggle Chip
                            FilterChip(
                                selected = state.isShowingOriginalBeforeAfter,
                                onClick = { viewModel.toggleBeforeAfterComparison(!state.isShowingOriginalBeforeAfter) },
                                label = {
                                    Text(
                                        if (state.isShowingOriginalBeforeAfter) "Showing: Original" else "Compare: Before/After",
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.height(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.applyEditingPlan(pendingPlan) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Haan, Apply Karein", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.setActiveSheet(EditorSheet.AI_PROMPT) },
                                border = ButtonDefaults.outlinedButtonBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(VioletLight)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = VioletLight, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modify / Ask AI", color = VioletLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { viewModel.rejectPreviewPlan() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF451A1A), RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 1. Video Player Area (Supports Before/After Original comparison)
            VideoPreviewPlayer(
                clips = state.clips,
                currentPositionMs = state.currentPositionMs,
                totalDurationMs = viewModel.totalDurationMs,
                isPlaying = state.isPlaying,
                colorGrade = if (state.isShowingOriginalBeforeAfter) ColorGradeSettings() else state.colorGrade,
                textOverlays = if (state.isShowingOriginalBeforeAfter) emptyList() else state.textOverlays,
                motionGraphics = if (state.isShowingOriginalBeforeAfter) emptyList() else state.motionGraphics,
                transitions = if (state.isShowingOriginalBeforeAfter) emptyList() else state.transitions,
                onTogglePlay = { viewModel.setPlaying(!state.isPlaying) },
                onSeek = { viewModel.seekTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // 2. Main Tool Selector Row
            EditorToolBar(
                activeSheet = state.activeSheet,
                hasCustomApiKey = state.customAiConfig.apiKey.isNotBlank(),
                onSelectSheet = { viewModel.setActiveSheet(it) },
                onOpenApiKey = { viewModel.setShowApiKeyDialog(true) },
                onAddMedia = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )

            // 3. Interactive Multi-Track Timeline
            TimelineTrackView(
                clips = state.clips,
                audioTracks = state.audioTracks,
                textOverlays = state.textOverlays,
                motionGraphics = state.motionGraphics,
                transitions = state.transitions,
                currentPositionMs = state.currentPositionMs,
                totalDurationMs = viewModel.totalDurationMs,
                selectedClipId = state.selectedClipId,
                zoomFactor = state.timelineZoom,
                onSeek = { viewModel.seekTo(it) },
                onSelectClip = { viewModel.selectClip(it) },
                onSplit = { viewModel.splitClipAtPlayhead() },
                onDeleteClip = { viewModel.deleteSelectedClip() },
                onUpdateClipSpeed = { clipId, speed -> viewModel.updateClipSpeed(clipId, speed) },
                onTrimClip = { clipId, startMs, endMs -> viewModel.trimClip(clipId, startMs, endMs) },
                onZoomChange = { viewModel.setTimelineZoom(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Active Bottom Sheet Overlays
        when (state.activeSheet) {
            EditorSheet.AI_PROMPT -> {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setActiveSheet(EditorSheet.NONE) },
                    containerColor = VibeSurface,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    AiPromptPanel(
                        chatMessages = state.aiChatMessages,
                        isLoading = state.isAiLoading,
                        errorMessage = state.aiErrorMessage,
                        latestPlan = state.latestPlan,
                        hasCustomApiKey = state.customAiConfig.apiKey.isNotBlank(),
                        activeProviderLabel = if (state.customAiConfig.apiKey.isNotBlank()) state.customAiConfig.modelName else "Built-in Gemini",
                        onOpenApiKeyDialog = { viewModel.setShowApiKeyDialog(true) },
                        onSendPrompt = { prompt, url -> viewModel.sendAiPrompt(prompt, url) },
                        onPreviewPlan = { viewModel.previewEditingPlan(it) },
                        onApplyPlan = { viewModel.applyEditingPlan(it) },
                        onClose = { viewModel.setActiveSheet(EditorSheet.NONE) }
                    )
                }
            }
            EditorSheet.COLOR_GRADE -> {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setActiveSheet(EditorSheet.NONE) },
                    containerColor = VibeSurface
                ) {
                    ColorGradingSheet(
                        colorGrade = state.colorGrade,
                        onUpdateGrade = { viewModel.updateColorGrade(it) },
                        onClose = { viewModel.setActiveSheet(EditorSheet.NONE) }
                    )
                }
            }
            EditorSheet.MOTION_GRAPHICS -> {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setActiveSheet(EditorSheet.NONE) },
                    containerColor = VibeSurface
                ) {
                    MotionGraphicsSheet(
                        activeGraphics = state.motionGraphics,
                        currentPlayheadMs = state.currentPositionMs,
                        onAddGraphic = { viewModel.addMotionGraphic(it) },
                        onRemoveGraphic = { viewModel.removeMotionGraphic(it) },
                        onClose = { viewModel.setActiveSheet(EditorSheet.NONE) }
                    )
                }
            }
            EditorSheet.SUBTITLES -> {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setActiveSheet(EditorSheet.NONE) },
                    containerColor = VibeSurface
                ) {
                    SubtitlesSheet(
                        activeCaptions = state.textOverlays,
                        currentPlayheadMs = state.currentPositionMs,
                        onAddCaption = { viewModel.addTextOverlay(it) },
                        onRemoveCaption = { viewModel.removeTextOverlay(it) },
                        onClose = { viewModel.setActiveSheet(EditorSheet.NONE) }
                    )
                }
            }
            EditorSheet.AUDIO -> {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setActiveSheet(EditorSheet.NONE) },
                    containerColor = VibeSurface
                ) {
                    AudioDesignSheet(
                        audioTracks = state.audioTracks,
                        currentPlayheadMs = state.currentPositionMs,
                        onPlaySfx = { viewModel.playSfx(it) },
                        onAddTrack = { viewModel.addAudioTrack(it) },
                        onClose = { viewModel.setActiveSheet(EditorSheet.NONE) }
                    )
                }
            }
            EditorSheet.TRANSITIONS -> {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setActiveSheet(EditorSheet.NONE) },
                    containerColor = VibeSurface
                ) {
                    TransitionsSheet(
                        selectedClipId = state.selectedClipId ?: state.clips.firstOrNull()?.id,
                        onApplyTransition = { trType ->
                            val clipId = state.selectedClipId ?: state.clips.firstOrNull()?.id ?: ""
                            viewModel.setTransitionForClip(clipId, trType)
                            viewModel.setActiveSheet(EditorSheet.NONE)
                        },
                        onClose = { viewModel.setActiveSheet(EditorSheet.NONE) }
                    )
                }
            }
            EditorSheet.EXPORT -> {
                ExportDialog(
                    exportProgress = state.exportProgress,
                    onStartExport = { viewModel.startExport(it) },
                    onCancelExport = { viewModel.cancelExport() },
                    onDismiss = {
                        viewModel.dismissExport()
                        viewModel.setActiveSheet(EditorSheet.NONE)
                    }
                )
            }
            else -> {}
        }

        // Custom API Key Popup Dialog
        if (state.showApiKeyDialog) {
            ApiKeyDialog(
                initialConfig = state.customAiConfig,
                onSave = { config -> viewModel.saveCustomAiConfig(config) },
                onDismiss = { viewModel.setShowApiKeyDialog(false) }
            )
        }

        // Analysis Progress Dialog
        state.analysisProgress?.let { prog ->
            if (!prog.isComplete) {
                AlertDialog(
                    onDismissRequest = {},
                    containerColor = VibeSurface,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = EmeraldLight,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyzing Video Footage", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            LinearProgressIndicator(
                                progress = { prog.percentage / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = EmeraldLight,
                                trackColor = VibeBorder
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${prog.percentage}% • ${prog.stage}",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                            if (prog.detectedScenes > 0) {
                                Text(
                                    text = "Scenes Identified: ${prog.detectedScenes}",
                                    color = EmeraldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }

        // Rename Project Dialog
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                containerColor = VibeSurface,
                shape = RoundedCornerShape(16.dp),
                title = { Text("Rename Project", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldLight,
                            unfocusedBorderColor = VibeBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameInput.isNotBlank()) {
                                viewModel.renameProject(renameInput)
                            }
                            showRenameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun EditorTopBar(
    title: String,
    canUndo: Boolean,
    canRedo: Boolean,
    hasCustomApiKey: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenRename: () -> Unit,
    onOpenApiKey: () -> Unit,
    onOpenAiPrompt: () -> Unit,
    onOpenExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(VibeBackground)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(34.dp).testTag("editor_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.clickable { onOpenRename() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 105.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = TextTertiary,
                        modifier = Modifier.size(12.dp).padding(start = 3.dp)
                    )
                }
                Text(
                    text = if (hasCustomApiKey) "Custom API Active" else "Autosaved",
                    color = EmeraldLight,
                    fontSize = 9.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(30.dp).testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(30.dp).testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Custom API Key Popup Button
            OutlinedButton(
                onClick = onOpenApiKey,
                border = ButtonDefaults.outlinedButtonBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (hasCustomApiKey) EmeraldLight else VioletLight
                    )
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp).testTag("editor_api_key_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Key",
                    tint = if (hasCustomApiKey) EmeraldLight else VioletLight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (hasCustomApiKey) "Key ✓" else "API Key",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onOpenAiPrompt,
                colors = ButtonDefaults.buttonColors(containerColor = VioletDark),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp).testTag("open_ai_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("AI Edit", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onOpenExport,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp).testTag("export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Export", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EditorToolBar(
    activeSheet: EditorSheet,
    hasCustomApiKey: Boolean,
    onSelectSheet: (EditorSheet) -> Unit,
    onOpenApiKey: () -> Unit,
    onAddMedia: () -> Unit
) {
    val tools = listOf(
        Triple(EditorSheet.AI_PROMPT, Icons.Default.AutoAwesome, "AI / Link"),
        Triple(EditorSheet.COLOR_GRADE, Icons.Default.ColorLens, "Color"),
        Triple(EditorSheet.MOTION_GRAPHICS, Icons.Default.Layers, "Graphics"),
        Triple(EditorSheet.SUBTITLES, Icons.Default.Subtitles, "Captions"),
        Triple(EditorSheet.AUDIO, Icons.Default.GraphicEq, "Audio/SFX"),
        Triple(EditorSheet.TRANSITIONS, Icons.Default.Shuffle, "Transitions")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibeSurface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onAddMedia() }
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("add_clip_tool")
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(VibeSurfaceElevated, CircleShape)
                    .border(1.dp, EmeraldLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Media", tint = EmeraldLight, modifier = Modifier.size(20.dp))
            }
            Text("Add", color = EmeraldLight, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }

        tools.forEach { (sheet, icon, label) ->
            val isActive = activeSheet == sheet
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelectSheet(if (isActive) EditorSheet.NONE else sheet) }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (isActive) EmeraldDark else VibeSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, if (isActive) EmeraldLight else VibeBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isActive) Color.White else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = label,
                    color = if (isActive) EmeraldLight else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Quick API Key Tool Button on Toolbar as well
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onOpenApiKey() }
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("toolbar_api_key_button")
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(if (hasCustomApiKey) EmeraldDark else VioletDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .border(1.dp, if (hasCustomApiKey) EmeraldLight else VioletLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Key",
                    tint = if (hasCustomApiKey) EmeraldLight else VioletLight,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = "API Key",
                color = if (hasCustomApiKey) EmeraldLight else VioletLight,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
