package com.radwrld.wami.network

import android.content.Context
import com.radwrld.wami.storage.ServerConfigStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var downloadRetrofit: Retrofit? = null

    @Volatile var httpClient: OkHttpClient? = null
        private set

    @Volatile var downloadHttpClient: OkHttpClient? = null
        private set

    // El interceptor de autorización se mantiene, es correcto.
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
        return retrofit?.create(WhatsAppApi::class.java) ?: synchronized(this) {
            retrofit?.create(WhatsAppApi::class.java) ?: run {
                val safeContext = context.applicationContext
                val client = httpClient ?: buildClient(safeContext, HttpLoggingInterceptor.Level.BODY).also { httpClient = it }
                buildRetrofit(getBaseUrl(safeContext), client).also { retrofit = it }.create(WhatsAppApi::class.java)
            }
        }
    }

    // ++ ACTUALIZADO: El cliente de descarga ahora no necesita una URL base diferente,
    // ya que la URL de descarga se construye dinámicamente en la interfaz de la API.
    fun getDownloadingInstance(context: Context): WhatsAppApi {
        return downloadRetrofit?.create(WhatsAppApi::class.java) ?: synchronized(this) {
            downloadRetrofit?.create(WhatsAppApi::class.java) ?: run {
                val safeContext = context.applicationContext
                val client = downloadHttpClient ?: buildClient(safeContext, HttpLoggingInterceptor.Level.NONE, timeouts = true).also { downloadHttpClient = it }
                buildRetrofit(getBaseUrl(safeContext), client).also { downloadRetrofit = it }.create(WhatsAppApi::class.java)
            }
        }
    }

    fun close() {
        retrofit = null
        downloadRetrofit = null
        httpClient = null
        downloadHttpClient = null
    }
}
