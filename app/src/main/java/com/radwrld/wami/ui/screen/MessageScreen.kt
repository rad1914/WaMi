// @path: app/src/main/java/com/radwrld/wami/ui/screen/MessageScreen.kt
package com.radwrld.wami.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.radwrld.wami.data.Message
import com.radwrld.wami.ui.vm.MessageViewModel
import com.radwrld.wami.ui.vm.SessionViewModel

@Composable
fun MessageScreen(nav: NavController, jid: String, vm: MessageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val sid = androidx.lifecycle.viewmodel.compose.viewModel<SessionViewModel>().sessionId.collectAsState().value
        ?: return
    val msgs by vm.msgs.collectAsState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(jid) { vm.load(sid, jid) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(8.dp)) {
            items(msgs) { m: Message ->
                Text(
                    (if (m.fromMe) "You: " else "") + (m.text ?: "[Media]"), 
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type…") }
            ) 
            Button(onClick = {
                if (input.isNotBlank()) {
                    vm.send(sid, jid, input)
                    input = ""
                } 
            }) {
                Text("Send")
            }
        }
    }
}
