package com.example

import com.example.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVideoClipDurationAndTrimming() {
        val clip = VideoClip(
            id = "test_clip_1",
            uri = "content://media/external/video/media/1",
            name = "Test Video",
            originalDurationMs = 10000L,
            startTrimMs = 1000L,
            endTrimMs = 7000L,
            speed = 2.0f
        )
        // (7000 - 1000) / 2.0 = 3000ms
        assertEquals(3000L, clip.durationMs)
    }

    @Test
    fun testColorGradeSettings() {
        val settings = ColorGradeSettings(
            preset = ColorPreset.CINEMATIC_TEAL_ORANGE,
            contrast = 1.3f,
            saturation = 1.4f,
            vignette = 0.35f
        )
        assertEquals(ColorPreset.CINEMATIC_TEAL_ORANGE, settings.preset)
        assertEquals(1.3f, settings.contrast)
    }

    @Test
    fun testAudioTrackModel() {
        val track = AudioTrack(
            id = "sfx_1",
            uri = null,
            name = "Whoosh SFX",
            type = AudioType.SOUND_EFFECT,
            startOffsetMs = 500L,
            durationMs = 800L,
            volume = 0.9f
        )
        assertEquals(AudioType.SOUND_EFFECT, track.type)
        assertEquals(500L, track.startOffsetMs)
    }

    @Test
    fun testEditingPlanModel() {
        val plan = EditingPlan(
            planId = "p1",
            userPrompt = "Make it cinematic",
            styleSummary = "Applied Cinematic Teal & Orange",
            cuts = emptyList(),
            colorGrade = ColorGradeSettings(preset = ColorPreset.CINEMATIC_TEAL_ORANGE)
        )
        assertEquals("p1", plan.planId)
        assertEquals(ColorPreset.CINEMATIC_TEAL_ORANGE, plan.colorGrade.preset)
    }
}
