package com.example

import com.example.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleAppTest {

    @Test
    fun testAppName() {
        val appName = "Vibe Cut AI"
        assertEquals("Vibe Cut AI", appName)
    }

    @Test
    fun testClipPacingSpeed() {
        val clip = VideoClip(
            id = "clip_1",
            uri = "content://test",
            name = "Test Clip",
            originalDurationMs = 8000L,
            startTrimMs = 1000L,
            endTrimMs = 5000L,
            speed = 1.0f
        )
        assertEquals(4000L, clip.durationMs)
    }
}
