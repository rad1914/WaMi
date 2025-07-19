// @path: app/src/main/java/com/radwrld/wami/ui/screen/WamiScreens.kt
package com.radwrld.wami.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
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
import com.radwrld.wami.Constants
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
                        model = "${Constants.BASE_URL}/avatar/${chat.jid}", 
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

    LaunchedEffect(jid) { vm.load(jid) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp) 
        ) {
            items(msgs, key = { it.id }) { msg ->
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isOutgoing) Alignment.End else Alignment.Start 
                ) { 
                    Text("${if (msg.isOutgoing) "You: " else ""}${msg.text ?: "[Media]"}")

                    if (msg.isOutgoing) {
                        msg.status?.let {
                            val label = when (it) {
                                MessageStatus.SENDING -> "Sending..." 
                                MessageStatus.FAILED -> "Failed" 
                                MessageStatus.SENT -> "Sent" 
                            }
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    } 

                    if (msg.reactions.isNotEmpty()) {
                        val r = msg.reactions.entries.joinToString(" ") { "${it.key} ${it.value}" }
                        Text("Reactions: $r", style = MaterialTheme.typography.bodySmall) 
                    }

                    if (!msg.isOutgoing) {
                       TextButton(onClick = {
                           sid?.let { sessionId ->
                               vm.sendReaction(sessionId, msg.jid, msg.id, "👍")
                           }
                       },
                       modifier = Modifier.height(36.dp)) {
                           Text("👍")
                       }
                    }
                }
                Spacer(Modifier.height(8.dp))
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
                placeholder = { Text("Type…") }, 
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    sid?.let { 
                        vm.send(it, jid, input) 
                        input = "" 
                    }
                },
                enabled = input.isNotBlank() && sid != null 
            ) { Text("Send") }
        }
    }
}
