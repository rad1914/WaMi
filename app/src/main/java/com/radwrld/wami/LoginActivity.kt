// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.network.SyncService
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.ui.screens.LoginScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.LoginUiState
import com.radwrld.wami.ui.viewmodel.LoginViewModel
import com.radwrld.wami.ui.viewmodel.LoginViewModelFactory

class LoginActivity : ComponentActivity() {

    private val vm by viewModels<LoginViewModel> {
        LoginViewModelFactory(application, ServerConfigStorage(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ServerConfigStorage(this).isLoggedIn()) {
            openMain()
            return
        }

        setContent {
            WamiTheme {
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState) {
                    if (uiState is LoginUiState.LoggedIn) {
                        openMain()
                    }
                }

                LoginScreen(
                    uiState = uiState,
                    toastEvents = vm.toastEvents,
                    onStart = vm::start,
                    onSetSession = vm::setSessionAndRestart,
                    onNavigateToSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                )
            }
        }
    }

    private fun openMain() {
        startService(Intent(this, SyncService::class.java).apply {
            action = SyncService.ACTION_START
        })
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
