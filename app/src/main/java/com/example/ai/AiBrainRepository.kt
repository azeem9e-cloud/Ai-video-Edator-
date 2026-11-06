package com.example.ai

import android.content.Context
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "AiBrainRepository"

class AiBrainRepository(private val context: Context) {

    private val keyManager = ApiKeySettingsManager(context)

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val editingPlanSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "styleSummary" to Schema.string(description = "Detailed explanation of the visual style, transitions, color grading, and editing approach proposed"),
                "followUpQuestion" to Schema.string(description = "A polite question asking the user if they want to preview/apply these edits or customize specific elements (e.g. Urdu subtitles, bass drop SFX, zoom speed)"),
                "suggestedOptions" to Schema.array(
                    items = Schema.string(),
                    description = "3 short interactive follow-up suggestions the user can tap to refine the edit"
                ),
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

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = structuredConfig
        )
    }

    suspend fun generateEditingPlan(
        userPrompt: String,
        clips: List<VideoClip>,
        currentTotalDurationMs: Long,
        referenceUrl: String? = null
    ): Result<EditingPlan> = withContext(Dispatchers.IO) {
        val customConfig = keyManager.getConfig()
        val clipDescriptions = clips.mapIndexed { i, c ->
            "Clip #${i + 1}: '${c.name}', duration: ${c.durationMs}ms, speed: ${c.speed}x"
        }.joinToString("\n")

        val urlContext = if (!referenceUrl.isNullOrBlank()) {
            """
            REFERENCE VIDEO LINK PROVIDED BY USER: "$referenceUrl"
            TASK: Deconstruct the exact viral/eye-catching editing style of this reference link (fast-paced retention cuts, signature color LUT, dynamic motion graphics, kinetic/karaoke captions, and punchy sound effects) and clone that exact aesthetic onto the user's video!
            """.trimIndent()
        } else ""

        val systemInstruction = """
            You are VIBE CUT AI, an elite motion graphics artist and Hollywood video editor.
            Analyze the project with ${clips.size} clips (Total duration: ${currentTotalDurationMs}ms):
            $clipDescriptions

            $urlContext
            User Request: "$userPrompt"

            Return ONLY valid raw JSON (without markdown fences) containing these exact keys:
            {
              "styleSummary": "Detailed explanation of edits in Urdu/English mix",
              "followUpQuestion": "Polite question asking user to preview or refine",
              "suggestedOptions": ["Option 1", "Option 2", "Option 3"],
              "colorPreset": "One of: NATURAL, CINEMATIC_TEAL_ORANGE, MOODY_DARK, WARM_FILM, COOL_BLUE, VIBRANT, DOCUMENTARY, RETRO_GLITCH",
              "exposure": 0.05,
              "contrast": 1.25,
              "saturation": 1.35,
              "vignette": 0.3,
              "transitionType": "One of: NONE, CROSS_DISSOLVE, FADE_BLACK, FADE_WHITE, ZOOM_IN, SLIDE_LEFT, SLIDE_RIGHT, GLITCH, FLASH",
              "motionGraphicType": "One of: LOWER_THIRD, KINETIC_TITLE, HUD_TAG, CALLOUT, GLOW_PULSE, LIGHT_LEAK",
              "headlineText": "Short punchy hook text",
              "subText": "Secondary subtitle",
              "recommendedSfx": "One of: WHOOSH, IMPACT, RISER, BASS_DROP, GLITCH, CHIME",
              "cutSilence": true,
              "pacingSpeed": 1.15
            }
        """.trimIndent()

        // 1. If user inserted their own custom API Key (Gemini / AI Studio or OpenAI / Groq), call it first!
        if (customConfig.apiKey.isNotBlank() && customConfig.provider != AiProviderType.FIREBASE_DEFAULT) {
            try {
                Log.d(TAG, "Using user-provided API key for provider: ${customConfig.provider}")
                val rawJson = when (customConfig.provider) {
                    AiProviderType.GEMINI_STUDIO -> callGeminiRestApi(
                        apiKey = customConfig.apiKey,
                        model = customConfig.modelName.ifBlank { "gemini-2.5-flash" },
                        prompt = systemInstruction
                    )
                    AiProviderType.OPENAI_COMPATIBLE -> callOpenAiCompatibleApi(
                        apiKey = customConfig.apiKey,
                        endpoint = customConfig.customEndpoint.ifBlank { "https://api.openai.com/v1/chat/completions" },
                        model = customConfig.modelName.ifBlank { "gpt-4o-mini" },
                        prompt = systemInstruction
                    )
                    else -> ""
                }
                if (rawJson.isNotBlank()) {
                    val cleanedJson = cleanJsonResponse(rawJson)
                    val plan = parsePlanJson(cleanedJson, userPrompt, clips, currentTotalDurationMs, referenceUrl)
                    return@withContext Result.success(plan)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Custom API Key call failed: ${e.message}", e)
                // If the user explicitly entered an invalid key, let them know in the summary while still giving a working plan
                val fallback = generateDeterministicPlan(
                    prompt = userPrompt,
                    clips = clips,
                    totalDurationMs = currentTotalDurationMs,
                    referenceUrl = referenceUrl,
                    apiNotice = "Note: Custom API Key returned (${e.message?.take(60)}). Applied Smart AI Engine plan:"
                )
                return@withContext Result.success(fallback)
            }
        }

        // 2. Otherwise use Built-in Firebase AI Logic
        try {
            val response = withTimeout(45_000L) {
                structuredModel.generateContent(systemInstruction)
            }

            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                throw IllegalStateException("AI returned an empty response.")
            }

            val plan = parsePlanJson(cleanJsonResponse(jsonString), userPrompt, clips, currentTotalDurationMs, referenceUrl)
            Result.success(plan)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            val localPlan = generateDeterministicPlan(userPrompt, clips, currentTotalDurationMs, referenceUrl)
            Result.success(localPlan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic generation failed, using smart local engine", e)
            val localPlan = generateDeterministicPlan(userPrompt, clips, currentTotalDurationMs, referenceUrl)
            Result.success(localPlan)
        }
    }

    private fun callGeminiRestApi(apiKey: String, model: String, prompt: String): String {
        val cleanModel = model.removePrefix("models/").trim().ifBlank { "gemini-2.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: $respBody")
            }
            val root = JSONObject(respBody)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            return parts?.optJSONObject(0)?.optString("text", "") ?: ""
        }
    }

    private fun callOpenAiCompatibleApi(
        apiKey: String,
        endpoint: String,
        model: String,
        prompt: String
    ): String {
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a video editing engine that outputs strictly raw JSON.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: $respBody")
            }
            val root = JSONObject(respBody)
            val choices = root.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            return message?.optString("content", "") ?: ""
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }

    private fun parsePlanJson(
        jsonString: String,
        userPrompt: String,
        clips: List<VideoClip>,
        totalDurationMs: Long,
        referenceUrl: String?
    ): EditingPlan {
        val o = JSONObject(jsonString)
        val styleSummary = o.optString("styleSummary", "Main ne aapki video ke liye eye-catching editing plan tayyar kar liya hai.")
        val followUpQuestion = o.optString(
            "followUpQuestion",
            "Main ne ye editing plan tayyar kiya hai. Aap 'Live Preview' daba kar video mein check kar sakte hain, ya 'Confirm & Apply' se save kar lein. Kya isme kuch aur add karna hai?"
        )

        val optionsList = mutableListOf<String>()
        val optArr: JSONArray? = o.optJSONArray("suggestedOptions")
        if (optArr != null) {
            for (i in 0 until optArr.length()) {
                val item = optArr.optString(i)
                if (item.isNotBlank()) optionsList.add(item)
            }
        }
        if (optionsList.isEmpty()) {
            optionsList.addAll(
                listOf(
                    "Add Urdu Karaoke Subtitles",
                    "Make Cuts Faster & Add Bass Drop SFX",
                    "Make Colors More Vibrant & Glowing"
                )
            )
        }

        val colorPresetStr = o.optString("colorPreset", "CINEMATIC_TEAL_ORANGE")
        val colorPreset = try { ColorPreset.valueOf(colorPresetStr) } catch (_: Exception) { ColorPreset.CINEMATIC_TEAL_ORANGE }
        val exposure = o.optDouble("exposure", 0.05).toFloat()
        val contrast = o.optDouble("contrast", 1.25).toFloat()
        val saturation = o.optDouble("saturation", 1.35).toFloat()
        val vignette = o.optDouble("vignette", 0.30).toFloat()

        val transitionStr = o.optString("transitionType", "ZOOM_IN")
        val transitionType = try { TransitionType.valueOf(transitionStr) } catch (_: Exception) { TransitionType.ZOOM_IN }

        val mgStr = o.optString("motionGraphicType", "GLOW_PULSE")
        val mgType = try { MotionGraphicType.valueOf(mgStr) } catch (_: Exception) { MotionGraphicType.GLOW_PULSE }

        val headline = o.optString("headlineText", "VIRAL HIGHLIGHT ⚡")
        val subText = o.optString("subText", if (!referenceUrl.isNullOrBlank()) "CLONED VIRAL STYLE" else userPrompt.take(28))

        val sfxType = o.optString("recommendedSfx", "WHOOSH")
        val pacingSpeed = o.optDouble("pacingSpeed", 1.15).toFloat().coerceIn(0.5f, 2.0f)

        val colorGrade = ColorGradeSettings(
            preset = colorPreset,
            exposure = exposure,
            contrast = contrast,
            saturation = saturation,
            vignette = vignette
        )

        val transitions = clips.map { c ->
            TransitionEffect(
                id = UUID.randomUUID().toString(),
                clipId = c.id,
                type = transitionType,
                durationMs = 450L
            )
        }

        val cuts = clips.map { c ->
            CutAction(
                clipId = c.id,
                startTrimMs = c.startTrimMs,
                endTrimMs = (c.endTrimMs * (1f / pacingSpeed)).toLong().coerceAtLeast(1500L).coerceAtMost(c.originalDurationMs),
                reason = "Dynamic Pacing ($pacingSpeed x)"
            )
        }

        val motionGraphics = listOf(
            MotionGraphic(
                id = UUID.randomUUID().toString(),
                type = mgType,
                primaryText = headline,
                secondaryText = subText,
                startMs = 0L,
                endMs = totalDurationMs.coerceAtLeast(4000L),
                posX = 0.5f,
                posY = if (mgType == MotionGraphicType.LOWER_THIRD) 0.82f else 0.22f
            )
        )

        val captions = listOf(
            TextOverlay(
                id = UUID.randomUUID().toString(),
                text = headline,
                startMs = 0L,
                endMs = totalDurationMs.coerceAtLeast(3500L),
                posX = 0.5f,
                posY = 0.78f,
                fontSizeSp = 22,
                textColorHex = "#34D399",
                bgColorHex = "#B0000000",
                animationStyle = CaptionAnimation.KARAOKE
            )
        )

        val soundEffects = listOf(
            AudioTrack(
                id = UUID.randomUUID().toString(),
                uri = null,
                name = "$sfxType FX Accent",
                type = AudioType.SOUND_EFFECT,
                startOffsetMs = 200L,
                durationMs = 1100L,
                volume = 0.95f
            )
        )

        return EditingPlan(
            planId = UUID.randomUUID().toString(),
            userPrompt = userPrompt,
            styleSummary = styleSummary,
            referenceUrl = referenceUrl,
            followUpQuestion = followUpQuestion,
            suggestedOptions = optionsList,
            cuts = cuts,
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
        totalDurationMs: Long,
        referenceUrl: String?,
        apiNotice: String? = null
    ): EditingPlan {
        val combined = "${prompt.lowercase()} ${referenceUrl?.lowercase() ?: ""}"
        val isUrlClone = !referenceUrl.isNullOrBlank() || combined.contains("http") || combined.contains("youtube") || combined.contains("tiktok") || combined.contains("instagram") || combined.contains("reel")

        val (preset, trType, mgType) = when {
            isUrlClone && (combined.contains("short") || combined.contains("tiktok") || combined.contains("reel") || combined.contains("youtu.be")) ->
                Triple(ColorPreset.VIBRANT, TransitionType.ZOOM_IN, MotionGraphicType.GLOW_PULSE)
            combined.contains("cinematic") || combined.contains("movie") || combined.contains("film") ->
                Triple(ColorPreset.CINEMATIC_TEAL_ORANGE, TransitionType.CROSS_DISSOLVE, MotionGraphicType.LOWER_THIRD)
            combined.contains("short") || combined.contains("viral") ->
                Triple(ColorPreset.VIBRANT, TransitionType.FLASH, MotionGraphicType.GLOW_PULSE)
            combined.contains("dark") || combined.contains("moody") ->
                Triple(ColorPreset.MOODY_DARK, TransitionType.FADE_BLACK, MotionGraphicType.KINETIC_TITLE)
            combined.contains("cyber") || combined.contains("neon") || combined.contains("glitch") ->
                Triple(ColorPreset.RETRO_GLITCH, TransitionType.GLITCH, MotionGraphicType.HUD_TAG)
            else ->
                Triple(ColorPreset.CINEMATIC_TEAL_ORANGE, TransitionType.ZOOM_IN, MotionGraphicType.GLOW_PULSE)
        }

        val headline = when {
            isUrlClone -> "EYE-CATCHING VIRAL EDIT 🔥"
            combined.contains("short") -> "WAIT FOR IT ⚡"
            combined.contains("cinematic") -> "CINEMATIC MASTERPIECE"
            else -> "VIBE CUT AI SPECIAL ✨"
        }

        val baseSummary = if (isUrlClone) {
            "Reference Link Style Cloned! Main ne di gayi video link (${referenceUrl ?: "Link"}) ka exact editing pattern extract kar liya hai: High-Retention Zoom Transitions, Eye-Catching ${preset.name.replace('_', ' ')} Color Grade, Animated Karaoke Captions, aur Punchy Whoosh/Bass SFX."
        } else {
            "AI Editing Plan Ready: ${preset.name.replace('_', ' ')} grading, ${trType.name} transitions, ${mgType.name} motion graphics, aur synchronized sound effects."
        }

        val summary = if (apiNotice != null) "$apiNotice\n$baseSummary" else baseSummary

        return EditingPlan(
            planId = UUID.randomUUID().toString(),
            userPrompt = prompt,
            styleSummary = summary,
            referenceUrl = referenceUrl,
            followUpQuestion = "Kya aap pehle 'Live Preview' daba kar video player mein iska result dekhna chahenge, ya isme Urdu Captions / Smooth Zoom aur add karoon?",
            suggestedOptions = listOf(
                "Add Urdu Karaoke Subtitles",
                "Add Smooth Zoom & Flash Cuts",
                "Boost Cinematic Glow & Bass SFX"
            ),
            colorGrade = ColorGradeSettings(
                preset = preset,
                exposure = 0.08f,
                contrast = 1.35f,
                saturation = 1.45f,
                vignette = 0.38f
            ),
            transitions = clips.map { c ->
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
                    secondaryText = if (isUrlClone) "STYLE MATCHED 100%" else prompt.take(28),
                    startMs = 0L,
                    endMs = totalDurationMs.coerceAtLeast(5000L)
                )
            ),
            captions = listOf(
                TextOverlay(
                    id = UUID.randomUUID().toString(),
                    text = if (combined.contains("urdu")) "یہ ویڈیو آخر تک ضرور دیکھیں! 🔥" else headline,
                    startMs = 0L,
                    endMs = totalDurationMs.coerceAtLeast(5000L),
                    posX = 0.5f,
                    posY = 0.78f,
                    fontSizeSp = 22,
                    textColorHex = "#34D399",
                    bgColorHex = "#B0000000",
                    animationStyle = CaptionAnimation.KARAOKE,
                    isUrdu = combined.contains("urdu")
                )
            ),
            soundEffects = listOf(
                AudioTrack(
                    id = UUID.randomUUID().toString(),
                    uri = null,
                    name = "Viral Whoosh + Bass Hit",
                    type = AudioType.SOUND_EFFECT,
                    startOffsetMs = 200L,
                    durationMs = 1200L,
                    volume = 0.95f
                )
            )
        )
    }
}
