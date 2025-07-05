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

                ContactsScreen(
                    uiState = uiState,
                    onContactClick = { contact ->

                        
                        Toast.makeText(this, "${contact.name} clicked", Toast.LENGTH_SHORT).show()
                    },

                    onRefresh = viewModel::refreshContacts,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
