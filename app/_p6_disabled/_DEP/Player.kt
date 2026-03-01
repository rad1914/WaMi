// @path: app/_p6_disabled/_DEP/Player.kt
// @path: app/src/main/java/com/radwrld/resonance/Player.kt
package com.radwrld.resonance

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class Player : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var list: ArrayList<Uri>
    private var index = 0
    private lateinit var queueView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = ExoPlayer.Builder(this).build()

        list = TrackStore.list
        index = intent.getIntExtra("index", 0)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        setContentView(root)

        queueView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        Button(this).apply {
            text = "Prev"
            setOnClickListener { move(-1) }
        }.also(root::addView)

        Button(this).apply {
            text = "Play"
            setOnClickListener { playCurrent() }
        }.also(root::addView)

        Button(this).apply {
            text = "Pause"
            setOnClickListener { player.pause() }
        }.also(root::addView)

        Button(this).apply {
            text = "Next"
            setOnClickListener { move(1) }
        }.also(root::addView)

        root.addView(queueView)

        renderQueue()
        playCurrent()
    }

    private fun move(delta: Int) {
        if (list.isEmpty()) return
        index = (index + delta).coerceIn(0, list.lastIndex)
        playCurrent()
        renderQueue()
    }

    private fun playCurrent() {
        val uri = list.getOrNull(index) ?: return
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    private fun renderQueue() {
        queueView.removeAllViews()

        list.forEachIndexed { i, uri ->

            val name = DocumentFile.fromSingleUri(this, uri)?.name ?: uri.lastPathSegment ?: "track"

            TextView(this).apply {

                text = if (i == index) "▶ $name" else name
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    index = i
                    playCurrent()
                    renderQueue()
                }
            }.also(queueView::addView)
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
