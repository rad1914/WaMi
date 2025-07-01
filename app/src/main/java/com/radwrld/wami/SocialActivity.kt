// @path: app/src/main/java/com/radwrld/wami/SocialActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.radwrld.wami.network.StatusItem
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.SocialUiState
import com.radwrld.wami.ui.viewmodel.SocialViewModel

class SocialActivity : ComponentActivity() {

    private val viewModel: SocialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WamiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                SocialScreen(
                    uiState = uiState,
                    onRefresh = viewModel::fetchStatuses, // Ahora esto es válido
                    onNavigateBack = { finish() },
                    onStatusClick = { status ->
                        val intent = Intent(this, MediaViewActivity::class.java).apply {
                           data = Uri.parse(status.mediaUrl)
                           type = status.mimetype ?: "image/*"
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    uiState: SocialUiState,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    onStatusClick: (StatusItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estados") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = uiState.isLoading),
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding)
        ) {
            if (uiState.isLoading && uiState.statuses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.statuses, key = { it.id }) { status ->
                        StatusRow(status = status, onClick = { onStatusClick(status) })
                    }
                }
            }
        }
    }
}

@Composable
fun StatusRow(status: StatusItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(status.senderName ?: "Desconocido") },
        supportingContent = { Text(status.text ?: "Foto o video") },
        modifier = Modifier.clickable(onClick = onClick)
    )
}