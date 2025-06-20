// @path: app/src/main/java/com/radwrld/wami/WamiGlideModule.kt
package com.radwrld.wami

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.radwrld.wami.network.ApiClient
import java.io.InputStream

@GlideModule
class WamiGlideModule : AppGlideModule() {
    /**
     * Replaces Glide's default networking client with our app's shared,
     * authenticated OkHttpClient instance. This ensures that Authorization
     * headers are added to all image download requests made by Glide.
     */
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Ensure our client is initialized
        ApiClient.getDownloadingInstance(context)
        
        // Use the public, authenticated OkHttpClient from our ApiClient
        ApiClient.downloadHttpClient?.let { client ->
            registry.replace(
                GlideUrl::class.java,
                InputStream::class.java,
                OkHttpUrlLoader.Factory(client)
            )
        }
    }
}
