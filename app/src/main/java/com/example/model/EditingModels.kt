package com.example.model

enum class AudioType {
    BACKGROUND_MUSIC,
    SOUND_EFFECT,
    VOICEOVER
}

enum class CaptionAnimation {
    NONE,
    POP,
    FADE,
    TYPEWRITER,
    KARAOKE,
    GLITCH
}

enum class MotionGraphicType {
    LOWER_THIRD,
    KINETIC_TITLE,
    HUD_TAG,
    CALLOUT,
    GLOW_PULSE,
    LIGHT_LEAK
}

enum class ColorPreset {
    NATURAL,
    CINEMATIC_TEAL_ORANGE,
    MOODY_DARK,
    WARM_FILM,
    COOL_BLUE,
    VIBRANT,
    DOCUMENTARY,
    RETRO_GLITCH
}

enum class TransitionType {
    NONE,
    CROSS_DISSOLVE,
    FADE_BLACK,
    FADE_WHITE,
    ZOOM_IN,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    GLITCH,
    FLASH
}

data class VideoClip(
    val id: String,
    val uri: String,
    val name: String,
    val originalDurationMs: Long,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = originalDurationMs,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val orderIndex: Int = 0
) {
    val durationMs: Long
        get() = (((endTrimMs - startTrimMs).coerceAtLeast(0L)) / speed.coerceAtLeast(0.1f)).toLong()
}

data class AudioTrack(
    val id: String,
    val uri: String?,
    val name: String,
    val type: AudioType,
    val startOffsetMs: Long,
    val durationMs: Long,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
)

data class TextOverlay(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val posX: Float = 0.5f, // 0..1
    val posY: Float = 0.8f, // 0..1
    val fontSizeSp: Int = 20,
    val textColorHex: String = "#FFFFFF",
    val bgColorHex: String? = "#80000000",
    val animationStyle: CaptionAnimation = CaptionAnimation.POP,
    val isUrdu: Boolean = false
)

data class MotionGraphic(
    val id: String,
    val type: MotionGraphicType,
    val primaryText: String,
    val secondaryText: String = "",
    val startMs: Long,
    val endMs: Long,
    val posX: Float = 0.5f,
    val posY: Float = 0.25f,
    val scale: Float = 1.0f
)

data class ColorGradeSettings(
    val preset: ColorPreset = ColorPreset.NATURAL,
    val exposure: Float = 0f,      // -1.0 to 1.0
    val contrast: Float = 1.0f,    // 0.5 to 2.0
    val saturation: Float = 1.0f,  // 0.0 to 2.0
    val temperature: Float = 0f,   // -1.0 (cool) to 1.0 (warm)
    val tint: Float = 0f,          // -1.0 (green) to 1.0 (magenta)
    val vignette: Float = 0f,      // 0.0 to 1.0
    val sharpness: Float = 0f      // 0.0 to 1.0
)

data class TransitionEffect(
    val id: String,
    val clipId: String,
    val type: TransitionType,
    val durationMs: Long = 500L
)

data class CutAction(
    val clipId: String,
    val startTrimMs: Long,
    val endTrimMs: Long,
    val reason: String
)

data class EditingPlan(
    val planId: String,
    val userPrompt: String,
    val styleSummary: String,
    val cuts: List<CutAction> = emptyList(),
    val colorGrade: ColorGradeSettings = ColorGradeSettings(),
    val transitions: List<TransitionEffect> = emptyList(),
    val motionGraphics: List<MotionGraphic> = emptyList(),
    val captions: List<TextOverlay> = emptyList(),
    val soundEffects: List<AudioTrack> = emptyList()
)

data class VideoMetadata(
    val uri: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
    val frameRate: Float = 30f,
    val bitrate: Long = 5000000L,
    val mimeType: String = "video/mp4",
    val audioChannels: Int = 2,
    val detectedScenesCount: Int = 1
)

data class ExportConfig(
    val resolutionLabel: String = "1080p",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val isH265: Boolean = false,
    val bitrateKbps: Int = 8000
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val proposedPlan: EditingPlan? = null,
    val timestamp: Long = System.currentTimeMillis()
)
