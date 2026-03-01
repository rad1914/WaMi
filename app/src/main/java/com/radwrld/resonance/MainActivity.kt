package com.radwrld.resonance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.radwrld.resonance.data.db.AppDatabase
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        folderUri = uri
        loadFolderTracks()
    }
    private var folderUri: Uri? = null
    private val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    fun AppContent() {
        var tracks by remember { mutableStateOf(listOf<DocumentFile>()) }
        var results by remember { mutableStateOf(listOf<com.radwrld.resonance.data.db.TrackResult>()) }
        val dao = AppDatabase.getInstance(this@MainActivity).trackResultDao()
        LaunchedEffect(Unit) {
            dao.getAllLive().observe(this@MainActivity) { list -> results = list ?: listOf() }
        }
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { pickFolder.launch(null) }) { Text("Pick folder") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (folderUri != null) loadFolderTracks() }) { Text("Refresh") }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(tracks.size) { i ->
                    val doc = tracks[i]
                    val r = results.find { it.trackId == doc.uri.toString() }
                    TrackRow(doc, r)
                }
            }
        }
    }

    private fun loadFolderTracks() {
        val uri = folderUri ?: return
        val docs = DocumentFile.fromTreeUri(this, uri)?.listFiles()?.filter { it.isFile && it.type?.startsWith("audio/") == true } ?: listOf()
        setTracks(docs)
    }

    private fun setTracks(docs: List<DocumentFile>) {

        lifecycleScope.launch {  }
    }

    @Composable
    fun TrackRow(doc: DocumentFile, result: com.radwrld.resonance.data.db.TrackResult?) {
        Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = doc.name ?: doc.uri.toString(),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(6.dp))
                if (result == null) {
                    Row {
                        Text("Not analyzed")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { analyzeNow(doc.uri) }) { Text("Analyze") }
                    }
                } else {
                    Text("Valence: ${"%.2f".format(result.valence)}  Arousal: ${"%.2f".format(result.arousal)}")
                    val listType = object : TypeToken<List<TrackProcessor.WindowResult>>() {}.type
                    val windows = gson.fromJson<List<TrackProcessor.WindowResult>>(result.perWindowJson, listType)

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        windows.take(20).forEach { w ->
                            Box(modifier = Modifier
                                .width(8.dp)
                                .height((20 + (w.arousal * 20)).dp)
                                .padding(1.dp)) {  }
                        }
                    }
                }
            }
        }
    }

    private fun analyzeNow(uri: Uri) {
        lifecycleScope.launch {
            val tp = TrackProcessor(this@MainActivity, uri)
            tp.processTrack()
        }
    }
}