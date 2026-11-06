package com.example.ui.editor

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiBrainRepository
import com.example.data.AppDatabase
import com.example.data.DemoProjects
import com.example.data.ProjectRepository
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "EditorViewModel"

enum class EditorSheet {
    NONE,
    AI_PROMPT,
    COLOR_GRADE,
    MOTION_GRAPHICS,
    SUBTITLES,
    AUDIO,
    TRANSITIONS,
    EXPORT,
    CLIP_TRIM
}

data class EditorUiState(
    val projectId: String = UUID.randomUUID().toString(),
    val projectTitle: String = "Untitled Vibe Edit",
    val clips: List<VideoClip> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val motionGraphics: List<MotionGraphic> = emptyList(),
    val colorGrade: ColorGradeSettings = ColorGradeSettings(),
    val transitions: List<TransitionEffect> = emptyList(),
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedClipId: String? = null,
    val timelineZoom: Float = 1.0f, // 0.5f to 3.0f
    val activeSheet: EditorSheet = EditorSheet.NONE,
    val isAiLoading: Boolean = false,
    val aiErrorMessage: String? = null,
    val aiChatMessages: List<ChatMessage> = emptyList(),
    val latestPlan: EditingPlan? = null,
    val exportProgress: ExportProgress? = null,
    val analysisProgress: AnalysisProgress? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val projectRepo = ProjectRepository(db.projectDao())
    private val aiRepo = AiBrainRepository()
    private val analyzer = VideoAnalyzer(application)
    private val exportEngine = ExportEngine(application)
    private val audioMixer = AudioMixerEngine(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Undo / Redo History
    private val undoStack = mutableListOf<EditorUiState>()
    private val redoStack = mutableListOf<EditorUiState>()

    val totalDurationMs: Long
        get() = _uiState.value.clips.sumOf { it.durationMs }.coerceAtLeast(1000L)

    init {
        // Welcome message in AI chat
        val initialChat = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "Welcome to VIBE CUT AI! Tell me how you'd like to edit your video, or tap an AI suggestion below."
            )
        )
        _uiState.value = _uiState.value.copy(aiChatMessages = initialChat)
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val entity = projectRepo.getProject(projectId)
            if (entity != null) {
                _uiState.value = _uiState.value.copy(
                    projectId = entity.id,
                    projectTitle = entity.title,
                    clips = projectRepo.parseClips(entity.videoClipsJson),
                    audioTracks = projectRepo.parseAudio(entity.audioTracksJson),
                    textOverlays = projectRepo.parseTexts(entity.textOverlaysJson),
                    motionGraphics = projectRepo.parseMotionGraphics(entity.motionGraphicsJson),
                    colorGrade = projectRepo.parseColorGrade(entity.colorGradeJson),
                    transitions = projectRepo.parseTransitions(entity.transitionsJson)
                )
            }
        }
    }

    fun loadTemplate(templateId: String) {
        val template = DemoProjects.templates.find { it.id == templateId } ?: DemoProjects.templates.first()
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            projectId = UUID.randomUUID().toString(),
            projectTitle = template.title,
            clips = template.sampleClips,
            audioTracks = template.sampleAudio,
            textOverlays = template.sampleTexts,
            motionGraphics = template.sampleMotionGraphics,
            colorGrade = template.sampleColorGrade,
            transitions = template.sampleTransitions,
            currentPositionMs = 0L,
            selectedClipId = template.sampleClips.firstOrNull()?.id
        )
        saveCurrentProject()
    }

    fun importVideoUri(uri: Uri) {
        viewModelScope.launch {
            val uriStr = uri.toString()
            _uiState.value = _uiState.value.copy(
                analysisProgress = AnalysisProgress(percentage = 0, stage = "Reading video telemetry...")
            )

            val meta = analyzer.analyzeVideo(uriStr) { progress ->
                _uiState.value = _uiState.value.copy(analysisProgress = progress)
            }

            saveStateForUndo()
            val newClip = VideoClip(
                id = UUID.randomUUID().toString(),
                uri = uriStr,
                name = uri.lastPathSegment ?: "Imported Video",
                originalDurationMs = meta.durationMs,
                startTrimMs = 0L,
                endTrimMs = meta.durationMs,
                orderIndex = _uiState.value.clips.size
            )

            val updatedClips = _uiState.value.clips + newClip
            _uiState.value = _uiState.value.copy(
                clips = updatedClips,
                selectedClipId = newClip.id,
                analysisProgress = null
            )
            saveCurrentProject()
        }
    }

    fun setActiveSheet(sheet: EditorSheet) {
        _uiState.value = _uiState.value.copy(activeSheet = sheet)
    }

    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, totalDurationMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = clamped)
    }

    fun selectClip(clipId: String?) {
        _uiState.value = _uiState.value.copy(selectedClipId = clipId)
    }

    fun setTimelineZoom(zoom: Float) {
        _uiState.value = _uiState.value.copy(timelineZoom = zoom.coerceIn(0.5f, 3.0f))
    }

    // Clip Editing Operations
    fun splitClipAtPlayhead() {
        val pos = _uiState.value.currentPositionMs
        val clips = _uiState.value.clips
        if (clips.isEmpty()) return

        // Find which clip the playhead is currently on
        var accumulated = 0L
        var targetClipIndex = -1
        var offsetWithinClip = 0L

        for (i in clips.indices) {
            val c = clips[i]
            val clipDur = c.durationMs
            if (pos >= accumulated && pos < accumulated + clipDur) {
                targetClipIndex = i
                offsetWithinClip = ((pos - accumulated) * c.speed).toLong()
                break
            }
            accumulated += clipDur
        }

        if (targetClipIndex != -1 && offsetWithinClip > 400L) {
            saveStateForUndo()
            val orig = clips[targetClipIndex]
            val splitPoint = orig.startTrimMs + offsetWithinClip

            val clipA = orig.copy(
                id = UUID.randomUUID().toString(),
                endTrimMs = splitPoint
            )
            val clipB = orig.copy(
                id = UUID.randomUUID().toString(),
                startTrimMs = splitPoint,
                name = "${orig.name} (Part 2)"
            )

            val newClips = clips.toMutableList()
            newClips.removeAt(targetClipIndex)
            newClips.add(targetClipIndex, clipB)
            newClips.add(targetClipIndex, clipA)

            _uiState.value = _uiState.value.copy(
                clips = newClips,
                selectedClipId = clipB.id
            )
            saveCurrentProject()
        }
    }

    fun trimClip(clipId: String, newStartTrimMs: Long, newEndTrimMs: Long) {
        saveStateForUndo()
        val newClips = _uiState.value.clips.map {
            if (it.id == clipId) {
                it.copy(
                    startTrimMs = newStartTrimMs.coerceAtLeast(0L),
                    endTrimMs = newEndTrimMs.coerceAtMost(it.originalDurationMs)
                )
            } else it
        }
        _uiState.value = _uiState.value.copy(clips = newClips)
        saveCurrentProject()
    }

    fun deleteSelectedClip() {
        val selectedId = _uiState.value.selectedClipId ?: return
        saveStateForUndo()
        val newClips = _uiState.value.clips.filterNot { it.id == selectedId }
        _uiState.value = _uiState.value.copy(
            clips = newClips,
            selectedClipId = newClips.firstOrNull()?.id
        )
        saveCurrentProject()
    }

    fun updateClipSpeed(clipId: String, speed: Float) {
        saveStateForUndo()
        val newClips = _uiState.value.clips.map {
            if (it.id == clipId) it.copy(speed = speed) else it
        }
        _uiState.value = _uiState.value.copy(clips = newClips)
        saveCurrentProject()
    }

    fun updateClipVolume(clipId: String, volume: Float) {
        val newClips = _uiState.value.clips.map {
            if (it.id == clipId) it.copy(volume = volume) else it
        }
        _uiState.value = _uiState.value.copy(clips = newClips)
        saveCurrentProject()
    }

    // Color Grading
    fun updateColorGrade(settings: ColorGradeSettings) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(colorGrade = settings)
        saveCurrentProject()
    }

    // Transitions
    fun setTransitionForClip(clipId: String, type: TransitionType) {
        saveStateForUndo()
        val current = _uiState.value.transitions.filterNot { it.clipId == clipId }
        val updated = if (type != TransitionType.NONE) {
            current + TransitionEffect(
                id = UUID.randomUUID().toString(),
                clipId = clipId,
                type = type
            )
        } else current

        _uiState.value = _uiState.value.copy(transitions = updated)
        saveCurrentProject()
    }

    // Motion Graphics
    fun addMotionGraphic(graphic: MotionGraphic) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            motionGraphics = _uiState.value.motionGraphics + graphic
        )
        saveCurrentProject()
    }

    fun removeMotionGraphic(id: String) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            motionGraphics = _uiState.value.motionGraphics.filterNot { it.id == id }
        )
        saveCurrentProject()
    }

    // Subtitles & Captions
    fun addTextOverlay(textOverlay: TextOverlay) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            textOverlays = _uiState.value.textOverlays + textOverlay
        )
        saveCurrentProject()
    }

    fun removeTextOverlay(id: String) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            textOverlays = _uiState.value.textOverlays.filterNot { it.id == id }
        )
        saveCurrentProject()
    }

    // Audio & SFX
    fun playSfx(sfxType: String) {
        viewModelScope.launch {
            audioMixer.playSynthesizedSfx(sfxType)
        }
    }

    fun addAudioTrack(track: AudioTrack) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            audioTracks = _uiState.value.audioTracks + track
        )
        saveCurrentProject()
    }

    // AI Editing Intelligence
    fun sendAiPrompt(promptText: String) {
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = promptText
        )
        _uiState.value = _uiState.value.copy(
            isAiLoading = true,
            aiErrorMessage = null,
            aiChatMessages = _uiState.value.aiChatMessages + userMsg
        )

        viewModelScope.launch {
            val result = aiRepo.generateEditingPlan(
                userPrompt = promptText,
                clips = _uiState.value.clips,
                currentTotalDurationMs = totalDurationMs
            )

            result.onSuccess { plan ->
                val assistantMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = plan.styleSummary,
                    proposedPlan = plan
                )
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    latestPlan = plan,
                    aiChatMessages = _uiState.value.aiChatMessages + assistantMsg
                )
                // Automatically apply AI plan to the live engine
                applyEditingPlan(plan)
            }.onFailure { err ->
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "I couldn't reach the AI engine right now: ${err.message}. However, I have applied smart local styling matching '$promptText'."
                )
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    aiErrorMessage = err.message,
                    aiChatMessages = _uiState.value.aiChatMessages + errorMsg
                )
            }
        }
    }

    fun applyEditingPlan(plan: EditingPlan) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(
            colorGrade = plan.colorGrade,
            transitions = if (plan.transitions.isNotEmpty()) plan.transitions else _uiState.value.transitions,
            motionGraphics = if (plan.motionGraphics.isNotEmpty()) plan.motionGraphics else _uiState.value.motionGraphics,
            textOverlays = if (plan.captions.isNotEmpty()) plan.captions else _uiState.value.textOverlays,
            audioTracks = if (plan.soundEffects.isNotEmpty()) _uiState.value.audioTracks + plan.soundEffects else _uiState.value.audioTracks
        )
        saveCurrentProject()
    }

    // Export Pipeline
    fun startExport(config: ExportConfig) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                exportProgress = ExportProgress(percentage = 0, stage = "Initializing export...")
            )

            val resultFile = exportEngine.exportProject(
                clips = _uiState.value.clips,
                colorGrade = _uiState.value.colorGrade,
                config = config
            ) { progress ->
                _uiState.value = _uiState.value.copy(exportProgress = progress)
            }

            if (resultFile != null) {
                _uiState.value = _uiState.value.copy(
                    exportProgress = ExportProgress(
                        percentage = 100,
                        stage = "Complete!",
                        isComplete = true,
                        outputFile = resultFile
                    )
                )
            }
        }
    }

    fun cancelExport() {
        exportEngine.cancelExport()
        _uiState.value = _uiState.value.copy(exportProgress = null)
    }

    fun dismissExport() {
        _uiState.value = _uiState.value.copy(exportProgress = null)
    }

    // Undo / Redo
    private fun saveStateForUndo() {
        undoStack.add(_uiState.value)
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value)
            _uiState.value = prev
            updateUndoRedoAvailability()
            saveCurrentProject()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value)
            _uiState.value = next
            updateUndoRedoAvailability()
            saveCurrentProject()
        }
    }

    private fun updateUndoRedoAvailability() {
        _uiState.value = _uiState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    private fun saveCurrentProject() {
        viewModelScope.launch {
            val state = _uiState.value
            projectRepo.saveProject(
                id = state.projectId,
                title = state.projectTitle,
                thumbnailUri = state.clips.firstOrNull()?.uri,
                clips = state.clips,
                audioTracks = state.audioTracks,
                textOverlays = state.textOverlays,
                motionGraphics = state.motionGraphics,
                colorGrade = state.colorGrade,
                transitions = state.transitions,
                lastPrompt = state.latestPlan?.userPrompt
            )
        }
    }

    fun renameProject(newTitle: String) {
        _uiState.value = _uiState.value.copy(projectTitle = newTitle)
        saveCurrentProject()
    }
}
