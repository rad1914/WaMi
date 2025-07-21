// @path: app/src/main/java/com/radwrld/wami/ui/screen/WamiScreens.kt
package com.radwrld.wami.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.radwrld.wami.data.Message
import com.radwrld.wami.data.MessageStatus
import com.radwrld.wami.ui.vm.*

@Composable
fun QRScreen(nav: NavController, vm: SessionViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is SessionUiState.Authenticated) {
            nav.navigate("chats") { popUpTo("qr") { inclusive = true } }
        }
    }

    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                is SessionUiState.Loading -> CircularProgressIndicator()
                is SessionUiState.AwaitingRegistration -> {
                    Text("Create or join a session", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Enter a Session ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { if (input.isNotBlank()) vm.registerSession(input.trim()) },
                        enabled = input.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register Session")
                    }
                }
                is SessionUiState.AwaitingScan -> {
                    Text("Session Registered!", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Scan the QR code printed in your server's console using your WhatsApp mobile app.",
                        textAlign = TextAlign.Center
                    )
                    CircularProgressIndicator()
                    Text("Waiting for connection...")
                }
                is SessionUiState.Authenticated -> {
                    Text("Authenticated! Redirecting…")
                }
                is SessionUiState.Error -> {
                    Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.start() }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(nav: NavController, vm: ChatViewModel = hiltViewModel()) {
    val chats by vm.chats.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.load() }

    if (chats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No chats found. Messages from new contacts will appear here.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            items(chats, key = { it.jid }) { chat ->
                ListItem(
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

    // Message history loading is no longer supported
    LaunchedEffect(jid) {
        vm.prepareForJid(jid)
    }

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (msgs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages. Start the conversation!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(msgs, key = { it.id }) { msg ->
                    MessageItem(message = msg)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
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
private fun MessageItem(message: Message) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isOutgoing)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text ?: "[Media]",
                modifier = Modifier.padding(12.dp),
                color = if (message.isOutgoing)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status display is simplified as backend response doesn't allow for confirmation
        if (message.isOutgoing && message.status == MessageStatus.SENDING) {
            Text(
                text = "Sending...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, end = 4.dp)
            )
        }
    }
}