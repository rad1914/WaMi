package com.radwrld.wami.network

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.radwrld.wami.api.StatusResponse
import com.radwrld.wami.api.WaApi
import com.radwrld.wami.storage.ServerConfigStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitManager(
    private val context: Context,
    private val config: ServerConfigStorage,
    private val onSuccess: () -> Unit
) {
    lateinit var waApi: WaApi

    fun start() {
        attempt(0)
    }

    private fun attempt(tries: Int) {
        val server = if (tries == 0) config.primaryServer else config.fallbackServer
        initRetrofit(server)
        waApi.getStatus().enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                if (response.isSuccessful && response.body()?.connected == true) {
                    config.resetToPrimary()
                    onSuccess()
                } else if (tries == 0) {
                    attempt(1)
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                if (tries == 0) attempt(1)
            }
        })
    }

    private fun initRetrofit(serverIp: String) {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

        waApi = Retrofit.Builder()
            .baseUrl("http://$serverIp/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WaApi::class.java)
    }
}
