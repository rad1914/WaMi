// @path: app/src/main/java/com/radwrld/wami/network/RetrofitClient.kt
package com.radwrld.wami.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // IMPORTANT: Replace with your server's actual base URL.
    // If running the server locally and testing on an emulator, use 10.0.2.2.
    // If testing on a physical device, use your computer's network IP address.
    private const val BASE_URL = "http://192.168.1.68:3007/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
