// @path: app/src/main/java/com/radwrld/wami/LoginActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.graphics.*
import android.os.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.radwrld.wami.network.WhatsAppApi // FIXED: Corrected case to WhatsAppApi
import com.radwrld.wami.storage.ServerConfigStorage
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private lateinit var qrImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var waApi: WhatsAppApi // FIXED: Corrected case to WhatsAppApi
    private lateinit var config: ServerConfigStorage
    private val handler = Handler(Looper.getMainLooper())
    private val switchDelay = 12_000L

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

    private fun initApi(server: String) {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

        waApi = Retrofit.Builder()
            .baseUrl("http://$server/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhatsAppApi::class.java) // FIXED: Corrected case to WhatsAppApi

        Toast.makeText(this, "Connecting to http://$server/", Toast.LENGTH_SHORT).show()
    }

    private fun checkStatus() {
        lifecycleScope.launch {
            try {
                if (waApi.getStatus().connected) {
                    config.resetToPrimary()
                    config.saveLoginState(true)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    config.saveLoginState(false)
                    retryOrPoll()
                }
            } catch (e: Exception) {
                if (e is UnknownHostException) {
                    statusText.text = "No internet connection.\nPlease connect to log in for the first time."
                    Toast.makeText(this@LoginActivity, "Please check your internet connection", Toast.LENGTH_LONG).show()
                } else {
                    retryOrPoll()
                }
            }
        }
    }

    private fun retryOrPoll() {
        val next = config.moveToNextServer()
        if (next != config.getCurrentServer()) {
            handler.postDelayed({ initApi(next); checkStatus() }, switchDelay)
        } else pollQr()
    }

    private fun pollQr() {
        lifecycleScope.launch {
            try {
                val qr = waApi.getQr().qr
                if (qr.isNotBlank()) {
                    drawQr(qr)
                    statusText.text = "Scan this with WhatsApp"
                } else {
                    statusText.text = "Waiting for QR…"
                }
                handler.postDelayed({ pollQr() }, 9_000L)
            } catch (e: Exception) {
                if (e is UnknownHostException) {
                    statusText.text = "Internet required to scan QR code.\nPlease connect and restart the app."
                } else {
                    statusText.text = "QR error: Could not reach server."
                    handler.postDelayed({ pollQr() }, 10_000L)
                }
            }
        }
    }

    private fun drawQr(data: String) {
        try {
            val size = 400
            val matrix = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size)
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            qrImage.setImageBitmap(bmp)
        } catch (_: Exception) {
            statusText.text = "Failed to render QR"
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
