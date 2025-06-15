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
    // ADDED: Singleton instance for the SocketManager
    private var socketManager: SocketManager? = null

    private fun authInterceptor(context: Context) = Interceptor { chain ->
        val token = ServerConfigStorage(context).getSessionId()
        val request = if (token.isNullOrEmpty()) chain.request() else
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        chain.proceed(request)
    }

    private fun buildClient(context: Context, logLevel: HttpLoggingInterceptor.Level, timeouts: Boolean = false): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = logLevel })
            .addInterceptor(authInterceptor(context))
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
            
    fun getBaseUrl(context: Context): String {
        return ServerConfigStorage(context).getCurrentServer()
    }

    fun getInstance(context: Context): WhatsAppApi {
        if (retrofit == null) {
            retrofit = buildRetrofit(
                getBaseUrl(context),
                buildClient(context, HttpLoggingInterceptor.Level.BODY)
            )
        }
        return retrofit!!.create(WhatsAppApi::class.java)
    }

    fun getDownloadingInstance(context: Context): WhatsAppApi {
        if (downloadRetrofit == null) {
            downloadRetrofit = buildRetrofit(
                getBaseUrl(context),
                buildClient(context, HttpLoggingInterceptor.Level.NONE, timeouts = true)
            )
        }
        return downloadRetrofit!!.create(WhatsAppApi::class.java)
    }

    fun initializeSocket(context: Context) {
        if (socket != null) return
        val config = ServerConfigStorage(context)
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

    // ADDED: Function to provide a singleton SocketManager instance.
    fun getSocketManager(context: Context): SocketManager {
        if (socketManager == null) {
            initializeSocket(context.applicationContext)
            socketManager = SocketManager(context.applicationContext)
        }
        return socketManager!!
    }

    fun getSocket(): Socket? = socket
    fun connectSocket() = socket?.takeIf { !it.connected() }?.connect()
    fun disconnectSocket() = socket?.disconnect()
    fun close() {
        disconnectSocket()
        socket = null
        socketManager = null
        retrofit = null
        downloadRetrofit = null
    }
}
