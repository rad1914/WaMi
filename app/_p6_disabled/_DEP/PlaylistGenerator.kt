// @path: app/_p6_disabled/_DEP/PlaylistGenerator.kt
// @path: app/src/main/java/com/radwrld/resonance/PlaylistGenerator.kt
package com.radwrld.resonance

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import java.io.File

object PlaylistGenerator {

    private const val MAX_DIST = 0.75

    fun generate(
        ctx: Context,
        tree: Uri,
        targetMood: String
    ): ArrayList<Uri> {

        val jsonFile = File(ctx.getExternalFilesDir(null), "valence.json")
        if (!jsonFile.exists()) return arrayListOf()

        val arr = JSONArray(jsonFile.readText())

        val items = List(arr.length()) { i ->
            arr.getJSONObject(i).run {
                Triple(
                    getString("file"),
                    getDouble("valence"),
                    getDouble("arousal")
                )
            }
        }

        if (items.isEmpty()) return arrayListOf()

        val meanV = items.map { it.second }.average()
        val meanA = items.map { it.third }.average()

        val moodFiles = items
            .filter { (_, v, a) ->
                MoodClassifier.classify(v - meanV, a - meanA) == targetMood
            }
            .map { it.first }
            .toSet()

        val dir = DocumentFile.fromTreeUri(ctx, tree) ?: return arrayListOf()

        val uris = dir.listFiles()
            .filter { it.isFile && moodFiles.contains(it.name) }
            .mapNotNull { it.uri }

        return ArrayList(uris)
    }

    fun generateDynamic(
        ctx: Context,
        tree: Uri,
        targetValence: Float,
        targetArousal: Float
    ): ArrayList<Uri> {

        val jsonFile = File(ctx.getExternalFilesDir(null), "valence.json")
        if (!jsonFile.exists()) return arrayListOf()

        val arr = JSONArray(jsonFile.readText())

        val items = List(arr.length()) { i ->
            arr.getJSONObject(i).run {
                Triple(
                    getString("file"),
                    getDouble("valence").toFloat(),
                    getDouble("arousal").toFloat()
                )
            }
        }

        if (items.isEmpty()) return arrayListOf()

        val dir = DocumentFile.fromTreeUri(ctx, tree) ?: return arrayListOf()

        val scored = items.map { (name, v, a) ->
            val dv = v - targetValence
            val da = a - targetArousal
            val dist = kotlin.math.sqrt(dv * dv + da * da)
            Pair(name, dist)
        }
            .filter { it.second <= MAX_DIST }
            .sortedBy { it.second }
            .map { it.first }
            .toSet()

        val uris = dir.listFiles()
            .filter { it.isFile && scored.contains(it.name) }
            .mapNotNull { it.uri }

        return ArrayList(uris)
    }
}
