// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.ui.screens.ContactsScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.ContactsViewModel

class ContactsActivity : ComponentActivity() {

    private val viewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WamiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Use the primary, feature-rich ContactsScreen
                ContactsScreen(
                    uiState = uiState,
                    onContactClick = { contact ->
                        // TODO: Implement navigation to the correct chat screen
                        // Example from MainActivity:
                        /*
                        startActivity(
                            Intent(this, ChatActivity::class.java).apply {
                                putExtra("EXTRA_JID", contact.id)
                                putExtra("EXTRA_NAME", contact.name)
                                putExtra("EXTRA_AVATAR_URL", contact.avatarUrl)
                            }
                        )
                        */
                        Toast.makeText(this, "${contact.name} clicked", Toast.LENGTH_SHORT).show()
                    },
                    // Assuming the ViewModel has a function to trigger a refresh
                    onRefresh = viewModel::refreshContacts,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}