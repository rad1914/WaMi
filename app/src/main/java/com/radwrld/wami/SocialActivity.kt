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
import com.radwrld.wami.repository.WhatsAppRepository
import com.radwrld.wami.ui.screens.SocialScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.SocialViewModel
import com.radwrld.wami.ui.viewmodel.SocialViewModelFactory

class SocialActivity : ComponentActivity() {

    private val repository by lazy { WhatsAppRepository(applicationContext) }
    private val viewModel: SocialViewModel by viewModels {
        SocialViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WamiTheme {
                val statuses by viewModel.statuses.collectAsStateWithLifecycle()

                SocialScreen(
                    statuses = statuses,
                    onFetchStatuses = viewModel::fetchStatuses,
                    onNavigateBack = { finish() },
                    onShowToast = { message -> showToast(message) },
                    onStatusClick = { status ->

                        val intent = Intent(this, MediaViewActivity::class.java).apply {
                           data = Uri.parse(status.mediaUrl)
                           type = status.mimetype ?: "image/*"
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
