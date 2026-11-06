package com.example.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiBrainRepository
import com.example.ai.ApiKeySettingsManager
import com.example.ai.CustomAiConfig
import com.example.data.AppDatabase
import com.example.data.DemoProjects
import com.example.data.ProjectRepository
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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
    val timelineZoom: Float = 1.0f,
    val activeSheet: EditorSheet = EditorSheet.NONE,
    val isAiLoading: Boolean = false,
    val aiErrorMessage: String? = null,
    val aiChatMessages: List<ChatMessage> = emptyList(),
    val latestPlan: EditingPlan? = null,
    val pendingPreviewPlan: EditingPlan? = null,
    val isShowingOriginalBeforeAfter: Boolean = false,
    val exportProgress: ExportProgress? = null,
    val analysisProgress: AnalysisProgress? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showApiKeyDialog: Boolean = false,
    val customAiConfig: CustomAiConfig = CustomAiConfig()
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val projectRepo = ProjectRepository(db.projectDao())
    private val apiKeySettingsManager = ApiKeySettingsManager(application)
    private val aiRepo = AiBrainRepository(application)
    private val analyzer = VideoAnalyzer(application)
    private val exportEngine = ExportEngine(application)
    private val audioMixer = AudioMixerEngine(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<EditorUiState>()
    private val redoStack = mutableListOf<EditorUiState>()

    // Backup snapshot while user is previewing a proposed AI plan before confirming
    private var prePreviewSnapshot: EditorUiState? = null

    val totalDurationMs: Long
        get() = _uiState.value.clips.sumOf { it.durationMs }.coerceAtLeast(1000L)

    init {
        val loadedConfig = apiKeySettingsManager.loadConfig()
        val initialChat = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "Assalam-o-Alaikum! Main VIBE CUT AI hoon. Aap upar 🔑 API Key button se apni koi bhi Free API Key (Google AI Studio / Gemini / OpenAI / Groq) laga sakte hain, kisi bhi YouTube/TikTok/Reel ka link paste kar ke uska exact style clone kar sakte hain, ya mujhe bata sakte hain ke video kaisi edit karni hai!"
            )
        )
        _uiState.value = _uiState.value.copy(
            aiChatMessages = initialChat,
            customAiConfig = loadedConfig
        )
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showApiKeyDialog = show,
            customAiConfig = apiKeySettingsManager.loadConfig()
        )
    }

    fun saveCustomAiConfig(config: CustomAiConfig) {
        apiKeySettingsManager.saveConfig(config)
        val statusMsg = if (config.apiKey.isNotBlank()) {
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "✅ Aapki Custom API Key (${config.provider.displayName} • ${config.modelName}) save ho gayi hai! Ab aap jo bhi editing instruction denge, main isi API key ke zariye aapki video edit karunga."
            )
        } else {
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "ℹ️ Default Built-in AI Engine active kar diya gaya hai."
            )
        }
        _uiState.value = _uiState.value.copy(
            customAiConfig = config,
            showApiKeyDialog = false,
            aiChatMessages = _uiState.value.aiChatMessages + statusMsg
        )
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val entity = projectRepo.getProject(projectId)
            if (entity != null) {
                val parsedClips = projectRepo.parseClips(entity.videoClipsJson)
                _uiState.value = _uiState.value.copy(
                    projectId = entity.id,
                    projectTitle = entity.title,
                    clips = parsedClips,
                    audioTracks = projectRepo.parseAudio(entity.audioTracksJson),
                    textOverlays = projectRepo.parseTexts(entity.textOverlaysJson),
                    motionGraphics = projectRepo.parseMotionGraphics(entity.motionGraphicsJson),
                    colorGrade = projectRepo.parseColorGrade(entity.colorGradeJson),
                    transitions = projectRepo.parseTransitions(entity.transitionsJson),
                    selectedClipId = parsedClips.firstOrNull()?.id,
                    pendingPreviewPlan = null
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
            selectedClipId = template.sampleClips.firstOrNull()?.id,
            pendingPreviewPlan = null
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

            // Replace demo clips if all current clips are placeholder URIs, otherwise append
            val existingRealClips = _uiState.value.clips.filter {
                it.uri.startsWith("content://") || it.uri.startsWith("file://")
            }
            val updatedClips = existingRealClips + newClip
            _uiState.value = _uiState.value.copy(
                clips = updatedClips,
                selectedClipId = newClip.id,
                currentPositionMs = 0L,
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

    fun toggleBeforeAfterComparison(showOriginal: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingOriginalBeforeAfter = showOriginal)
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

    fun splitClipAtPlayhead() {
        val pos = _uiState.value.currentPositionMs
        val clips = _uiState.value.clips
        if (clips.isEmpty()) return

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

        // If playhead is at the very start or end of a clip, split the selected clip right in the middle so Split ALWAYS works!
        if (targetClipIndex == -1 || offsetWithinClip <= 300L || offsetWithinClip >= (clips[targetClipIndex].endTrimMs - clips[targetClipIndex].startTrimMs - 300L)) {
            val selIdx = clips.indexOfFirst { it.id == _uiState.value.selectedClipId }.takeIf { it >= 0 } ?: 0
            targetClipIndex = selIdx
            val targetClip = clips[targetClipIndex]
            offsetWithinClip = (targetClip.endTrimMs - targetClip.startTrimMs) / 2L
        }

        if (targetClipIndex != -1 && offsetWithinClip > 200L) {
            saveStateForUndo()
            val orig = clips[targetClipIndex]
            val splitPoint = (orig.startTrimMs + offsetWithinClip).coerceIn(orig.startTrimMs + 200L, orig.endTrimMs - 200L)

            val clipA = orig.copy(
                id = UUID.randomUUID().toString(),
                endTrimMs = splitPoint
            )
            val clipB = orig.copy(
                id = UUID.randomUUID().toString(),
                startTrimMs = splitPoint,
                name = "${orig.name} (2)"
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
                val safeStart = newStartTrimMs.coerceAtLeast(0L)
                val safeEnd = newEndTrimMs.coerceAtLeast(safeStart + 500L)
                it.copy(
                    startTrimMs = safeStart,
                    endTrimMs = safeEnd,
                    originalDurationMs = maxOf(it.originalDurationMs, safeEnd)
                )
            } else it
        }
        _uiState.value = _uiState.value.copy(clips = newClips)
        saveCurrentProject()
    }

    fun deleteSelectedClip() {
        val selectedId = _uiState.value.selectedClipId ?: return
        if (_uiState.value.clips.size <= 1) return
        saveStateForUndo()
        val newClips = _uiState.value.clips.filterNot { it.id == selectedId }
        _uiState.value = _uiState.value.copy(
            clips = newClips,
            selectedClipId = newClips.firstOrNull()?.id,
            currentPositionMs = _uiState.value.currentPositionMs.coerceAtMost(
                newClips.sumOf { it.durationMs }.coerceAtLeast(1000L)
            )
        )
        saveCurrentProject()
    }

    fun updateClipSpeed(clipId: String, speed: Float) {
        saveStateForUndo()
        val newClips = _uiState.value.clips.map {
            if (it.id == clipId) it.copy(speed = speed.coerceIn(0.25f, 4.0f)) else it
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

    fun updateColorGrade(settings: ColorGradeSettings) {
        saveStateForUndo()
        _uiState.value = _uiState.value.copy(colorGrade = settings)
        saveCurrentProject()
    }

    fun setTransitionForClip(clipId: String, type: TransitionType) {
        saveStateForUndo()
        val targetClipId = clipId.ifBlank { _uiState.value.clips.firstOrNull()?.id ?: "" }
        if (targetClipId.isBlank()) return
        val current = _uiState.value.transitions.filterNot { it.clipId == targetClipId }
        val updated = if (type != TransitionType.NONE) {
            current + TransitionEffect(
                id = UUID.randomUUID().toString(),
                clipId = targetClipId,
                type = type
            )
        } else current

        _uiState.value = _uiState.value.copy(transitions = updated)
        saveCurrentProject()
    }

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

    // AI Editing Intelligence with Interactive Ask-First & Reference Link Support
    fun sendAiPrompt(promptText: String, referenceUrl: String? = null) {
        val displayMsg = if (!referenceUrl.isNullOrBlank()) {
            "🔗 Link: $referenceUrl\n✨ Instruction: $promptText"
        } else {
            promptText
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = displayMsg
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
                currentTotalDurationMs = totalDurationMs,
                referenceUrl = referenceUrl
            )

            result.onSuccess { plan ->
                val fullMessage = "${plan.styleSummary}\n\n💬 ${plan.followUpQuestion}"
                val assistantMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = fullMessage,
                    proposedPlan = plan
                )
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    latestPlan = plan,
                    aiChatMessages = _uiState.value.aiChatMessages + assistantMsg
                )
            }.onFailure { err ->
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "Error: ${err.message}"
                )
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    aiErrorMessage = err.message,
                    aiChatMessages = _uiState.value.aiChatMessages + errorMsg
                )
            }
        }
    }

    private fun applyPlanCutsToClips(clips: List<VideoClip>, cuts: List<CutAction>): List<VideoClip> {
        if (cuts.isEmpty()) return clips
        return clips.mapIndexed { idx, clip ->
            val matchingCut = cuts.find { it.clipId == clip.id } ?: cuts.getOrNull(idx)
            if (matchingCut != null) {
                val safeStart = matchingCut.startTrimMs.coerceAtLeast(0L)
                val safeEnd = matchingCut.endTrimMs.coerceAtLeast(safeStart + 800L)
                    .coerceAtMost(clip.originalDurationMs.coerceAtLeast(safeStart + 800L))
                clip.copy(
                    startTrimMs = safeStart,
                    endTrimMs = safeEnd
                )
            } else {
                clip
            }
        }
    }

    // Live Preview Mode: Temporarily shows the proposed AI edits on the video player with a floating confirmation bar
    fun previewEditingPlan(plan: EditingPlan) {
        if (prePreviewSnapshot == null) {
            prePreviewSnapshot = _uiState.value
        }
        viewModelScope.launch {
            audioMixer.playSynthesizedSfx("WHOOSH")
        }
        val updatedClips = applyPlanCutsToClips(_uiState.value.clips, plan.cuts)
        _uiState.value = _uiState.value.copy(
            clips = updatedClips,
            colorGrade = plan.colorGrade,
            transitions = if (plan.transitions.isNotEmpty()) plan.transitions else _uiState.value.transitions,
            motionGraphics = if (plan.motionGraphics.isNotEmpty()) plan.motionGraphics else _uiState.value.motionGraphics,
            textOverlays = if (plan.captions.isNotEmpty()) plan.captions else _uiState.value.textOverlays,
            audioTracks = if (plan.soundEffects.isNotEmpty()) _uiState.value.audioTracks + plan.soundEffects else _uiState.value.audioTracks,
            pendingPreviewPlan = plan,
            activeSheet = EditorSheet.NONE,
            currentPositionMs = 0L,
            isPlaying = true
        )
    }

    // Permanently Confirm & Apply the AI Plan
    fun applyEditingPlan(plan: EditingPlan) {
        val baseState = prePreviewSnapshot ?: _uiState.value
        prePreviewSnapshot?.let { snapshot ->
            undoStack.add(snapshot.copy(pendingPreviewPlan = null))
            prePreviewSnapshot = null
        } ?: saveStateForUndo()

        viewModelScope.launch {
            audioMixer.playSynthesizedSfx("CHIME")
        }

        val updatedClips = applyPlanCutsToClips(baseState.clips, plan.cuts)
        _uiState.value = _uiState.value.copy(
            clips = updatedClips,
            colorGrade = plan.colorGrade,
            transitions = if (plan.transitions.isNotEmpty()) plan.transitions else baseState.transitions,
            motionGraphics = if (plan.motionGraphics.isNotEmpty()) plan.motionGraphics else baseState.motionGraphics,
            textOverlays = if (plan.captions.isNotEmpty()) plan.captions else baseState.textOverlays,
            audioTracks = if (plan.soundEffects.isNotEmpty()) baseState.audioTracks + plan.soundEffects else baseState.audioTracks,
            pendingPreviewPlan = null,
            activeSheet = EditorSheet.NONE,
            currentPositionMs = 0L,
            isPlaying = true
        )
        updateUndoRedoAvailability()
        saveCurrentProject()
    }

    // Reject / Revert the Previewed Plan back to original state
    fun rejectPreviewPlan() {
        val snapshot = prePreviewSnapshot
        if (snapshot != null) {
            _uiState.value = snapshot.copy(
                pendingPreviewPlan = null,
                isPlaying = false,
                aiChatMessages = _uiState.value.aiChatMessages
            )
            prePreviewSnapshot = null
        } else {
            _uiState.value = _uiState.value.copy(pendingPreviewPlan = null, isPlaying = false)
        }
    }

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

    private fun saveStateForUndo() {
        undoStack.add(_uiState.value.copy(pendingPreviewPlan = null))
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
