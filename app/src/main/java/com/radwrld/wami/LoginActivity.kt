// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.UnknownHostException

class LoginActivity : AppCompatActivity() {
    private lateinit var qrImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var importButton: TextView
    private lateinit var waApi: WhatsAppApi
    private lateinit var config: ServerConfigStorage
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { uri -> importSession(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Views and Storage
        qrImage = findViewById(R.id.qrImage)
        statusText = findViewById(R.id.statusText)
        importButton = findViewById(R.id.importSessionButton)
        config = ServerConfigStorage(this)
        waApi = ApiClient.getInstance(this)

        // Setup Click Listeners
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        importButton.setOnClickListener {
            pickFileLauncher.launch("application/zip")
        }

        // --- Offline-First Logic ---
        // 1. Check for a valid, existing session saved locally.
        if (config.isLoggedIn() && !config.getSessionId().isNullOrEmpty()) {
            // If session is valid, go directly to MainActivity. No network required.
            statusText.text = "Loading from local storage..."
            launchMainActivity()
        }
        // 2. If no local session, check for an internet connection.
        else if (!isNetworkAvailable()) {
            // If offline, inform the user they need to connect.
            statusText.text = "Please connect to the internet to log in."
            qrImage.isVisible = false
            importButton.isVisible = false
        }
        // 3. If no local session AND online, start the remote authentication process.
        else {
            startAuthFlow()
        }
    }

    private fun startAuthFlow() = lifecycleScope.launch {
        // If there's no session ID at all, create a new one.
        if (config.getSessionId().isNullOrEmpty()) {
            statusText.text = "Creating new session..."
            try {
                val session = waApi.createSession()
                config.saveSessionId(session.sessionId)
                // Re-initialize ApiClient to ensure it uses the new session token.
                ApiClient.close()
                waApi = ApiClient.getInstance(this@LoginActivity)
            } catch (e: Exception) {
                handleError(e)
                return@launch
            }
        }
        // Begin polling for QR code and connection status.
        pollStatus()
    }

    private fun pollStatus() {
        if (isPolling) return
        if (!isNetworkAvailable()) {
            handleError(UnknownHostException())
            return
        }
        isPolling = true

        lifecycleScope.launch {
            try {
                val status = waApi.getStatus()
                if (status.connected) {
                    config.saveLoginState(true)
                    launchMainActivity()
                } else {
                    status.qr?.let { drawQr(it) }
                    statusText.text = "Scan QR code with WhatsApp"
                    // Continue polling after a delay.
                    handler.postDelayed({ isPolling = false; pollStatus() }, 5000)
                }
            } catch (e: Exception) {
                isPolling = false
                handleError(e)
            }
        }
    }

    private fun importSession(uri: Uri) = lifecycleScope.launch {
        statusText.text = "Importing session..."
        isPolling = false
        handler.removeCallbacksAndMessages(null) // Stop any ongoing polling

        try {
            // A new session ID is required on the server for an import.
            val session = waApi.createSession()
            config.saveSessionId(session.sessionId)
            ApiClient.close()
            waApi = ApiClient.getInstance(this@LoginActivity)

            val bytes = contentResolver.openInputStream(uri)?.readBytes()
            if (bytes == null) {
                showToast("Failed to read session file.")
                startAuthFlow()
                return@launch
            }

            val requestBody = bytes.toRequestBody("application/zip".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("sessionFile", "session.zip", requestBody)

            waApi.importSession(body)
            showToast("Import successful. Connecting...")
            handler.postDelayed({ pollStatus() }, 3000)

        } catch (e: Exception) {
            showToast("Import failed. Please try again.")
            config.saveSessionId(null)
            handler.postDelayed({ startAuthFlow() }, 1000)
        }
    }

    private fun handleError(e: Exception) {
        when (e) {
            is HttpException -> {
                if (e.code() == 401) { // Unauthorized/Invalid Session
                    showToast("Session invalid. Creating a new one.")
                    config.saveSessionId(null)
                    handler.postDelayed({ startAuthFlow() }, 1000)
                } else {
                     statusText.text = "Server Error. Retrying..."
                     handler.postDelayed({ isPolling = false; pollStatus() }, 10000)
                }
            }
            is UnknownHostException -> {
                statusText.text = "Offline. Check connection and try again."
                showToast("No internet connection")
                handler.postDelayed({ isPolling = false; pollStatus() }, 15000)
            }
            else -> {
                statusText.text = "An error occurred. Retrying..."
                handler.postDelayed({ isPolling = false; pollStatus() }, 10000)
            }
        }
    }

    private fun drawQr(data: String) {
        try {
            val base64 = data.substringAfter(",")
            val decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            qrImage.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.size))
            qrImage.isVisible = true
        } catch (_: Exception) {
            statusText.text = "Failed to display QR Code"
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    private fun launchMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        // Stop polling when the activity is not visible.
        isPolling = false
        handler.removeCallbacksAndMessages(null)
    }
}
