// @path: app/src/main/java/com/radwrld/wami/SharedMediaActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.ui.screens.SharedMediaScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.SharedMediaViewModel
import com.radwrld.wami.ui.viewmodel.SharedMediaViewModelFactory

class SharedMediaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val jid = intent.getStringExtra("EXTRA_JID")
        if (jid.isNullOrBlank()) {
            finish()
            return
        }

        val viewModel: SharedMediaViewModel by viewModels {
            SharedMediaViewModelFactory(application, jid)
        }

        setContent {
            WamiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SharedMediaScreen(
                    uiState = uiState,
                    onMediaClick = { mediaItem ->
                        val intent = Intent(this, MediaViewActivity::class.java).apply {
                            data = Uri.parse(mediaItem.uri)
                            type = mediaItem.type
                        }
                        startActivity(intent)
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
