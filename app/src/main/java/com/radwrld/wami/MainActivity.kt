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
import com.radwrld.wami.network.SyncService
import com.radwrld.wami.ui.screens.MainScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.ConversationListViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ConversationListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(Intent(this, SyncService::class.java).setAction(SyncService.ACTION_START))

        setContent {
            WamiTheme {
                val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()

                MainScreen(
                    conversationState = conversationState,
                    searchState = searchState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onRefresh = viewModel::load,
                    onConversationClick = ::openChat,
                    onConversationHide = { contact ->
                        viewModel.hide(contact.id)
                        Toast.makeText(this, "Hidden", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onNavigateToSocial = { startActivity(Intent(this, SocialActivity::class.java)) },
                    onNavigateToContacts = { startActivity(Intent(this, ContactsActivity::class.java)) },
                    onNavigateToTweaks = { startActivity(Intent(this, TweaksActivity::class.java)) }
                )
            }
        }
    }

    private fun openChat(contact: Contact) {

        if (viewModel.searchState.value.query.isNotBlank()) {
            viewModel.onSearchQueryChanged("")
        }
        
        startActivity(
            Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_JID", contact.id)
                putExtra("EXTRA_NAME", contact.name)
                putExtra("EXTRA_AVATAR_URL", contact.avatarUrl)
            }
        )
    }

}
