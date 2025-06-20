// @path: app/src/main/java/com/radwrld/wami/util/MediaCache.kt
package com.radwrld.wami.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.radwrld.wami.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object MediaCache {

    private fun cacheDir(context: Context) = File(context.cacheDir, "media_cache").apply { mkdirs() }

    // FIXED: Removed the 'private' modifier to make this function accessible from other classes.
    fun fileExt(mime: String?) = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"

    private fun sha256(stream: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) digest.update(buffer, 0, bytesRead)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun saveToCache(context: Context, uri: Uri): Pair<File, String>? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { hashStream ->
                val hash = sha256(hashStream)
                context.contentResolver.openInputStream(uri)?.use { fileStream ->
                    val ext = fileExt(context.contentResolver.getType(uri))
                    val file = File(cacheDir(context), "$hash.$ext")
                    FileOutputStream(file).use { fileStream.copyTo(it) }
                    return@withContext file to hash
                }
            }
        } catch (_: Exception) {}
        null
    }

    fun getCachedFile(context: Context, sha: String, ext: String): File? {
        val file = File(cacheDir(context), "$sha.$ext")
        return file.takeIf { it.exists() }
    }

    suspend fun downloadAndCache(context: Context, url: String, sha: String, ext: String): File? {
        getCachedFile(context, sha, ext)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val res = ApiClient.getDownloadingInstance(context).downloadFile(url)
                if (res.isSuccessful) {
                    res.body()?.let {
                        val file = File(cacheDir(context), "$sha.$ext")
                        it.byteStream().use { input -> FileOutputStream(file).use { input.copyTo(it) } }
                        return@withContext file
                    }
                }
            } catch (_: Exception) {}
            null
        }
    }
}
