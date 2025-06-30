// @path: app/src/main/java/com/radwrld/wami/ui/screens/MainScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.radwrld.wami.R
import com.radwrld.wami.network.Contact
import com.radwrld.wami.ui.viewmodel.ConversationState
import com.radwrld.wami.ui.viewmodel.SearchState
import com.radwrld.wami.ui.viewmodel.SearchResultItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    conversationState: ConversationState,
    searchState: SearchState,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onConversationClick: (Contact) -> Unit,
    onConversationHide: (Contact) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToTweaks: () -> Unit,
) {
    var isSearchActive by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = conversationState.isLoading,
        onRefresh = onRefresh
    )

    var contactToHide by remember { mutableStateOf<Contact?>(null) }

    Scaffold(
        topBar = {
            MainTopAppBar(
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                searchValue = searchState.query,
                onSearchValueChange = onSearchQueryChanged,
                onNavigateToSettings = onNavigateToSettings
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (searchState.query.isBlank()) {
                        items(conversationState.conversations, key = { it.id }) { conversation ->
                            ConversationItem(
                                contact = conversation,
                                onClick = { onConversationClick(conversation) },
                                onLongClick = { contactToHide = conversation }
                            )
                        }
                    } else {

                        items(searchState.results, key = {

                            when (it) {
                                is SearchResultItem.ContactItem -> "contact_${it.contact.id}"
                                is SearchResultItem.MessageItem -> "message_${it.message.id}"
                            }
                        }) { result ->
                            when (result) {
                                is SearchResultItem.ContactItem -> {

                                    ConversationItem(
                                        contact = result.contact,
                                        onClick = { onConversationClick(result.contact) },
                                        onLongClick = {  }
                                    )
                                }
                                is SearchResultItem.MessageItem -> {

                                    ConversationItem(
                                        contact = result.contact,
                                        onClick = { onConversationClick(result.contact) }
                                    )
                                }
                            }
                        }

                    }
                }

                PullRefreshIndicator(
                    refreshing = conversationState.isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            PillNavBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                onSocialClick = onNavigateToSocial,
                onContactsClick = onNavigateToContacts,
                onTweaksClick = onNavigateToTweaks
            )
        }
    }

    if (contactToHide != null) {
        AlertDialog(
            onDismissRequest = { contactToHide = null },
            title = { Text("Hide Conversation") },
            text = { Text("Hide conversation with ${contactToHide!!.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConversationHide(contactToHide!!)
                        contactToHide = null
                    }
                ) { Text("Hide") }
            },
            dismissButton = {
                TextButton(onClick = { contactToHide = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchValue: String,
    onSearchValueChange: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    TopAppBar(
        title = { if (!isSearchActive) Text("Messages") },
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = { onSearchActiveChange(false) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close Search")
                }
            } else {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(painterResource(R.drawable.ic_profile_placeholder), contentDescription = "User Profile")
                }
            }
        },
        actions = {
            if (isSearchActive) {
                TextField(
                    value = searchValue,
                    onValueChange = onSearchValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { onSearchValueChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                )
            } else {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        }
    )
}

@Composable
fun PillNavBar(
    modifier: Modifier = Modifier,
    onSocialClick: () -> Unit,
    onContactsClick: () -> Unit,
    onTweaksClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillNavButton(iconRes = R.drawable.ic_chat_circle_dots, description = "Messages", isSelected = true) {}
            PillNavButton(iconRes = R.drawable.ic_group_placeholder, description = "Groups & Social", onClick = onSocialClick)
            PillNavButton(iconRes = R.drawable.ic_users, description = "Contacts", onClick = onContactsClick)
            PillNavButton(iconRes = R.drawable.ic_add, description = "Tweaks", onClick = onTweaksClick)
        }
    }
}

@Composable
fun RowScope.PillNavButton(
    iconRes: Int,
    description: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    contact: Contact,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.width(16.dp))
        Text(contact.name, style = MaterialTheme.typography.bodyLarge)
    }
}
