// @path: app/src/main/java/com/radwrld/wami/network/ApiClient.kt
package com.radwrld.wami.network

import android.content.Context
import android.util.Log
import com.radwrld.wami.storage.ServerConfigStorage
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

object ApiClient {

    private var retrofit: Retrofit? = null
    private var downloadRetrofit: Retrofit? = null
    private var socket: Socket? = null
    private var socketManager: SocketManager? = null

    // Use applicationContext to avoid memory leaks
    private fun authInterceptor(context: Context) = Interceptor { chain ->
        val token = ServerConfigStorage(context.applicationContext).getSessionId()
        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        chain.proceed(request)
    }

    private fun buildClient(context: Context, logLevel: HttpLoggingInterceptor.Level, timeouts: Boolean = false): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = logLevel })
            .addInterceptor(authInterceptor(context.applicationContext))
            .apply {
                if (timeouts) {
                    readTimeout(5, TimeUnit.MINUTES)
                    writeTimeout(5, TimeUnit.MINUTES)
                    connectTimeout(2, TimeUnit.MINUTES)
                }
            }
            .build()
    }

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    fun getBaseUrl(context: Context): String = ServerConfigStorage(context.applicationContext).getCurrentServer()

    fun getInstance(context: Context): WhatsAppApi {
        // Use applicationContext for building the client
        val safeContext = context.applicationContext
        return (retrofit ?: buildRetrofit(
            getBaseUrl(safeContext),
            buildClient(safeContext, HttpLoggingInterceptor.Level.BODY)
        ).also { retrofit = it }).create(WhatsAppApi::class.java)
    }

    fun getDownloadingInstance(context: Context): WhatsAppApi {
        // Use applicationContext for building the client
        val safeContext = context.applicationContext
        return (downloadRetrofit ?: buildRetrofit(
            getBaseUrl(safeContext),
            buildClient(safeContext, HttpLoggingInterceptor.Level.NONE, timeouts = true)
        ).also { downloadRetrofit = it }).create(WhatsAppApi::class.java)
    }

    fun initializeSocket(context: Context) {
        if (socket != null) return
        // Use applicationContext for safety
        val safeContext = context.applicationContext
        val config = ServerConfigStorage(safeContext)
        val token = config.getSessionId() ?: run {
            Log.e("ApiClient", "No session ID for socket.")
            return
        }
        try {
            socket = IO.socket(config.getCurrentServer(), IO.Options().apply {
                auth = mapOf("token" to token)
            })
        } catch (e: URISyntaxException) {
            Log.e("ApiClient", "Socket init failed", e)
        }
    }

    fun getSocketManager(context: Context): SocketManager {
        // Use applicationContext for safety
        val safeContext = context.applicationContext
        return socketManager ?: synchronized(this) {
             socketManager ?: run {
                initializeSocket(safeContext)
                SocketManager(safeContext).also { socketManager = it }
            }
        }
    }

    fun getSocket(): Socket? = socket
    fun connectSocket() = socket?.takeIf { !it.connected() }?.connect()
    fun disconnectSocket() = socket?.disconnect()

    fun close() {
        disconnectSocket()
        socketManager?.close() // Clean up listeners
        socket = null
        socketManager = null
        retrofit = null
        downloadRetrofit = null
    }
}
