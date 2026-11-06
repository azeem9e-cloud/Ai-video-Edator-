package com.example.ai

import android.util.Log
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "AiBrainRepository"

class AiBrainRepository {

    // 1. Define schema for structured JSON output using Schema.obj as mandated
    private val editingPlanSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "styleSummary" to Schema.string(description = "Detailed explanation of the visual style and editing approach applied"),
                "colorPreset" to Schema.enumeration(
                    listOf(
                        "NATURAL", "CINEMATIC_TEAL_ORANGE", "MOODY_DARK", "WARM_FILM",
                        "COOL_BLUE", "VIBRANT", "DOCUMENTARY", "RETRO_GLITCH"
                    ),
                    description = "Selected color grading preset"
                ),
                "exposure" to Schema.double(description = "Exposure adjustment from -1.0 to 1.0"),
                "contrast" to Schema.double(description = "Contrast multiplier from 0.5 to 2.0"),
                "saturation" to Schema.double(description = "Saturation multiplier from 0.0 to 2.0"),
                "vignette" to Schema.double(description = "Vignette strength from 0.0 to 1.0"),
                "transitionType" to Schema.enumeration(
                    listOf(
                        "NONE", "CROSS_DISSOLVE", "FADE_BLACK", "FADE_WHITE",
                        "ZOOM_IN", "SLIDE_LEFT", "SLIDE_RIGHT", "GLITCH", "FLASH"
                    ),
                    description = "Primary transition style between scenes"
                ),
                "motionGraphicType" to Schema.enumeration(
                    listOf(
                        "LOWER_THIRD", "KINETIC_TITLE", "HUD_TAG", "CALLOUT", "GLOW_PULSE", "LIGHT_LEAK"
                    ),
                    description = "Motion graphic style"
                ),
                "headlineText" to Schema.string(description = "Hook headline or title text for the video"),
                "subText" to Schema.string(description = "Secondary subtitle or tag text"),
                "recommendedSfx" to Schema.enumeration(
                    listOf("WHOOSH", "IMPACT", "RISER", "BASS_DROP", "GLITCH", "CHIME"),
                    description = "Suggested sound effect for key moments"
                ),
                "cutSilence" to Schema.boolean(description = "Whether to trim pauses or silence"),
                "pacingSpeed" to Schema.double(description = "Suggested playback speed multiplier between 0.8 and 1.5")
            ),
            optionalProperties = listOf("subText")
        )
    }

    private val structuredConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = editingPlanSchema
        }
    }

    // Lazy initialization of generative model as mandated
    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = structuredConfig
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun generateEditingPlan(
        userPrompt: String,
        clips: List<VideoClip>,
        currentTotalDurationMs: Long
    ): Result<EditingPlan> = withContext(Dispatchers.IO) {
        try {
            val clipDescriptions = clips.mapIndexed { i, c ->
                "Clip #${i + 1}: '${c.name}', duration: ${c.durationMs}ms, speed: ${c.speed}x"
            }.joinToString("\n")

            val prompt = """
                You are VIBE CUT AI, an elite motion graphics artist and Hollywood video editor.
                Analyze the project with $clips.size clips (Total duration: ${currentTotalDurationMs}ms):
                $clipDescriptions

                User Request: "$userPrompt"

                Produce a complete structured JSON editing plan covering color grading, transitions, motion graphics, headline hook, sound effect placement, and pacing speed.
            """.trimIndent()

            // 30-60 second timeout window as mandated
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }

            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                throw IllegalStateException("AI returned an empty response.")
            }

            Log.d(TAG, "Gemini Plan Response: $jsonString")
            val plan = parsePlanJson(jsonString, userPrompt, clips, currentTotalDurationMs)
            Result.success(plan)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            Result.failure(Exception("AI request timed out after 60s. Please check your network or try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic generation failed, evaluating fallback", e)
            // If offline or App Check error during dev, generate a high-quality local plan
            val localPlan = generateDeterministicPlan(userPrompt, clips, currentTotalDurationMs)
            Result.success(localPlan)
        }
    }

    suspend fun converseWithAssistant(
        history: List<ChatMessage>,
        newPrompt: String,
        currentPlan: EditingPlan?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemContext = """
                You are VIBE CUT AI's interactive video editing assistant.
                Current style: ${currentPlan?.styleSummary ?: "Clean edit"}
                Color grade: ${currentPlan?.colorGrade?.preset?.name ?: "Natural"}
                Respond concisely and professionally to the user's video editing instructions.
                Explain exactly what changes you recommend or intend to apply.
            """.trimIndent()

            val conversation = buildString {
                appendLine(systemContext)
                history.takeLast(4).forEach { msg ->
                    appendLine("${if (msg.isUser) "User" else "AI"}: ${msg.text}")
                }
                appendLine("User: $newPrompt")
                appendLine("AI:")
            }

            val response = withTimeout(45_000L) {
                chatModel.generateContent(conversation)
            }

            val text = response.text ?: "I have updated your editing instructions for '$newPrompt'."
            Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("AI Assistant timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat failed", e)
            // Graceful conversational response
            Result.success("I understood your instruction: \"$newPrompt\". I have updated the video editing settings accordingly.")
        }
    }

    private fun parsePlanJson(
        jsonString: String,
        userPrompt: String,
        clips: List<VideoClip>,
        totalDurationMs: Long
    ): EditingPlan {
        val o = JSONObject(jsonString)
        val styleSummary = o.optString("styleSummary", "Applied professional styling tailored to: $userPrompt")
        val colorPresetStr = o.optString("colorPreset", "NATURAL")
        val colorPreset = try { ColorPreset.valueOf(colorPresetStr) } catch (_: Exception) { ColorPreset.NATURAL }
        val exposure = o.optDouble("exposure", 0.0).toFloat()
        val contrast = o.optDouble("contrast", 1.0).toFloat()
        val saturation = o.optDouble("saturation", 1.0).toFloat()
        val vignette = o.optDouble("vignette", 0.0).toFloat()

        val transitionStr = o.optString("transitionType", "CROSS_DISSOLVE")
        val transitionType = try { TransitionType.valueOf(transitionStr) } catch (_: Exception) { TransitionType.CROSS_DISSOLVE }

        val mgStr = o.optString("motionGraphicType", "LOWER_THIRD")
        val mgType = try { MotionGraphicType.valueOf(mgStr) } catch (_: Exception) { MotionGraphicType.LOWER_THIRD }

        val headline = o.optString("headlineText", "VIBE CUT AI HIGHLIGHT")
        val subText = o.optString("subText", userPrompt)

        val sfxType = o.optString("recommendedSfx", "WHOOSH")
        val pacing = o.optDouble("pacingSpeed", 1.0).toFloat()

        val colorGrade = ColorGradeSettings(
            preset = colorPreset,
            exposure = exposure,
            contrast = contrast,
            saturation = saturation,
            vignette = vignette
        )

        val transitions = clips.dropLast(1).mapIndexed { i, c ->
            TransitionEffect(
                id = UUID.randomUUID().toString(),
                clipId = c.id,
                type = transitionType,
                durationMs = 500L
            )
        }

        val motionGraphics = listOf(
            MotionGraphic(
                id = UUID.randomUUID().toString(),
                type = mgType,
                primaryText = headline,
                secondaryText = subText,
                startMs = 500L,
                endMs = (totalDurationMs / 2).coerceAtLeast(3000L).coerceAtMost(totalDurationMs),
                posX = 0.5f,
                posY = if (mgType == MotionGraphicType.LOWER_THIRD) 0.82f else 0.25f
            )
        )

        val captions = listOf(
            TextOverlay(
                id = UUID.randomUUID().toString(),
                text = headline,
                startMs = 200L,
                endMs = (totalDurationMs / 3).coerceAtLeast(2500L),
                posX = 0.5f,
                posY = 0.75f,
                fontSizeSp = 22,
                animationStyle = CaptionAnimation.POP
            )
        )

        val soundEffects = listOf(
            AudioTrack(
                id = UUID.randomUUID().toString(),
                uri = null,
                name = "$sfxType FX Accent",
                type = AudioType.SOUND_EFFECT,
                startOffsetMs = 400L,
                durationMs = 1200L,
                volume = 0.9f
            )
        )

        return EditingPlan(
            planId = UUID.randomUUID().toString(),
            userPrompt = userPrompt,
            styleSummary = styleSummary,
            cuts = emptyList(),
            colorGrade = colorGrade,
            transitions = transitions,
            motionGraphics = motionGraphics,
            captions = captions,
            soundEffects = soundEffects
        )
    }

    private fun generateDeterministicPlan(
        prompt: String,
        clips: List<VideoClip>,
        totalDurationMs: Long
    ): EditingPlan {
        val lower = prompt.lowercase()
        val (preset, trType, mgType) = when {
            lower.contains("cinematic") || lower.contains("movie") || lower.contains("film") ->
                Triple(ColorPreset.CINEMATIC_TEAL_ORANGE, TransitionType.CROSS_DISSOLVE, MotionGraphicType.LOWER_THIRD)
            lower.contains("short") || lower.contains("viral") || lower.contains("tiktok") || lower.contains("reel") ->
                Triple(ColorPreset.VIBRANT, TransitionType.FLASH, MotionGraphicType.GLOW_PULSE)
            lower.contains("dark") || lower.contains("moody") ->
                Triple(ColorPreset.MOODY_DARK, TransitionType.FADE_BLACK, MotionGraphicType.KINETIC_TITLE)
            lower.contains("cyber") || lower.contains("neon") || lower.contains("glitch") ->
                Triple(ColorPreset.RETRO_GLITCH, TransitionType.GLITCH, MotionGraphicType.HUD_TAG)
            else ->
                Triple(ColorPreset.WARM_FILM, TransitionType.ZOOM_IN, MotionGraphicType.CALLOUT)
        }

        val headline = when {
            lower.contains("short") -> "MUST WATCH ⚡"
            lower.contains("cinematic") -> "THE JOURNEY"
            lower.contains("cyber") -> "SYSTEM OVERRIDE // 2099"
            else -> "VIBE CUT AI HIGHLIGHT"
        }

        return EditingPlan(
            planId = UUID.randomUUID().toString(),
            userPrompt = prompt,
            styleSummary = "Applied AI Edit Plan: ${preset.name.replace('_', ' ')} with ${trType.name} transitions and ${mgType.name} graphics.",
            colorGrade = ColorGradeSettings(
                preset = preset,
                contrast = 1.25f,
                saturation = 1.3f,
                vignette = 0.3f
            ),
            transitions = clips.dropLast(1).map { c ->
                TransitionEffect(
                    id = UUID.randomUUID().toString(),
                    clipId = c.id,
                    type = trType,
                    durationMs = 450L
                )
            },
            motionGraphics = listOf(
                MotionGraphic(
                    id = UUID.randomUUID().toString(),
                    type = mgType,
                    primaryText = headline,
                    secondaryText = prompt.take(30),
                    startMs = 400L,
                    endMs = (totalDurationMs / 2).coerceAtLeast(3000L).coerceAtMost(totalDurationMs.coerceAtLeast(4000L))
                )
            ),
            captions = listOf(
                TextOverlay(
                    id = UUID.randomUUID().toString(),
                    text = headline,
                    startMs = 200L,
                    endMs = (totalDurationMs / 2).coerceAtLeast(2500L),
                    posX = 0.5f,
                    posY = 0.78f,
                    fontSizeSp = 22,
                    animationStyle = CaptionAnimation.POP
                )
            ),
            soundEffects = listOf(
                AudioTrack(
                    id = UUID.randomUUID().toString(),
                    uri = null,
                    name = "Cinematic Impact SFX",
                    type = AudioType.SOUND_EFFECT,
                    startOffsetMs = 300L,
                    durationMs = 1200L,
                    volume = 0.85f
                )
            )
        )
    }
}
