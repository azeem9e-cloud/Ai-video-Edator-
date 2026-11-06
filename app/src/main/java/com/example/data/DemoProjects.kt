package com.example.data

import com.example.R
import com.example.model.*

object DemoProjects {

    data class TemplateItem(
        val id: String,
        val title: String,
        val category: String,
        val description: String,
        val bannerRes: Int,
        val durationLabel: String,
        val aspect: String,
        val defaultPrompt: String,
        val sampleClips: List<VideoClip>,
        val sampleAudio: List<AudioTrack>,
        val sampleTexts: List<TextOverlay>,
        val sampleMotionGraphics: List<MotionGraphic>,
        val sampleColorGrade: ColorGradeSettings,
        val sampleTransitions: List<TransitionEffect>
    )

    val templates = listOf(
        TemplateItem(
            id = "template_cyberpunk",
            title = "Cyberpunk Neo-Tokyo",
            category = "Cinematic / Sci-Fi",
            description = "High-energy futuristic aesthetic with neon violet & emerald glow, glitch transitions, and kinetic lower thirds.",
            bannerRes = R.drawable.template_cyberpunk_1793965207665,
            durationLabel = "0:15",
            aspect = "16:9",
            defaultPrompt = "Make this video cinematic cyberpunk style with vibrant neon glow, energetic cuts, glitch transitions, and futuristic HUD graphics.",
            sampleClips = listOf(
                VideoClip(
                    id = "clip_cp_1",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_cyberpunk_1793965207665,
                    name = "Neon Metropolis Skyline",
                    originalDurationMs = 5000L,
                    startTrimMs = 0L,
                    endTrimMs = 5000L,
                    orderIndex = 0
                ),
                VideoClip(
                    id = "clip_cp_2",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_cyberpunk_1793965207665,
                    name = "High-Speed Hover Flight",
                    originalDurationMs = 5000L,
                    startTrimMs = 500L,
                    endTrimMs = 4500L,
                    speed = 1.25f,
                    orderIndex = 1
                ),
                VideoClip(
                    id = "clip_cp_3",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_cyberpunk_1793965207665,
                    name = "Rain Streets & Neon Glow",
                    originalDurationMs = 6000L,
                    startTrimMs = 1000L,
                    endTrimMs = 5500L,
                    orderIndex = 2
                )
            ),
            sampleAudio = listOf(
                AudioTrack(
                    id = "bgm_synth",
                    uri = null,
                    name = "Synthwave Pulse 120BPM",
                    type = AudioType.BACKGROUND_MUSIC,
                    startOffsetMs = 0L,
                    durationMs = 14000L,
                    volume = 0.85f
                ),
                AudioTrack(
                    id = "sfx_cyber_whoosh",
                    uri = null,
                    name = "Cyber Whoosh Transition",
                    type = AudioType.SOUND_EFFECT,
                    startOffsetMs = 4800L,
                    durationMs = 800L,
                    volume = 1.0f
                )
            ),
            sampleTexts = listOf(
                TextOverlay(
                    id = "txt_cp_title",
                    text = "NEO-TOKYO // 2099",
                    startMs = 500L,
                    endMs = 3800L,
                    posX = 0.5f,
                    posY = 0.20f,
                    fontSizeSp = 24,
                    textColorHex = "#34D399",
                    bgColorHex = "#800B0F19",
                    animationStyle = CaptionAnimation.GLITCH
                ),
                TextOverlay(
                    id = "txt_urdu_sub",
                    text = "مستقبل کی دنیا میں خوش آمدید",
                    startMs = 4000L,
                    endMs = 8500L,
                    posX = 0.5f,
                    posY = 0.82f,
                    fontSizeSp = 22,
                    textColorHex = "#A78BFA",
                    bgColorHex = "#A0000000",
                    animationStyle = CaptionAnimation.KARAOKE,
                    isUrdu = true
                )
            ),
            sampleMotionGraphics = listOf(
                MotionGraphic(
                    id = "mg_hud",
                    type = MotionGraphicType.HUD_TAG,
                    primaryText = "SYS.ONLINE: 99.4%",
                    secondaryText = "VIBE CUT NEURAL ENGINE",
                    startMs = 600L,
                    endMs = 4000L,
                    posX = 0.5f,
                    posY = 0.15f
                )
            ),
            sampleColorGrade = ColorGradeSettings(
                preset = ColorPreset.RETRO_GLITCH,
                contrast = 1.35f,
                saturation = 1.4f,
                exposure = 0.05f,
                vignette = 0.45f
            ),
            sampleTransitions = listOf(
                TransitionEffect(
                    id = "tr_cp_1",
                    clipId = "clip_cp_1",
                    type = TransitionType.GLITCH,
                    durationMs = 400L
                ),
                TransitionEffect(
                    id = "tr_cp_2",
                    clipId = "clip_cp_2",
                    type = TransitionType.ZOOM_IN,
                    durationMs = 500L
                )
            )
        ),
        TemplateItem(
            id = "template_travel",
            title = "Golden Hour Wanderlust",
            category = "Cinematic Travel",
            description = "Rich cinematic film tones, smooth cross-dissolves, ambient drone pacing, and elegant lower-third titles.",
            bannerRes = R.drawable.template_travel_1793965208019,
            durationLabel = "0:18",
            aspect = "16:9",
            defaultPrompt = "Make this video look like an award-winning cinematic travel documentary with warm teal & orange color grading and gentle cross-dissolves.",
            sampleClips = listOf(
                VideoClip(
                    id = "clip_tr_1",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_travel_1793965208019,
                    name = "Alpine Peak Drone Sunrise",
                    originalDurationMs = 6000L,
                    startTrimMs = 0L,
                    endTrimMs = 6000L,
                    orderIndex = 0
                ),
                VideoClip(
                    id = "clip_tr_2",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_travel_1793965208019,
                    name = "Mountain Highway Curve",
                    originalDurationMs = 6000L,
                    startTrimMs = 500L,
                    endTrimMs = 5500L,
                    speed = 0.85f,
                    orderIndex = 1
                ),
                VideoClip(
                    id = "clip_tr_3",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_travel_1793965208019,
                    name = "Sunset Valley Horizon",
                    originalDurationMs = 7000L,
                    startTrimMs = 1000L,
                    endTrimMs = 6500L,
                    orderIndex = 2
                )
            ),
            sampleAudio = listOf(
                AudioTrack(
                    id = "bgm_acoustic",
                    uri = null,
                    name = "Cinematic Horizon Strings",
                    type = AudioType.BACKGROUND_MUSIC,
                    startOffsetMs = 0L,
                    durationMs = 17000L,
                    volume = 0.75f
                )
            ),
            sampleTexts = listOf(
                TextOverlay(
                    id = "txt_tr_loc",
                    text = "CANADIAN ROCKIES // 2026",
                    startMs = 800L,
                    endMs = 4500L,
                    posX = 0.5f,
                    posY = 0.85f,
                    fontSizeSp = 18,
                    textColorHex = "#FFFFFF",
                    bgColorHex = "#66000000",
                    animationStyle = CaptionAnimation.FADE
                )
            ),
            sampleMotionGraphics = listOf(
                MotionGraphic(
                    id = "mg_tr_lower",
                    type = MotionGraphicType.LOWER_THIRD,
                    primaryText = "ELEVATION: 2,450M",
                    secondaryText = "GOLDEN HOUR EXPEDITION",
                    startMs = 1000L,
                    endMs = 4800L,
                    posX = 0.5f,
                    posY = 0.78f
                )
            ),
            sampleColorGrade = ColorGradeSettings(
                preset = ColorPreset.CINEMATIC_TEAL_ORANGE,
                exposure = 0.05f,
                contrast = 1.25f,
                saturation = 1.3f,
                temperature = 0.25f,
                vignette = 0.35f
            ),
            sampleTransitions = listOf(
                TransitionEffect(
                    id = "tr_tr_1",
                    clipId = "clip_tr_1",
                    type = TransitionType.CROSS_DISSOLVE,
                    durationMs = 800L
                ),
                TransitionEffect(
                    id = "tr_tr_2",
                    clipId = "clip_tr_2",
                    type = TransitionType.FADE_WHITE,
                    durationMs = 600L
                )
            )
        ),
        TemplateItem(
            id = "template_shorts",
            title = "Viral Reels & Shorts Hype",
            category = "Social Media / Shorts",
            description = "Fast-paced punch cuts, dynamic karaoke captions with highlighted words, energetic impact SFX, and vibrant neon grading.",
            bannerRes = R.drawable.template_hype_shorts_1793965208278,
            durationLabel = "0:12",
            aspect = "9:16",
            defaultPrompt = "Turn this video into a viral high-retention YouTube Short with fast cuts, bold animated karaoke captions, and punchy transitions.",
            sampleClips = listOf(
                VideoClip(
                    id = "clip_sh_1",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_hype_shorts_1793965208278,
                    name = "Neon Drift Opening",
                    originalDurationMs = 4000L,
                    startTrimMs = 0L,
                    endTrimMs = 3800L,
                    orderIndex = 0
                ),
                VideoClip(
                    id = "clip_sh_2",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_hype_shorts_1793965208278,
                    name = "Tokyo Street Rhythm",
                    originalDurationMs = 4000L,
                    startTrimMs = 200L,
                    endTrimMs = 3800L,
                    speed = 1.15f,
                    orderIndex = 1
                ),
                VideoClip(
                    id = "clip_sh_3",
                    uri = "android.resource://com.aistudio.vibecut.wzkpt/" + R.drawable.template_hype_shorts_1793965208278,
                    name = "Night Pulse Climax",
                    originalDurationMs = 4500L,
                    startTrimMs = 500L,
                    endTrimMs = 4200L,
                    orderIndex = 2
                )
            ),
            sampleAudio = listOf(
                AudioTrack(
                    id = "bgm_trap",
                    uri = null,
                    name = "Phonk Drift 140BPM",
                    type = AudioType.BACKGROUND_MUSIC,
                    startOffsetMs = 0L,
                    durationMs = 12000L,
                    volume = 0.9f
                ),
                AudioTrack(
                    id = "sfx_bass_drop",
                    uri = null,
                    name = "Heavy 808 Sub Drop",
                    type = AudioType.SOUND_EFFECT,
                    startOffsetMs = 3600L,
                    durationMs = 1200L,
                    volume = 1.0f
                )
            ),
            sampleTexts = listOf(
                TextOverlay(
                    id = "txt_sh_hook",
                    text = "NEVER STOP THE HUSTLE ⚡",
                    startMs = 200L,
                    endMs = 3500L,
                    posX = 0.5f,
                    posY = 0.40f,
                    fontSizeSp = 24,
                    textColorHex = "#10B981",
                    bgColorHex = "#D0000000",
                    animationStyle = CaptionAnimation.POP
                ),
                TextOverlay(
                    id = "txt_sh_karaoke",
                    text = "WATCH TILL THE VERY END!",
                    startMs = 3700L,
                    endMs = 7500L,
                    posX = 0.5f,
                    posY = 0.65f,
                    fontSizeSp = 22,
                    textColorHex = "#FBBF24",
                    bgColorHex = "#B0000000",
                    animationStyle = CaptionAnimation.KARAOKE
                )
            ),
            sampleMotionGraphics = listOf(
                MotionGraphic(
                    id = "mg_sh_glow",
                    type = MotionGraphicType.GLOW_PULSE,
                    primaryText = "TRENDING #1",
                    secondaryText = "",
                    startMs = 300L,
                    endMs = 3500L,
                    posX = 0.5f,
                    posY = 0.20f
                )
            ),
            sampleColorGrade = ColorGradeSettings(
                preset = ColorPreset.VIBRANT,
                contrast = 1.30f,
                saturation = 1.45f,
                vignette = 0.25f
            ),
            sampleTransitions = listOf(
                TransitionEffect(
                    id = "tr_sh_1",
                    clipId = "clip_sh_1",
                    type = TransitionType.FLASH,
                    durationMs = 300L
                ),
                TransitionEffect(
                    id = "tr_sh_2",
                    clipId = "clip_sh_2",
                    type = TransitionType.SLIDE_LEFT,
                    durationMs = 350L
                )
            )
        )
    )
}
