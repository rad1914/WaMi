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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.radwrld.wami.Constants
import com.radwrld.wami.data.MessageStatus
import com.radwrld.wami.ui.vm.ChatViewModel
import com.radwrld.wami.ui.vm.MessageViewModel
import com.radwrld.wami.ui.vm.SessionUiState
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun QRScreen(nav: NavController, vm: SessionViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState) {
        if (uiState is SessionUiState.Authenticated) {
            nav.navigate("chats") {
                popUpTo("qr") { inclusive = true }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter Session ID") },
            text = {
                OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Session ID") })
            },
            confirmButton = {
                Button(onClick = {
                    vm.loginWithId(input)
                    showDialog = false
                }) { Text("Login") }
            },
            dismissButton = { Button(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (val state = uiState) {
                is SessionUiState.Loading -> CircularProgressIndicator()
                is SessionUiState.AwaitingScan -> {
                    val bmp = remember(state.qrCode) {
                        state.qrCode?.substringAfter(",")?.let {
                            runCatching {
                                val bytes = Base64.decode(it, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            }.getOrNull()
                        }
                    }
                    if (bmp != null) {
                        Text("Scan this QR")
                        Spacer(Modifier.height(16.dp))
                        Image(bitmap = bmp, contentDescription = "QR Code", modifier = Modifier.size(250.dp))
                    } else {
                        Text("Generating QR Code...")
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showDialog = true }) { Text("Login with ID") }
                }
                is SessionUiState.Authenticated -> {
                     Text("Authenticated! Redirecting...")
                }
                is SessionUiState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.start() }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(nav: NavController, vm: ChatViewModel = hiltViewModel()) {
    val chats by vm.chats.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(Modifier.fillMaxSize()) {
        items(chats) { chat ->
            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = "${Constants.BASE_URL}/avatar/${chat.jid}",
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                },
                headlineContent = { Text(chat.name) },
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
    val msgs by vm.getMessages(jid).collectAsState(initial = emptyList())
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(jid) {
        vm.load(jid)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(8.dp),
            reverseLayout = true
        ) {
            items(msgs, key = { it.id }) { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.fromMe) Alignment.End else Alignment.Start
                ) {
                    Text("${if (msg.fromMe) "You: " else ""}${msg.text ?: "[Media]"}")
                    if (msg.fromMe) {
                        val statusText = when (msg.status) {
                            MessageStatus.SENDING -> "Sending..."
                            MessageStatus.FAILED -> "Failed"
                            MessageStatus.SENT -> ""
                        }
                        if(statusText.isNotEmpty()) {
                            Text(statusText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (msg.reactions.isNotEmpty()) Text("Reactions: ${msg.reactions}")
                }
                 Spacer(Modifier.height(8.dp))
            }
        }

        Row(
            Modifier.padding(8.dp).fillMaxWidth(),
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
            ) {
                Text("Send")
            }
        }
    }
}