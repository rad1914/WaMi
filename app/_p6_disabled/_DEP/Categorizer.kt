// @path: app/_p6_disabled/_DEP/Categorizer.kt
// @path: app/src/main/java/com/radwrld/resonance/Categorizer.kt
package com.radwrld.resonance

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.io.File
import kotlin.math.abs

object MoodClassifier {

    private const val DZ = 0.15

    fun classify(vRaw: Double, aRaw: Double): String {
        val v = vRaw.coerceIn(-1.0, 1.0)
        val a = aRaw.coerceIn(-1.0, 1.0)

        if (abs(v) < DZ && abs(a) < DZ) return "Neutral"

        return when {
            v >= 0 && a >= 0 -> if (a > 0.6) "Excited" else "Happy"
            v >= 0 && a < 0  -> if (a < -0.6) "Relaxed" else "Calm"
            v < 0 && a < 0   -> if (a < -0.6) "Depressed" else "Sad"
            else             -> if (a > 0.6) "Angry" else "Tense"
        }
    }
}

class Categorizer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(ScrollView(this).apply { addView(root) })

        val file = File(getExternalFilesDir(null), "valence.json")
        if (!file.exists()) {
            Toast.makeText(this, "valence.json not found", Toast.LENGTH_LONG).show()
            return
        }

        val arr = JSONArray(file.readText())

        val items = List(arr.length()) { i ->
            arr.getJSONObject(i).run {
                Triple(
                    getString("file"),
                    getDouble("valence"),
                    getDouble("arousal")
                )
            }
        }

        if (items.isEmpty()) return

        val meanV = items.map { it.second }.average()
        val meanA = items.map { it.third }.average()

        val buckets = mutableMapOf<String, MutableList<String>>()

        items.forEach { (name, v, a) ->
            val mood = MoodClassifier.classify(v - meanV, a - meanA)
            buckets.getOrPut(mood) { mutableListOf() }.add(name)
        }

        val order = listOf(
            "Excited","Happy","Calm","Relaxed",
            "Neutral","Tense","Sad","Depressed","Angry"
        )

        order.forEach { mood ->
            val list = buckets[mood] ?: return@forEach

            root.addView(TextView(this).apply {
                text = "=== $mood (${list.size}) ==="
                textSize = 20f
            })

            list.forEach { name ->
                root.addView(TextView(this).apply { text = name })
            }
        }
    }
}
