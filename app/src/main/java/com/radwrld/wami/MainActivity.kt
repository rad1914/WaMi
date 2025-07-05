// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.network.Contact
import com.radwrld.wami.ui.screens.MainScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.ConversationListViewModel
import com.radwrld.wami.ui.viewmodel.ConversationListViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: ConversationListViewModel by viewModels {
        ConversationListViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WamiTheme {
                val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()

                MainScreen(
                    conversationState = conversationState,
                    searchState = searchState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onDeleteConversation = { contact ->
                        viewModel.deleteConversation(contact)
                        Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show()
                    },
                    onOpenChat = { contact ->
                        startActivity(
                            Intent(this, ChatActivity::class.java).apply {
                                putExtra("EXTRA_JID", contact.id)
                                putExtra("EXTRA_NAME", contact.name)
                                putExtra("EXTRA_AVATAR_URL", contact.avatarUrl)
                            }
                        )
                    },
                    onNavigateToContacts = {
                        startActivity(Intent(this, ContactsActivity::class.java))
                    },
                    onNavigateToProfile = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onNavigateToSocial = {
                        startActivity(Intent(this, SocialActivity::class.java))
                    },
                    onFastContact = {
                        Toast.makeText(this, "Fast Contact clicked", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToTweaks = {
                        startActivity(Intent(this, TweaksActivity::class.java))
                    },

                    onRefresh = viewModel::refreshConversations
                )
            }
        }
    }
}
