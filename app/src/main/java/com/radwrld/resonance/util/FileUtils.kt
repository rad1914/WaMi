// @path: app/src/main/java/com/radwrld/resonance/util/FileUtils.kt
package com.radwrld.resonance.util

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {
    fun cacheFileFromUri(context: Context, uri: Uri, filename: String): File {
        val f = File(context.cacheDir, filename)
        context.contentResolver.openInputStream(uri).use { input -> f.outputStream().use { output -> input?.copyTo(output) } }
        return f
    }
}
