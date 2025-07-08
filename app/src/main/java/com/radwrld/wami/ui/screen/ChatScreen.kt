// @path: app/src/main/java/com/radwrld/wami/ui/screen/ChatScreen.kt
package com.radwrld.wami.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.radwrld.wami.data.Chat
import com.radwrld.wami.ui.vm.ChatViewModel
import com.radwrld.wami.ui.vm.SessionViewModel
import com.radwrld.wami.Constants.BASE_URL
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(nav: NavController, vm: ChatViewModel = viewModel()) {

    val sessionViewModel: SessionViewModel = viewModel()
    val sid by sessionViewModel.sessionId.collectAsState()
    val chats by vm.chats.collectAsState()

    LaunchedEffect(Unit) {
        sessionViewModel.start()
    }

    LaunchedEffect(sid) {
        sid?.let { vm.load(it) }
    }

    Column(Modifier.fillMaxSize()) {

        val debugInfo = """
            DEBUG INFO:
            Session ID: $sid
            Chats Loaded: ${chats.size}
        """.trimIndent()

        Text(
            text = debugInfo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            color = Color.Gray,
            fontSize = 12.sp
        )

        Divider()

        LazyColumn(Modifier.weight(1f)) {
            items(chats) { chat ->
                ListItem(
                    headlineContent = { Text(chat.name) },
                    supportingContent = { Text(chat.jid) },
                    leadingContent = {
                        AsyncImage(
                            model = "$BASE_URL/avatar/${chat.jid}",
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
}
