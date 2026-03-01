// @path: app/u/resonance/MainActivity.kt
package com.radwrld.resonance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.radwrld.resonance.valence.ValenceExtractor
import com.radwrld.resonance.valence.ValenceResult
import java.io.File

class MainActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    private val openTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri: Uri? ->
        treeUri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onTreePicked(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        openTreeLauncher.launch(null)
    }

    private fun onTreePicked(treeUri: Uri) {
        val pickedDir = DocumentFile.fromTreeUri(this, treeUri) ?: return
        val audioFiles = pickedDir.listFiles().filter { it.isFile && (it.type?.startsWith("audio/") == true || it.name?.endsWith(".mp3", true) == true) }

        if (audioFiles.isEmpty()) return

        val doc = audioFiles.first()
        val uri = doc.uri
        prepareAndPlay(uri)

        CoroutineScope(Dispatchers.IO).launch {
            val result: ValenceResult = ValenceExtractor.extractPerTrackValence(applicationContext, uri)
            saveValenceJson(result)
        }
    }

    private fun prepareAndPlay(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    private fun saveValenceJson(result: ValenceResult) {
        val json = Json { prettyPrint = true }.encodeToString(result)
        val outDir = File(getExternalFilesDir(null), "valence")
        if (!outDir.exists()) outDir.mkdirs()
        val safeName = result.displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outFile = File(outDir, "${safeName}.valence.json")
        outFile.writeText(json)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
