// @path: app/src/main/java/com/radwrld/wami/AboutActivity.kt
package com.radwrld.wami

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.ui.screens.AboutScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.AboutViewModel
import com.radwrld.wami.ui.viewmodel.AboutViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import android.content.Intent

class AboutActivity : ComponentActivity() {

    private val jid by lazy { intent.getStringExtra(EXTRA_JID).orEmpty() }

    private val viewModel: AboutViewModel by viewModels {
        AboutViewModelFactory(application, jid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (jid.isBlank()) {
            Toast.makeText(this, "Error: Contacto no encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            WamiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.uiState.collectLatest { state ->
                        state.error?.let { msg ->
                            Toast.makeText(this@AboutActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                AboutScreen(
                    uiState = uiState,
                    onNavigateBack = { finish() },
                    onSharedMediaClick = {
                        startActivity(
                            Intent(this, SharedMediaActivity::class.java).apply {
                                putExtra(EXTRA_JID, jid)
                            }
                        )
                    },
                    onBlockClick = viewModel::toggleBlockContact,
                    onReportClick = viewModel::reportContact
                )
            }
        }
    }

    companion object {
        const val EXTRA_JID = "EXTRA_JID"
    }
}
