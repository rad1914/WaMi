// @path: app/src/main/java/com/radwrld/wami/ContactsActivity.kt
package com.radwrld.wami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.ui.screens.ContactItem
import com.radwrld.wami.ui.theme.WamiTheme
// CORRECCIÓN: Se importan el ViewModel y el State correctos
import com.radwrld.wami.ui.viewmodel.ContactsViewModel
import com.radwrld.wami.ui.viewmodel.ContactsUiState

class ContactsActivity : ComponentActivity() {

    // CORRECCIÓN: Se usa el ViewModel correcto
    private val viewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WamiTheme {
                // CORRECCIÓN: Se consume el state del ViewModel correcto
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ContactsScreen(
                    state = state,
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    // CORRECCIÓN: El estado ahora es del tipo correcto
    state: ContactsUiState,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // CORRECCIÓN: Se itera sobre `state.contacts` y se usa `contact.id`
                    items(
                        items = state.contacts,
                        key = { contact -> contact.id }
                    ) { contact ->
                        ContactItem(contact = contact)
                    }
                }
            }
            state.error?.let {
                Text(text = "Error: $it")
            }
        }
    }
}