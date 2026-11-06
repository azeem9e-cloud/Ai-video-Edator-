package com.example.data

import android.content.Context
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProject(id: String): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun saveProject(
        id: String,
        title: String,
        thumbnailUri: String?,
        clips: List<VideoClip>,
        audioTracks: List<AudioTrack>,
        textOverlays: List<TextOverlay>,
        motionGraphics: List<MotionGraphic>,
        colorGrade: ColorGradeSettings,
        transitions: List<TransitionEffect>,
        lastPrompt: String? = null
    ) {
        val totalDuration = clips.sumOf { it.durationMs }
        val entity = ProjectEntity(
            id = id,
            title = title,
            thumbnailUri = thumbnailUri ?: clips.firstOrNull()?.uri,
            durationMs = totalDuration,
            updatedAt = System.currentTimeMillis(),
            videoClipsJson = serializeClips(clips),
            audioTracksJson = serializeAudio(audioTracks),
            textOverlaysJson = serializeTexts(textOverlays),
            motionGraphicsJson = serializeMotionGraphics(motionGraphics),
            colorGradeJson = serializeColorGrade(colorGrade),
            transitionsJson = serializeTransitions(transitions),
            lastAiPrompt = lastPrompt
        )
        projectDao.insertOrUpdate(entity)
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
    }

    suspend fun duplicateProject(id: String): String? {
        val existing = projectDao.getProjectById(id) ?: return null
        val newId = UUID.randomUUID().toString()
        val copy = existing.copy(
            id = newId,
            title = "${existing.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertOrUpdate(copy)
        return newId
    }

    // JSON Serialization Helpers
    fun serializeClips(clips: List<VideoClip>): String {
        val arr = JSONArray()
        for (c in clips) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("uri", c.uri)
            obj.put("name", c.name)
            obj.put("origDur", c.originalDurationMs)
            obj.put("startTrim", c.startTrimMs)
            obj.put("endTrim", c.endTrimMs)
            obj.put("speed", c.speed.toDouble())
            obj.put("volume", c.volume.toDouble())
            obj.put("orderIndex", c.orderIndex)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseClips(json: String?): List<VideoClip> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<VideoClip>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    VideoClip(
                        id = o.getString("id"),
                        uri = o.getString("uri"),
                        name = o.getString("name"),
                        originalDurationMs = o.getLong("origDur"),
                        startTrimMs = o.optLong("startTrim", 0L),
                        endTrimMs = o.optLong("endTrim", o.getLong("origDur")),
                        speed = o.optDouble("speed", 1.0).toFloat(),
                        volume = o.optDouble("volume", 1.0).toFloat(),
                        orderIndex = o.optInt("orderIndex", i)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeAudio(tracks: List<AudioTrack>): String {
        val arr = JSONArray()
        for (a in tracks) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("uri", a.uri ?: "")
            obj.put("name", a.name)
            obj.put("type", a.type.name)
            obj.put("startOffset", a.startOffsetMs)
            obj.put("duration", a.durationMs)
            obj.put("volume", a.volume.toDouble())
            obj.put("isMuted", a.isMuted)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseAudio(json: String?): List<AudioTrack> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<AudioTrack>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    AudioTrack(
                        id = o.getString("id"),
                        uri = o.optString("uri").takeIf { it.isNotBlank() },
                        name = o.getString("name"),
                        type = AudioType.valueOf(o.optString("type", AudioType.SOUND_EFFECT.name)),
                        startOffsetMs = o.getLong("startOffset"),
                        durationMs = o.getLong("duration"),
                        volume = o.optDouble("volume", 1.0).toFloat(),
                        isMuted = o.optBoolean("isMuted", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeTexts(texts: List<TextOverlay>): String {
        val arr = JSONArray()
        for (t in texts) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("text", t.text)
            obj.put("startMs", t.startMs)
            obj.put("endMs", t.endMs)
            obj.put("posX", t.posX.toDouble())
            obj.put("posY", t.posY.toDouble())
            obj.put("fontSizeSp", t.fontSizeSp)
            obj.put("textColorHex", t.textColorHex)
            obj.put("bgColorHex", t.bgColorHex ?: "")
            obj.put("animationStyle", t.animationStyle.name)
            obj.put("isUrdu", t.isUrdu)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseTexts(json: String?): List<TextOverlay> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<TextOverlay>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TextOverlay(
                        id = o.getString("id"),
                        text = o.getString("text"),
                        startMs = o.getLong("startMs"),
                        endMs = o.getLong("endMs"),
                        posX = o.optDouble("posX", 0.5).toFloat(),
                        posY = o.optDouble("posY", 0.8).toFloat(),
                        fontSizeSp = o.optInt("fontSizeSp", 20),
                        textColorHex = o.optString("textColorHex", "#FFFFFF"),
                        bgColorHex = o.optString("bgColorHex").takeIf { it.isNotBlank() },
                        animationStyle = CaptionAnimation.valueOf(o.optString("animationStyle", CaptionAnimation.POP.name)),
                        isUrdu = o.optBoolean("isUrdu", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeMotionGraphics(mg: List<MotionGraphic>): String {
        val arr = JSONArray()
        for (m in mg) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("type", m.type.name)
            obj.put("primaryText", m.primaryText)
            obj.put("secondaryText", m.secondaryText)
            obj.put("startMs", m.startMs)
            obj.put("endMs", m.endMs)
            obj.put("posX", m.posX.toDouble())
            obj.put("posY", m.posY.toDouble())
            obj.put("scale", m.scale.toDouble())
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseMotionGraphics(json: String?): List<MotionGraphic> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<MotionGraphic>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MotionGraphic(
                        id = o.getString("id"),
                        type = MotionGraphicType.valueOf(o.optString("type", MotionGraphicType.LOWER_THIRD.name)),
                        primaryText = o.getString("primaryText"),
                        secondaryText = o.optString("secondaryText", ""),
                        startMs = o.getLong("startMs"),
                        endMs = o.getLong("endMs"),
                        posX = o.optDouble("posX", 0.5).toFloat(),
                        posY = o.optDouble("posY", 0.25).toFloat(),
                        scale = o.optDouble("scale", 1.0).toFloat()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeColorGrade(cg: ColorGradeSettings): String {
        val obj = JSONObject()
        obj.put("preset", cg.preset.name)
        obj.put("exposure", cg.exposure.toDouble())
        obj.put("contrast", cg.contrast.toDouble())
        obj.put("saturation", cg.saturation.toDouble())
        obj.put("temperature", cg.temperature.toDouble())
        obj.put("tint", cg.tint.toDouble())
        obj.put("vignette", cg.vignette.toDouble())
        obj.put("sharpness", cg.sharpness.toDouble())
        return obj.toString()
    }

    fun parseColorGrade(json: String?): ColorGradeSettings {
        if (json.isNullOrBlank()) return ColorGradeSettings()
        return try {
            val o = JSONObject(json)
            ColorGradeSettings(
                preset = ColorPreset.valueOf(o.optString("preset", ColorPreset.NATURAL.name)),
                exposure = o.optDouble("exposure", 0.0).toFloat(),
                contrast = o.optDouble("contrast", 1.0).toFloat(),
                saturation = o.optDouble("saturation", 1.0).toFloat(),
                temperature = o.optDouble("temperature", 0.0).toFloat(),
                tint = o.optDouble("tint", 0.0).toFloat(),
                vignette = o.optDouble("vignette", 0.0).toFloat(),
                sharpness = o.optDouble("sharpness", 0.0).toFloat()
            )
        } catch (e: Exception) {
            ColorGradeSettings()
        }
    }

    fun serializeTransitions(tr: List<TransitionEffect>): String {
        val arr = JSONArray()
        for (t in tr) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("clipId", t.clipId)
            obj.put("type", t.type.name)
            obj.put("durationMs", t.durationMs)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseTransitions(json: String?): List<TransitionEffect> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<TransitionEffect>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TransitionEffect(
                        id = o.getString("id"),
                        clipId = o.getString("clipId"),
                        type = TransitionType.valueOf(o.optString("type", TransitionType.CROSS_DISSOLVE.name)),
                        durationMs = o.optLong("durationMs", 500L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
