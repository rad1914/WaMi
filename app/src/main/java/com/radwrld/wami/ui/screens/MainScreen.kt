// @path: app/src/main/java/com/radwrld/wami/ui/screens/MainScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.radwrld.wami.R
import com.radwrld.wami.network.Contact
import com.radwrld.wami.ui.viewmodel.ConversationListState
import com.radwrld.wami.ui.viewmodel.ConversationUiItem
import com.radwrld.wami.ui.viewmodel.SearchState // <-- IMPORT ADDED
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    conversationState: ConversationListState,
    searchState: SearchState,
    onSearchQueryChanged: (String) -> Unit,
    onDeleteConversation: (Contact) -> Unit,
    onOpenChat: (Contact) -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onFastContact: () -> Unit,
    onNavigateToTweaks: () -> Unit,
    onRefresh: () -> Unit
) {

    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete the conversation with ${contact.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConversation(contact)
                        contactToDelete = null
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) { Text("Cancel") }
            }
        )
    }

    val isRefreshing = conversationState.isLoading
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onFastContact) {
                        Icon(Icons.Default.Add, "Fast Contact")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(painterResource(id = R.drawable.ic_profile_placeholder), "User Profile")
                    }
                    IconButton(onClick = { /* TODO: Implement more options menu */ }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(
                    items = conversationState.conversations,
                    key = { item -> "conversation-${item.contact.id}" }
                ) { item ->
                    ConversationItem(
                        item = item,
                        onClick = { onOpenChat(item.contact) },
                        onLongClick = { contactToDelete = item.contact }
                    )
                }
            }

            PillBottomNavigation(
                modifier = Modifier.align(Alignment.BottomCenter),
                onNavigateToMessages = { },
                onNavigateToSocial = onNavigateToSocial,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToTweaks = onNavigateToTweaks
            )

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PillBottomNavigation(
    modifier: Modifier = Modifier,
    onNavigateToMessages: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToTweaks: () -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }

    Card(
        modifier = modifier.padding(bottom = 24.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillNavigationButton(Icons.Default.Chat, "Messages", selectedItem == 0) {
                selectedItem = 0; onNavigateToMessages()
            }
            PillNavigationButton(Icons.Default.Group, "Groups & Social", selectedItem == 1) {
                selectedItem = 1; onNavigateToSocial()
            }
            PillNavigationButton(Icons.Default.People, "Contacts", selectedItem == 2) {
                selectedItem = 2; onNavigateToContacts()
            }
            PillNavigationButton(Icons.Default.Build, "Tweaks", selectedItem == 3) {
                selectedItem = 3; onNavigateToTweaks()
            }
        }
    }
}

@Composable
private fun RowScope.PillNavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .size(40.dp, 40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
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

private fun formatTimestamp(ts: Long): String {
    val msgCal = Calendar.getInstance().apply { timeInMillis = ts }
    val now = Calendar.getInstance()
    return when {
        now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
        now.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1 -> "Yesterday"
        else -> SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(ts))
    }
}
