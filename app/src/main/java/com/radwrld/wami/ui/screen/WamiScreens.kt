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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.radwrld.wami.Constants
import com.radwrld.wami.ui.vm.ChatViewModel
import com.radwrld.wami.ui.vm.MessageViewModel
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun QRScreen(nav: NavController, vm: SessionViewModel = viewModel()) {
    val qr by vm.qrCode.collectAsState()
    val auth by vm.isAuth.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    Log.d("QRScreen", "Authenticated: $auth")
    Log.d("QRScreen", "QR Code data: $qr")

    if (showDialog) {
        AlertDialog( onDismissRequest = { showDialog = false },
            title = { Text("Enter Session ID") },
            text  = {
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
                Button(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    val bmp = remember(qr) {
        qr?.substringAfter(",")?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Log.d("QRScreen", "Bitmap created: ${bmp != null}")

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Debug Info:")
            Text("Authenticated: $auth")
            Text("QR available: ${qr != null}")
            Text("Bitmap available: ${bmp != null}")
            Spacer(Modifier.height(16.dp))
            when {
                auth -> Button(onClick = { nav.navigate("chats") }) { Text("Enter Chats") }
                bmp != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scan this QR")
                    Spacer(Modifier.height(16.dp))
                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(250.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showDialog = true }) { Text("Login with ID") }
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showDialog = true }) { Text("Login with ID") }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(nav: NavController,
               vm: ChatViewModel = viewModel()
) {
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
    sessionVM: SessionViewModel = viewModel(),
    vm: MessageViewModel = viewModel()
) {
    val sid by sessionVM.sessionId.collectAsState()
    val msgs by vm.msgs.collectAsState()
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(sid, jid) { sid?.let { vm.load(jid) } }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            reverseLayout = true
        ) {
            items(msgs, key = { it.id }) { it ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text("${if (it.fromMe) "You: " else ""}${it.text ?: "[Media]"}")
                    if (it.reactions.isNotEmpty()) {
                        Text("Reactions: ${it.reactions}")
                    }
                }
            }
        }

        Row(
            Modifier
                .padding(8.dp)
                .fillMaxWidth(),
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
                    isSending = true
                    vm.send(sid!!, jid, input)
                    input = ""
                    isSending = false
                },
                enabled = input.isNotBlank() && sid != null
            ) {
                if (isSending) CircularProgressIndicator(Modifier.size(16.dp))
                else Text("Send")
            }
        }
    }
}
