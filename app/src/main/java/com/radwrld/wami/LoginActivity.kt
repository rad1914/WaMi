// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
// LoginActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.*
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.radwrld.wami.WaApi
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private lateinit var qrImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var waApi: WaApi
    private lateinit var config: ServerConfigStorage
    private val handler = Handler(Looper.getMainLooper())

    private val SWITCH_SERVER_DELAY_MS = 12_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        qrImage = findViewById(R.id.qrImage)
        statusText = findViewById(R.id.statusText)
        config = ServerConfigStorage(this)

        config.saveServers("22.ip.gl.ply.gg:18880", "127.0.0.1:3007")
        initApi(config.getCurrentServer())
        checkStatus()
    }

    private fun initApi(serverIp: String) {
        val baseUrl = "http://$serverIp/"
        Toast.makeText(this, "Connecting to $baseUrl", Toast.LENGTH_SHORT).show()

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

        waApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaApi::class.java)
    }

    private fun checkStatus() {
        lifecycleScope.launch {
            try {
                val statusResponse = waApi.getStatus()
                if (statusResponse.connected) {
                    config.resetToPrimary()
                    launchMain()
                } else {
                    retryOrPollQr()
                }
            } catch (e: Exception) {
                Log.e("Login", "Status check failed: ${e.localizedMessage}")
                retryOrPollQr()
            }
        }
    }

    private fun retryOrPollQr() {
        val oldServer = config.getCurrentServer()
        val nextServer = config.moveToNextServer()

        if (oldServer != nextServer) {
            handler.postDelayed({ initApi(nextServer); checkStatus() }, SWITCH_SERVER_DELAY_MS)
        } else {
            pollForQr()
        }
    }

    private fun pollForQr() {
        lifecycleScope.launch {
            try {
                val qrResponse = waApi.getQr()
                val qr = qrResponse.qr
                if (qr != null) {
                    renderQr(qr)
                    statusText.text = "Scan this with WhatsApp"
                } else {
                    statusText.text = "Waiting for QR…"
                }
                handler.postDelayed({ pollForQr() }, 9_000L)
            } catch (e: Exception) {
                statusText.text = "QR error: ${e.localizedMessage}"
                handler.postDelayed({ pollForQr() }, 10_000L)
            }
        }
    }

    private fun renderQr(text: String) {
        try {
            val size = 400
            val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size)
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            qrImage.setImageBitmap(bmp)
        } catch (e: Exception) {
            statusText.text = "Failed to render QR"
            Log.e("Login", "QR render error", e)
        }
    }

    private fun launchMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
