// @path: app/src/main/java/com/radwrld/wami/ui/screen/WamiScreens.kt
package com.radwrld.wami.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.radwrld.wami.WamiApp // Para acceder a WamiApp.Constants
import com.radwrld.wami.data.Message
import com.radwrld.wami.data.MessageStatus
import com.radwrld.wami.ui.vm.*

@Composable
fun QRScreen(nav: NavController, vm: SessionViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is SessionUiState.Authenticated) {
            nav.navigate("chats") { popUpTo("qr") { inclusive = true } }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter Session ID") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Session ID") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.loginWithId(input)
                    showDialog = false
                }) { Text("Login") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (val s = state) {
                is SessionUiState.Loading -> CircularProgressIndicator()
                is SessionUiState.AwaitingScan -> {
                    val bmp = remember(s.qrCode) { qrCodeToImageBitmap(s.qrCode) }
                    Text("Scan this QR")
                    Spacer(Modifier.height(16.dp))
                    bmp?.let {
                        Image(bitmap = it, contentDescription = null, modifier = Modifier.size(250.dp))
                    } ?: CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showDialog = true }) { Text("Login with ID") }
                }
                is SessionUiState.Authenticated -> Text("Authenticated! Redirecting…")
                is SessionUiState.Error -> {
                    Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { vm.start() }) { Text("Retry") }
                }
            }
        }
    }
}

private fun qrCodeToImageBitmap(dataUrl: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val base64 = dataUrl?.substringAfter(",") ?: return null
    return runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun ChatScreen(nav: NavController, vm: ChatViewModel = hiltViewModel()) {
    val chats by vm.chats.collectAsState()
    val listState = rememberLazyListState()
    var visibleCount by remember { mutableStateOf(12) }

    LaunchedEffect(Unit) { vm.load() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect {
                if ((it.lastOrNull()?.index ?: 0) >= visibleCount - 3 && visibleCount < chats.size) {
                    visibleCount += 12
                }
            }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        items(chats.take(visibleCount)) { chat ->
            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = "${WamiApp.Constants.BASE_URL}/media/avatar/${chat.jid}",
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                },
                headlineContent = { Text(chat.name ?: chat.jid) },
                supportingContent = { Text(chat.jid) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate("chat/${chat.jid}") }
                    .padding(vertical = 4.dp)
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun MessageScreen(
    jid: String,
    sessionVM: SessionViewModel = hiltViewModel(),
    vm: MessageViewModel = hiltViewModel()
) {
    val sid by sessionVM.sessionId.collectAsState()
    val msgs by vm.msgs.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(jid) {
        Log.d("MessageScreen", "Loading messages for JID: $jid")
        vm.load(jid)
    }

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty() && msgs.first().isOutgoing) {
            listState.animateScrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            if (msgs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(msgs, key = { it.id }) { msg ->
                    MessageItem(
                        message = msg,
                        onReaction = { emoji ->
                            sid?.let { sessionId ->
                                vm.sendReaction(sessionId, msg.jid, msg.id, emoji)
                            }
                        },
                        onRetry = {
                            sid?.let { sessionId ->
                                vm.retryFailedMessage(msg, sessionId)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        sid?.let { sessionId ->
                            vm.send(sessionId, jid, input.trim())
                            input = ""
                        }
                    }
                },
                enabled = input.isNotBlank() && sid != null
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    onReaction: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = if (message.isOutgoing) 16.dp else 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isOutgoing)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text ?: "[Media]",
                modifier = Modifier.padding(12.dp),
                color = if (message.isOutgoing)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (message.isOutgoing) {
            message.status?.let { status ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (label, color) = when (status) {
                        MessageStatus.SENDING -> "Sending..." to MaterialTheme.colorScheme.outline
                        MessageStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
                        MessageStatus.SENT -> "Sent" to MaterialTheme.colorScheme.outline
                    }

                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )

                    if (status == MessageStatus.FAILED) {
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Retry", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            val reactionsText = message.reactions.entries.joinToString(" ") { "${it.key} ${it.value}" }
            Text(
                text = "Reactions: $reactionsText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (!message.isOutgoing) {
            TextButton(
                onClick = { onReaction("👍") },
                modifier = Modifier.height(36.dp)
            ) {
                Text("👍")
            }
        }
    }
}