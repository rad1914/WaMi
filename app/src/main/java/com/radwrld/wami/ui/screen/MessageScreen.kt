// @path: app/src/main/java/com/radwrld/wami/ui/screen/MessageScreen.kt
package com.radwrld.wami.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.radwrld.wami.ui.vm.MessageViewModel
import com.radwrld.wami.ui.vm.SessionViewModel

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
            Modifier.weight(1f).padding(8.dp),
            reverseLayout = true
        ) {
            items(msgs, key = { it.id }) {
                Text((if (it.fromMe) "You: " else "") + (it.text ?: "[Media]"))
            }
        }

        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type…") }
            )
            Button(
                onClick = {
                    sid?.takeIf { input.isNotBlank() }?.let {
                        vm.send(it, jid, input)
                        input = ""
                    }
                },
                enabled = sid != null
            ) { Text("Send") }
        }
    }
}
