// @path: app/src/main/java/com/radwrld/wami/ui/screens/MainScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.radwrld.wami.R
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message
import com.radwrld.wami.ui.viewmodel.ConversationListState
import com.radwrld.wami.ui.viewmodel.ConversationUiItem
import com.radwrld.wami.ui.viewmodel.SearchState
import com.radwrld.wami.ui.viewmodel.SearchResultItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    conversationState: ConversationListState,
    searchState: SearchState,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onHideConversation: (String) -> Unit,
    onOpenChat: (Contact) -> Unit,
    onNavigateToContacts: () -> Unit
) {
    Scaffold(
        topBar = {
             TopAppBar(
                title = { Text("Messages") },
                actions = { IconButton(onClick = {  }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                      }
                }
            )
        },
        bottomBar = { AppBottomNavigation(onNavigateToContacts = onNavigateToContacts) }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = conversationState.isLoading),
            onRefresh = onRefresh,
             modifier = Modifier.padding(paddingValues)
        ) {

            val itemsToShow: List<Any> = if (searchState.query.isBlank()) {
                conversationState.conversations
            } else {
                searchState.results
            }

             LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = itemsToShow,
                     key = { item ->
                        when (item) {
                            is ConversationUiItem -> "conversation-${item.contact.id}"
                            is SearchResultItem.ContactItem -> "search-contact-${item.contact.id}"
                            is SearchResultItem.MessageItem -> "search-message-${item.message.id}"
                            else -> UUID.randomUUID().toString()
                        }
                     }
                ) { item ->

                    when (item) {
                        is ConversationUiItem -> {
                             ConversationItem(
                                item = item,
                                onClick = { onOpenChat(item.contact) },
                                onLongClick = { onHideConversation(item.contact.id) }
                            )
                        }
                        is SearchResultItem.ContactItem -> {
                             SearchResultContact(contact = item.contact)
                        }
                        is SearchResultItem.MessageItem -> {
                             SearchResultMessage(message = item.message, contact = item.contact)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(onNavigateToContacts: () -> Unit) {
     var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Messages", "Social", "Contacts")
    val icons = listOf(Icons.Filled.Message, Icons.Filled.Group, Icons.Filled.People)

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                 selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    if (item == "Contacts") onNavigateToContacts()
                }
             )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    item: ConversationUiItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholder = if (item.contact.isGroup) painterResource(id = R.drawable.ic_group_placeholder)
                          else painterResource(id = R.drawable.ic_profile_placeholder)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(item.contact.avatarUrl).crossfade(true).build(),
            placeholder = placeholder,
             error = placeholder,
            contentDescription = "${item.contact.name} avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(CircleShape)
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(text = item.contact.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = item.lastMessage?.text ?: "Tap to start chatting",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            item.contact.lastMessageTimestamp?.let {
                val formattedTime = remember(it) { formatTimestamp(it) }
                Text(text = formattedTime, style = MaterialTheme.typography.bodySmall)
            }
            if (item.contact.unreadCount > 0) {
                Badge { Text(item.contact.unreadCount.toString()) }
            }
        }
    }
}

@Composable
private fun SearchResultContact(contact: Contact) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = contact.name, fontWeight = FontWeight.Bold)
        Text(text = "Contacto", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SearchResultMessage(message: Message, contact: Contact) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = message.text ?: "Mensaje vacío", style = MaterialTheme.typography.bodyLarge)
        Text(text = "En chat con ${contact.name}", style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatTimestamp(ts: Long): String {
    val msgCal = Calendar.getInstance().apply { timeInMillis = ts }
    val now = Calendar.getInstance()
     return when {
        now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
        now.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1 -> "Ayer"
        else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(ts))
    }
}