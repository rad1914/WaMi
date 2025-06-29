// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt

package com.radwrld.wami

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radwrld.wami.network.SyncService
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.ui.screens.LoginScreen
import com.radwrld.wami.ui.theme.WamiTheme

class LoginActivity : ComponentActivity() {

    private val vm by viewModels<LoginViewModel> {
        LoginViewModelFactory(application, ServerConfigStorage(this), contentResolver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ServerConfigStorage(this).isLoggedIn()) {
            openMain(); return
        }

        setContent {
            WamiTheme {
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                LoginScreen(
                    uiState = uiState,
                    toastEvents = vm.toastEvents,
                    onStart = { if (isOnline()) vm.start() else vm.notifyNoNet() },
                    onSetSession = vm::setSessionAndRestart,
                    onNavigateToSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onLoggedIn = ::openMain
                )
            }
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(net)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun openMain() {
        startService(Intent(this, SyncService::class.java).apply {
            action = SyncService.ACTION_START
        })
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
