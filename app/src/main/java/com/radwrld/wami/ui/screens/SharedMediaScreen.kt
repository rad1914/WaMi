// @path: app/src/main/java/com/radwrld/wami/ui/screens/SharedMediaScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.radwrld.wami.ui.viewmodel.MediaItem
import com.radwrld.wami.ui.viewmodel.SharedMediaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaScreen(
    uiState: SharedMediaUiState,
    onMediaClick: (MediaItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multimedia Compartida") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->

        if (uiState.mediaItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay multimedia compartida", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(uiState.mediaItems, key = { it.uri }) { item ->
                    SharedMediaItem(
                        item = item,
                        onClick = { onMediaClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun SharedMediaItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        AsyncImage(
            model = item.uri,
            contentDescription = "Shared Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
