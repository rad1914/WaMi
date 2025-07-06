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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.radwrld.wami.R
import com.radwrld.wami.network.Contact
import com.radwrld.wami.network.Message
// --- IMPORTACIONES CLAVE ---
// Ahora importamos las clases de estado desde el paquete del ViewModel
import com.radwrld.wami.ui.viewmodel.ConversationUiItem
import com.radwrld.wami.ui.viewmodel.MainScreenUiState
import com.radwrld.wami.ui.viewmodel.SearchResultItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainScreenUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit,
    onShowDeleteDialog: (Contact?) -> Unit,
    onDeleteConversation: (Contact) -> Unit,
    onShowMenuChanged: (Boolean) -> Unit,
    onOpenChat: (Contact) -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onFastContact: () -> Unit,
    onNavigateToTweaks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNewGroup: () -> Unit,
    onRefresh: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    uiState.contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { onShowDeleteDialog(null) },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete the conversation with ${contact.name}?") },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteConversation(contact) }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { onShowDeleteDialog(null) }) { Text("Cancel") }
            }
        )
    }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) { onRefresh() }
    }

    if (!uiState.isLoading && pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) { pullToRefreshState.endRefresh() }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                uiState = uiState,
                focusRequester = focusRequester,
                onSearchQueryChanged = onSearchQueryChanged,
                onSearchActiveChanged = onSearchActiveChanged,
                onShowMenuChanged = onShowMenuChanged,
                onFastContact = onFastContact,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToNewGroup = onNavigateToNewGroup
            )
        },
        bottomBar = {
            if (!uiState.isSearchActive) {
                PillBottomNavigation(
                    onNavigateToSocial = onNavigateToSocial,
                    onNavigateToContacts = onNavigateToContacts,
                    onNavigateToTweaks = onNavigateToTweaks
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            if (uiState.isSearchActive) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = uiState.searchState.results,
                        key = { result ->
                            when (result) {
                                is SearchResultItem.ContactItem -> "search-contact-${result.contact.id}"
                                is SearchResultItem.MessageItem -> "search-message-${result.message.id}"
                            }
                        }
                    ) { result ->
                        when (result) {
                            is SearchResultItem.ContactItem -> SearchContactItem(
                                item = result.contact,
                                onClick = { onOpenChat(result.contact) }
                            )
                            is SearchResultItem.MessageItem -> SearchMessageItem(
                                item = result,
                                onClick = { onOpenChat(result.contact) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = uiState.conversationState.conversations,
                        key = { item -> "conversation-${item.contact.id}" }
                    ) { item ->
                        ConversationItem(
                            item = item,
                            onClick = { onOpenChat(item.contact) },
                            onLongClick = { onShowDeleteDialog(item.contact) }
                        )
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    uiState: MainScreenUiState,
    focusRequester: FocusRequester,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit,
    onShowMenuChanged: (Boolean) -> Unit,
    onFastContact: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNewGroup: () -> Unit
) {
    TopAppBar(
        title = {
            if (uiState.isSearchActive) {
                OutlinedTextField(
                    value = uiState.searchState.query,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Text("Messages")
            }
        },
        navigationIcon = {
            if (uiState.isSearchActive) {
                IconButton(onClick = { onSearchActiveChanged(false) }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            } else {
                IconButton(onClick = onFastContact) {
                    Icon(Icons.Default.Add, "Fast Contact")
                }
            }
        },
        actions = {
            if (uiState.isSearchActive) {
                if (uiState.searchState.query.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, "Clear Search")
                    }
                }
            } else {
                IconButton(onClick = { onSearchActiveChanged(true) }) {
                    Icon(Icons.Default.Search, "Search")
                }
                IconButton(onClick = onNavigateToProfile) {
                    Icon(painterResource(id = R.drawable.ic_profile_placeholder), "User Profile")
                }
                IconButton(onClick = { onShowMenuChanged(true) }) {
                    Icon(Icons.Default.MoreVert, "More Options")
                }
                DropdownMenu(
                    expanded = uiState.isMenuExpanded,
                    onDismissRequest = { onShowMenuChanged(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            onNavigateToSettings()
                            onShowMenuChanged(false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Group") },
                        onClick = {
                            onNavigateToNewGroup()
                            onShowMenuChanged(false)
                        }
                    )
                }
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BaseListItem(
    modifier: Modifier = Modifier,
    title: String,
    imageUrl: String?,
    isGroup: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholder = if (isGroup) painterResource(id = R.drawable.ic_group_placeholder)
        else painterResource(id = R.drawable.ic_profile_placeholder)

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
            placeholder = placeholder,
            error = placeholder,
            contentDescription = "Avatar for $title",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
        content()
    }
}

@Composable
private fun SearchContactItem(item: Contact, onClick: () -> Unit) {
    BaseListItem(
        title = item.name,
        imageUrl = item.avatarUrl,
        isGroup = item.isGroup,
        onClick = onClick
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchMessageItem(item: SearchResultItem.MessageItem, onClick: () -> Unit) {
    val now = remember { Calendar.getInstance() }
    BaseListItem(
        title = item.contact.name,
        imageUrl = item.contact.avatarUrl,
        isGroup = item.contact.isGroup,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(text = item.contact.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = item.message.text ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        item.message.timestamp.let {
            val formattedTime = remember(it, now) { formatTimestamp(it, now) }
            Text(text = formattedTime, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConversationItem(
    item: ConversationUiItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val now = remember { Calendar.getInstance() }
    BaseListItem(
        title = item.contact.name,
        imageUrl = item.contact.avatarUrl,
        isGroup = item.contact.isGroup,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(text = item.contact.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = item.lastMessage?.text ?: "Start chatting...",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            item.contact.lastMessageTimestamp?.let {
                val formattedTime = remember(it, now) { formatTimestamp(it, now) }
                Text(text = formattedTime, style = MaterialTheme.typography.bodySmall)
            }
            if (item.contact.unreadCount > 0) {
                Badge { Text(item.contact.unreadCount.toString()) }
            }
        }
    }
}

@Composable
private fun PillBottomNavigation(
    modifier: Modifier = Modifier,
    onNavigateToSocial: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToTweaks: () -> Unit
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillNavigationButton(Icons.Default.Chat, "Messages", true) { /* No-op */ }
                    PillNavigationButton(Icons.Default.Group, "Social", false, onNavigateToSocial)
                    PillNavigationButton(Icons.Default.People, "Contacts", false, onNavigateToContacts)
                    PillNavigationButton(Icons.Default.Build, "Tweaks", false, onNavigateToTweaks)
                }
            }
        }
    }
}

@Composable
private fun RowScope.PillNavigationButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .size(40.dp)
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

private fun formatTimestamp(ts: Long, now: Calendar): String {
    val msgCal = Calendar.getInstance().apply { timeInMillis = ts }

    val isSameDay = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

    val isYesterday = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1

    return when {
        isSameDay -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
        isYesterday -> "Yesterday"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ts))
    }
}
