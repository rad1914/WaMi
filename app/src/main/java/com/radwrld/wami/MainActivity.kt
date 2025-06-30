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
import com.radwrld.wami.ui.screens.MainScreen // Importa la pantalla desde el otro archivo
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.ConversationListViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ConversationListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WamiTheme {
                val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()

                // La Activity solo llama a la pantalla. Punto.
                MainScreen(
                    conversationState = conversationState,
                    searchState = searchState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onRefresh = viewModel::load,
                    onHideConversation = { jid ->
                        viewModel.hide(jid)
                        Toast.makeText(this, "Hidden", Toast.LENGTH_SHORT).show()
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
                    }
                )
            }
        }
    }
}