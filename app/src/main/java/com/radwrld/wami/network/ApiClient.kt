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

/**
 * NOTE ON ARCHITECTURE:
 * This singleton `object` pattern is functional for managing network clients.
 * For future development, consider migrating to a dependency injection framework like Hilt or Koin.
 *
 * Benefits of Dependency Injection:
 * - Improved Testability: Easily provide mock/fake API clients in tests.
 * - Better Lifecycle Management: Tie component lifecycles (e.g., Sockets) to ViewModels or the Application.
 * - Simplified Code: Removes the need for manual context passing and singleton management.
 */
object ApiClient {

    private var standardRetrofit: Retrofit? = null
    private var downloadRetrofit: Retrofit? = null
    private var socket: Socket? = null
    private const val TAG = "ApiClient"

    private fun getAuthInterceptor(context: Context): Interceptor {
        val serverConfig = ServerConfigStorage(context)
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val sessionId = serverConfig.getSessionId()
            if (sessionId.isNullOrEmpty()) {
                return@Interceptor chain.proceed(originalRequest)
            }
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $sessionId")
                .build()
            chain.proceed(newRequest)
        }
    }

    fun getInstance(context: Context): WhatsAppApi {
        // UPDATED: Added synchronized block for thread safety
        synchronized(this) {
            if (standardRetrofit == null) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(getAuthInterceptor(context))
                    .build()

                standardRetrofit = Retrofit.Builder()
                    // UPDATED: Removed hardcoded "http://". Assumes full URL from storage.
                    .baseUrl(ServerConfigStorage(context).getCurrentServer())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return standardRetrofit!!.create(WhatsAppApi::class.java)
        }
    }

    fun getDownloadingInstance(context: Context): WhatsAppApi {
        // UPDATED: Added synchronized block for thread safety
        synchronized(this) {
            if (downloadRetrofit == null) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.NONE
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(getAuthInterceptor(context))
                    .readTimeout(5, TimeUnit.MINUTES)
                    .writeTimeout(5, TimeUnit.MINUTES)
                    .connectTimeout(2, TimeUnit.MINUTES)
                    .build()

                downloadRetrofit = Retrofit.Builder()
                    // UPDATED: Removed hardcoded "http://". Assumes full URL from storage.
                    .baseUrl(ServerConfigStorage(context).getCurrentServer())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return downloadRetrofit!!.create(WhatsAppApi::class.java)
        }
    }

    fun initializeSocket(context: Context) {
        // UPDATED: Added synchronized block for thread safety
        synchronized(this) {
            if (socket == null) {
                val serverConfig = ServerConfigStorage(context)
                val serverUrl = serverConfig.getCurrentServer() // Assumes full URL from storage
                val sessionId = serverConfig.getSessionId()

                if (sessionId.isNullOrEmpty()) {
                    Log.e(TAG, "Cannot initialize socket without a session ID.")
                    return
                }

                val opts = IO.Options().apply {
                    auth = mapOf("token" to sessionId)
                }

                try {
                    socket = IO.socket(serverUrl, opts)
                } catch (e: URISyntaxException) {
                    Log.e(TAG, "Socket URI syntax error", e)
                }
            }
        }
    }

    fun getSocket(): Socket? {
        return socket
    }



    fun connectSocket() {
        socket?.takeIf { !it.connected() }?.connect()
    }

    fun disconnectSocket() {
        socket?.disconnect()
    }

    fun close() {
        disconnectSocket()
        socket = null
        standardRetrofit = null
        downloadRetrofit = null
    }
}
