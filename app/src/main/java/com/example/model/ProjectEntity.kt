package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val thumbnailUri: String?,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val videoClipsJson: String = "[]",
    val audioTracksJson: String = "[]",
    val textOverlaysJson: String = "[]",
    val motionGraphicsJson: String = "[]",
    val colorGradeJson: String = "{}",
    val transitionsJson: String = "[]",
    val lastAiPrompt: String? = null
)
