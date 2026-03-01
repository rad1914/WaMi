// @path: app/u/resonance/ValenceResult.kt
package com.radwrld.resonance.valence

import kotlinx.serialization.Serializable

@Serializable
data class ValenceResult(
    val trackUri: String,
    val displayName: String,
    val durationMs: Long,
    val sampleRate: Int,
    val predictedValence: Double,
    val featuresSummary: Map<String, Double>,
    val timestampUtc: Long = System.currentTimeMillis()
)
