package com.radwrld.wami

import android.content.Intent
import android.graphics.*
import android.os.*
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.radwrld.wami.network.WhatsAppApi
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private lateinit var qrImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var waApi: WhatsAppApi
    private lateinit var config: ServerConfigStorage
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        qrImage = findViewById(R.id.qrImage)
        statusText = findViewById(R.id.statusText)
        config = ServerConfigStorage(this)

        config.saveServers("22.ip.gl.ply.gg:18880", "127.0.0.1:3007")
        initApi(config.getCurrentServer())

        // Start the authentication flow
        startAuthFlow()
    }

    private fun initApi(server: String) {
        // Interceptor to add the Authorization header to every request
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val sessionId = config.getSessionId()
            if (sessionId.isNullOrEmpty()) {
                return@Interceptor chain.proceed(originalRequest)
            }
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $sessionId")
                .build()
            chain.proceed(newRequest)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(authInterceptor)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

        waApi = Retrofit.Builder()
            .baseUrl("http://$server/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java)

        Toast.makeText(this, "Connecting to http://$server/", Toast.LENGTH_SHORT).show()
    }

    private fun startAuthFlow() {
        lifecycleScope.launch {
            // Step 1: Ensure we have a session ID
            if (config.getSessionId().isNullOrEmpty()) {
                statusText.text = "Creating new session..."
                try {
                    val sessionResponse = waApi.createSession()
                    config.saveSessionId(sessionResponse.sessionId)
                    Log.d("LoginActivity", "New session created: ${sessionResponse.sessionId}")
                } catch (e: Exception) {
                    statusText.text = "Failed to create session. Please restart."
                    Log.e("LoginActivity", "Session creation failed", e)
                    return@launch
                }
            }
            // Step 2: Start polling for status
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
                        statusText.text = "Waiting for QR code..."
                    }
                    // Continue polling
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
                // Unauthorized, session is likely invalid. Create a new one.
                Log.w("LoginActivity", "Session token is invalid (401). Resetting.")
                Toast.makeText(this, "Session expired. Creating a new one.", Toast.LENGTH_SHORT).show()
                config.saveSessionId(null)
                // Restart the authentication flow
                handler.postDelayed({ startAuthFlow() }, 1000L)
            }
            e is UnknownHostException -> {
                statusText.text = "Cannot connect to the server. Check your network."
                Toast.makeText(this@LoginActivity, "Please check your internet connection", Toast.LENGTH_LONG).show()
            }
            else -> {
                statusText.text = "Error connecting to server. Retrying..."
                Log.e("LoginActivity", "API Error", e)
                // General retry
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
            // The data is already a data URL (e.g., "data:image/png;base64,...")
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
