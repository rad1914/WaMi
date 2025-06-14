// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.widget.*
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

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { importSession(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        qrImage = findViewById(R.id.qrImage)
        statusText = findViewById(R.id.statusText)
        waApi = ApiClient.getInstance(this)
        config = ServerConfigStorage(this)

        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<TextView>(R.id.importSessionButton).setOnClickListener {
            pickFileLauncher.launch("application/zip")
        }

        startAuthFlow()
    }

    private fun startAuthFlow() = lifecycleScope.launch {
        if (config.getSessionId().isNullOrEmpty()) {
            statusText.text = "Creating session..."
            try {
                val session = waApi.createSession()
                config.saveSessionId(session.sessionId)
                waApi = ApiClient.getInstance(this@LoginActivity)
            } catch (e: Exception) {
                statusText.text = "Failed to create session"
                return@launch
            }
        }
        pollStatus()
    }

    private fun pollStatus() {
        if (isPolling) return
        isPolling = true

        lifecycleScope.launch {
            try {
                val status = waApi.getStatus()
                if (status.connected) {
                    config.saveLoginState(true)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    if (!status.qr.isNullOrBlank()) drawQr(status.qr)
                    statusText.text = "Scan QR with WhatsApp"
                    handler.postDelayed({ isPolling = false; pollStatus() }, 5000)
                }
            } catch (e: Exception) {
                isPolling = false
                handleError(e)
            }
        }
    }

    private fun importSession(uri: Uri) = lifecycleScope.launch {
        statusText.text = "Importing..."
        isPolling = false
        handler.removeCallbacksAndMessages(null)

        try {
            val session = waApi.createSession()
            config.saveSessionId(session.sessionId)
            waApi = ApiClient.getInstance(this@LoginActivity)

            val bytes = contentResolver.openInputStream(uri)?.readBytes()
            if (bytes == null) {
                showToast("Failed to read file")
                startAuthFlow()
                return@launch
            }

            val body = MultipartBody.Part.createFormData(
                "sessionFile", "session.zip", bytes.toRequestBody("application/zip".toMediaTypeOrNull())
            )

            val result = waApi.importSession(body)
            if (result.isSuccessful) {
                showToast("Import OK. Connecting...")
                pollStatus()
            } else {
                showToast("Import failed")
                config.saveSessionId(null)
                startAuthFlow()
            }
        } catch (e: Exception) {
            showToast("Import error")
            config.saveSessionId(null)
            startAuthFlow()
        }
    }

    private fun handleError(e: Exception) {
        when (e) {
            is HttpException -> {
                if (e.code() == 401) {
                    showToast("Session expired")
                    config.saveSessionId(null)
                    handler.postDelayed({ startAuthFlow() }, 1000)
                }
            }
            is UnknownHostException -> {
                statusText.text = "Can't connect. Check IP or internet."
                showToast("No connection")
            }
            else -> {
                statusText.text = "Error. Retrying..."
                handler.postDelayed({ pollStatus() }, 10000)
            }
        }
    }

    private fun drawQr(data: String) {
        try {
            val base64 = data.substringAfter(",")
            val decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            qrImage.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.size))
        } catch (_: Exception) {
            statusText.text = "QR Error"
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
