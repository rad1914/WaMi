// @path: app/src/main/java/com/radwrld/wami/ui/screens/ChatScreen.kt
// @path: app/src/main/java/com/radwrld/wami/ui/screens/chat/ChatScreen.kt
package com.radwrld.wami.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.radwrld.wami.adapter.ChatListItem
import com.radwrld.wami.ui.viewmodel.ChatViewModel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ChatScreen(
    state: UiState,
    isGroup: Boolean,
    contactName: String,
    onNavigateBack: () -> Unit,
    onLoadOlder: () -> Unit,
    onSendText: (String) -> Unit,
    onSendMedia: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(state.loadingOlder, onLoadOlder)
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mensaje...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendText(text)
                            text = ""
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                reverseLayout = true
            ) {

                items(items = state.messages.reversed(), key = { it.key }) { item ->
                    when (item) {
                        is ChatListItem.MessageItem -> {

                            Text(
                                text = "${item.message.senderName}: ${item.message.text}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                textAlign = if (item.message.isOutgoing) TextAlign.End else TextAlign.Start
                            )
                        }
                        is ChatListItem.DividerItem -> {

                            DateDivider(timestamp = item.timestamp)
                        }
                        is ChatListItem.WarningItem -> {

                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.loadingOlder,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun DateDivider(timestamp: Long) {
    val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    Text(
        text = formatter.format(Date(timestamp)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}
