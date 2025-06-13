// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var waApi: WhatsAppApi
    private lateinit var config: ServerConfigStorage
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { importSession(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        qrImage = findViewById(R.id.qrImage)
        statusText = findViewById(R.id.statusText)
        config = ServerConfigStorage(this)

        waApi = ApiClient.getInstance(this)

        setupClickListeners()
        startAuthFlow()
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<TextView>(R.id.importSessionButton).setOnClickListener {
            pickFileLauncher.launch("application/zip")
        }
    }

    private fun reinitApi() {
        ApiClient.close() // Clear old instances
        waApi = ApiClient.getInstance(this)
    }

    private fun importSession(uri: Uri) {
        lifecycleScope.launch {
            statusText.text = "Importing session..."
            qrImage.setImageDrawable(null)
            isPolling = false
            handler.removeCallbacksAndMessages(null)

            try {
                // 1. Create a new empty session on the server
                val sessionResponse = waApi.createSession()
                val newSessionId = sessionResponse.sessionId
                config.saveSessionId(newSessionId)
                Log.d("LoginActivity", "Created new session for import: $newSessionId")
                
                // Re-initialize API client to use the new auth token for the next request
                reinitApi()

                // 2. Prepare the file for upload
                val fileContent = contentResolver.openInputStream(uri)?.readBytes()
                if (fileContent == null) {
                    Toast.makeText(this@LoginActivity, "Failed to read file.", Toast.LENGTH_SHORT).show()
                    startAuthFlow()
                    return@launch
                }
                
                val requestFile = fileContent.toRequestBody("application/zip".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("sessionFile", "session.zip", requestFile)

                // 3. Upload to the import endpoint
                val response = waApi.importSession(body)
                if (response.isSuccessful) {
                    Toast.makeText(this@LoginActivity, "Import successful. Connecting...", Toast.LENGTH_SHORT).show()
                    pollStatus()
                } else {
                    val error = response.errorBody()?.string() ?: "Failed to import."
                    Toast.makeText(this@LoginActivity, "Import failed: $error", Toast.LENGTH_LONG).show()
                    config.saveSessionId(null)
                    startAuthFlow()
                }

            } catch (e: Exception) {
                Log.e("LoginActivity", "Import process failed", e)
                Toast.makeText(this@LoginActivity, "Import error: ${e.message}", Toast.LENGTH_LONG).show()
                config.saveSessionId(null)
                startAuthFlow()
            }
        }
    }
    
    private fun startAuthFlow() {
        lifecycleScope.launch {
            if (config.getSessionId().isNullOrEmpty()) {
                statusText.text = "Creating new session..."
                try {
                    val sessionResponse = waApi.createSession()
                    config.saveSessionId(sessionResponse.sessionId)
                    Log.d("LoginActivity", "New session created: ${sessionResponse.sessionId}")
                    reinitApi()
                } catch (e: Exception) {
                    statusText.text = "Failed to create session. Please restart."
                    Log.e("LoginActivity", "Session creation failed", e)
                    return@launch
                }
            }
            pollStatus()
        }
    }

    private fun pollStatus() {
        if (isPolling) return
        isPolling = true
        statusText.text = "Checking connection status..."

        lifecycleScope.launch {
            try {
                val status = waApi.getStatus()
                if (status.connected) {
                    navigateToMain()
                } else {
                    if (!status.qr.isNullOrBlank()) {
                        drawQr(status.qr)
                        statusText.text = "Scan this with WhatsApp to log in"
                    } else {
                        qrImage.setImageDrawable(null)
                        statusText.text = "..."
                    }
                    handler.postDelayed({ isPolling = false; pollStatus() }, 5_000L)
                }
            } catch (e: Exception) {
                isPolling = false
                handleApiError(e)
            }
        }
    }

    private fun handleApiError(e: Exception) {
        when {
            e is HttpException && e.code() == 401 -> {
                Log.w("LoginActivity", "Session token is invalid (401). Resetting.")
                Toast.makeText(this, "Session expired. Creating a new one.", Toast.LENGTH_SHORT).show()
                config.saveSessionId(null)
                handler.postDelayed({ startAuthFlow() }, 1000L)
            }
            e is UnknownHostException -> {
                statusText.text = "Cannot connect to the server.\nUse the settings icon to change the IP."
                Toast.makeText(this@LoginActivity, "Please check your internet connection", Toast.LENGTH_LONG).show()
            }
            else -> {
                statusText.text = "Error connecting to server. Retrying..."
                Log.e("LoginActivity", "API Error", e)
                handler.postDelayed({ pollStatus() }, 10_000L)
            }
        }
    }

    private fun navigateToMain() {
        config.saveLoginState(true)
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        finish()
    }

    private fun drawQr(data: String) {
        try {
            val base64String = data.substring(data.indexOf(",") + 1)
            val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            qrImage.setImageBitmap(bmp)
        } catch (e: Exception) {
            statusText.text = "Failed to render QR code"
            Log.e("LoginActivity", "QR drawing failed", e)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
