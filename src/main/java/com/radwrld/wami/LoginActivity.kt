package com.radwrld.wami

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.radwrld.wami.api.StatusResponse
import com.radwrld.wami.api.QrResponse
import com.radwrld.wami.api.WaApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private lateinit var qrImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var waApi: WaApi
    private val handler = Handler()
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        qrImage    = findViewById(R.id.qrImage)
        statusText = findViewById(R.id.statusText)

        // Retrofit + logging
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.68:3007/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        waApi = retrofit.create(WaApi::class.java)

        // 1) Check status
        waApi.getStatus().enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, resp: Response<StatusResponse>) {
                if (resp.isSuccessful && resp.body()?.connected == true) {
                    launchMain()
                } else {
                    pollForQr()
                }
            }
            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                Log.e("LoginActivity", "status() failed: ${t.localizedMessage}", t)
                pollForQr()
            }
        })
    }

    private fun pollForQr() {
        if (isConnected) return
        waApi.getQr().enqueue(object : Callback<QrResponse> {
            override fun onResponse(call: Call<QrResponse>, resp: Response<QrResponse>) {
                if (resp.isSuccessful && resp.body()?.qr != null) {
                    renderQr(resp.body()!!.qr)
                    statusText.text = "Scan this with WhatsApp"
                } else {
                    statusText.text = "Waiting for QR…"
                }
                handler.postDelayed({ pollForQr() }, 3000)
            }
            override fun onFailure(call: Call<QrResponse>, t: Throwable) {
                statusText.text = "Error fetching QR: ${t.localizedMessage}"
                handler.postDelayed({ pollForQr() }, 5000)
            }
        })
    }

    private fun renderQr(text: String) {
        try {
            val size = 400
            val matrix: BitMatrix = MultiFormatWriter()
                .encode(text, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size)
                for (y in 0 until size)
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            qrImage.setImageBitmap(bmp)
        } catch (e: Exception) {
            statusText.text = "Failed to render QR"
            Log.e("LoginActivity", "renderQr error: ${e.localizedMessage}", e)
        }
    }

    private fun launchMain() {
        isConnected = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
