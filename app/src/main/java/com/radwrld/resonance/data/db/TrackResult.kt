// @path: app/src/main/java/com/radwrld/resonance/data/db/TrackResult.kt
package com.radwrld.resonance.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_results")
data class TrackResult(
    @PrimaryKey val trackId: String,
    val modelVersion: String,
    val durationSeconds: Double?,
    val windowsProcessed: Int,
    val valence: Float,
    val arousal: Float,
    val confidence: Float,
    val perWindowJson: String,
    val lastUpdatedMs: Long
)
