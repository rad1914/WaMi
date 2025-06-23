// @path: app/src/main/java/com/radwrld/wami/data/WamiGlideActivity.kt
// @path: app/src/main/java/com/radwrld/wami/data/WamiGlideModule.kt
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
    
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {

        ApiClient.getDownloadingInstance(context)

        ApiClient.downloadHttpClient?.let { client ->
            registry.replace(
                GlideUrl::class.java,
                InputStream::class.java,
                OkHttpUrlLoader.Factory(client)
            )
        }
    }
}
