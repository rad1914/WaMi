// @path: app/src/main/java/com/radwrld/wami/ui/screen/WamiScreens.kt
package com.radwrld.wami.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.radwrld.wami.Constants
import com.radwrld.wami.ui.vm.ChatViewModel
import com.radwrld.wami.ui.vm.MessageViewModel
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun ChatScreen(
    nav: NavController,
    sessionViewModel: SessionViewModel,
    vm: ChatViewModel = viewModel()
) {
    val sid by sessionViewModel.sessionId.collectAsState()
    val chats by vm.chats.collectAsState()

    LaunchedEffect(sid) { sid?.let(vm::load) }

    LazyColumn(Modifier.fillMaxSize()) {
        items(chats) { chat ->
            ListItem(
                headlineContent = { Text(chat.name) },
                supportingContent = { Text(chat.jid) },
                leadingContent = {
                    AsyncImage(
                        model = "${Constants.BASE_URL}/avatar/${chat.jid}",
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate("chat/${chat.jid}") }
                    .padding(vertical = 4.dp)
            )
            Divider()
        }
    }
}

@Composable
fun MessageScreen(
    nav: NavController,
    jid: String,
    sessionViewModel: SessionViewModel,
    vm: MessageViewModel = viewModel()
) {
    val sid by sessionViewModel.sessionId.collectAsState()
    val msgs by vm.msgs.collectAsState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(sid, jid) { sid?.let { vm.load(it, jid) } }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            reverseLayout = true
        ) {
            items(msgs, key = { it.id }) {

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("${if (it.fromMe) "You: " else ""}${it.text ?: "[Media]"}")
                    if (it.reactions.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            it.reactions.forEach { (emoji, count) ->
                                Text("$emoji $count")
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type…") }
            )
            Button(
                onClick = {
                    if (sid != null && input.isNotBlank()) {
                        vm.send(sid!!, jid, input)
                        input = ""
                    }
                },
                enabled = sid != null
            ) { Text("Send") }
        }
    }
}

@Composable
fun QRScreen(
    nav: NavController,
    vm: SessionViewModel
) {
    val qr by vm.qrCode.collectAsState()
    val auth by vm.isAuth.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var sessionId by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter Session ID") },
            text = {
                OutlinedTextField(
                    value = sessionId,
                    onValueChange = { sessionId = it },
                    label = { Text("Session ID") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sessionId.isNotBlank()) {
                            vm.loginWithId(sessionId)
                            showDialog = false
                        }
                    }
                ) { Text("Login") }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    val bitmap = remember(qr) {
        qr?.substringAfter(",")?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            auth -> Button(onClick = { nav.navigate("chats") }) { Text("Enter Chats") }

            bitmap != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scan this QR")
                Spacer(Modifier.height(16.dp))
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(250.dp))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDialog = true }) { Text("Login with Session ID") }
            }

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDialog = true }) { Text("Login with Session ID") }
            }
        }
    }
}
