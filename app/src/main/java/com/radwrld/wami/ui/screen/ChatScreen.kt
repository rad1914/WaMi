// @path: app/src/main/java/com/radwrld/wami/ui/screen/ChatScreen.kt
package com.radwrld.wami.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.radwrld.wami.Constants.BASE_URL
import com.radwrld.wami.ui.vm.ChatViewModel
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
