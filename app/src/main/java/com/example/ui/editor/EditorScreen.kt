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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // System BackHandler mandated by guidelines for sub-screens
    BackHandler {
        if (state.activeSheet != EditorSheet.NONE) {
            viewModel.setActiveSheet(EditorSheet.NONE)
        } else {
            onNavigateBack()
        }
    }

    // Photo picker for importing additional clips directly in editor
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
                onBack = onNavigateBack,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onOpenRename = {
                    renameInput = state.projectTitle
                    showRenameDialog = true
                },
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
            // 1. Video Player Area
            VideoPreviewPlayer(
                clips = state.clips,
                currentPositionMs = state.currentPositionMs,
                totalDurationMs = viewModel.totalDurationMs,
                isPlaying = state.isPlaying,
                colorGrade = state.colorGrade,
                textOverlays = state.textOverlays,
                motionGraphics = state.motionGraphics,
                transitions = state.transitions,
                onTogglePlay = { viewModel.setPlaying(!state.isPlaying) },
                onSeek = { viewModel.seekTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // 2. Main Tool Selector Row
            EditorToolBar(
                activeSheet = state.activeSheet,
                onSelectSheet = { viewModel.setActiveSheet(it) },
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
                        onSendPrompt = { viewModel.sendAiPrompt(it) },
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
                        selectedClipId = state.selectedClipId,
                        onApplyTransition = { trType ->
                            state.selectedClipId?.let { clipId ->
                                viewModel.setTransitionForClip(clipId, trType)
                            }
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
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenRename: () -> Unit,
    onOpenAiPrompt: () -> Unit,
    onOpenExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(VibeBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = TextTertiary,
                        modifier = Modifier.size(12.dp).padding(start = 4.dp)
                    )
                }
                Text("Autosaved", color = EmeraldLight, fontSize = 9.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Undo / Redo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(34.dp).testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(34.dp).testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // AI Brain Button
            Button(
                onClick = onOpenAiPrompt,
                colors = ButtonDefaults.buttonColors(containerColor = VioletDark),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("open_ai_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Edit", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Export Button
            Button(
                onClick = onOpenExport,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EditorToolBar(
    activeSheet: EditorSheet,
    onSelectSheet: (EditorSheet) -> Unit,
    onAddMedia: () -> Unit
) {
    val tools = listOf(
        Triple(EditorSheet.AI_PROMPT, Icons.Default.AutoAwesome, "AI Prompt"),
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add Media (+) Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onAddMedia() }
                .padding(horizontal = 6.dp, vertical = 4.dp)
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
                    .padding(horizontal = 6.dp, vertical = 4.dp)
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
    }
}
