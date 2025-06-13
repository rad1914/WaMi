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

object ApiClient {

    private var retrofit: Retrofit? = null
    private var socket: Socket? = null
    private const val TAG = "ApiClient"

    fun getInstance(context: Context): WhatsAppApi {
        if (retrofit == null) {
            val serverConfig = ServerConfigStorage(context)

            val authInterceptor = Interceptor { chain ->
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

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl("http://${serverConfig.getCurrentServer()}/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(WhatsAppApi::class.java)
    }

    fun initializeSocket(context: Context) {
        if (socket == null) {
            val serverConfig = ServerConfigStorage(context)
            val serverUrl = "http://${serverConfig.getCurrentServer()}"
            val sessionId = serverConfig.getSessionId()

            if (sessionId.isNullOrEmpty()) {
                Log.e(TAG, "Cannot initialize socket without a session ID.")
                return
            }

            // Set authentication options for the socket connection
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

    fun getSocket(): Socket? {
        return socket
    }

    fun connectSocket() {
        socket?.takeIf { !it.connected() }?.connect()
    }

    fun disconnectSocket() {
        socket?.disconnect()
    }

    // Call this on logout to allow re-initialization with a new session
    fun close() {
        disconnectSocket()
        socket = null
        retrofit = null
    }
}
