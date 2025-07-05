// @path: app/src/main/java/com/radwrld/wami/SocialActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.ui.screens.SocialScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.SocialViewModel

class SocialActivity : ComponentActivity() {

    private val viewModel: SocialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WamiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                SocialScreen(
                    statuses = uiState.statuses,
                    onFetchStatuses = viewModel::fetchStatuses,
                    onNavigateBack = { finish() },
                    onStatusClick = { status ->
                        val intent = Intent(this, MediaViewActivity::class.java).apply {
                           data = Uri.parse(status.mediaUrl)
                           type = status.mimetype ?: "image/*"
                        }
                        startActivity(intent)
                    },
                    onShowToast = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
