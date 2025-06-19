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
import java.io.InputStream
import java.security.MessageDigest

object MediaCache {

    private fun getCacheDir(context: Context): File {
        return File(context.cacheDir, "media_cache").apply { mkdirs() }
    }

    fun getFileExtensionFromMimeType(mimeType: String?): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
    }

    private fun calculateSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun saveToCache(context: Context, sourceUri: Uri): Pair<File, String>? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { hashStream ->
                    val hash = calculateSha256(hashStream)
                    
                    context.contentResolver.openInputStream(sourceUri)?.use { fileStream ->
                        val mimeType = context.contentResolver.getType(sourceUri)
                        val extension = getFileExtensionFromMimeType(mimeType)
                        val cachedFile = File(getCacheDir(context), "$hash.$extension")

                        FileOutputStream(cachedFile).use { outputStream ->
                            fileStream.copyTo(outputStream)
                        }
                        Pair(cachedFile, hash)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    fun getCachedFile(context: Context, sha256: String, extension: String): File? {
        val file = File(getCacheDir(context), "$sha256.$extension")
        return if (file.exists()) file else null
    }

    suspend fun downloadAndCache(context: Context, downloadUrl: String, sha256: String, extension: String): File? {
        getCachedFile(context, sha256, extension)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val api = ApiClient.getDownloadingInstance(context)
                val response = api.downloadFile(downloadUrl)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        // FIX: Corrected typo from getCacheE to getCacheDir
                        val file = File(getCacheDir(context), "$sha256.$extension")
                        saveResponseBody(body, file)
                        return@withContext file
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveResponseBody(body: ResponseBody, destinationFile: File) {
        body.byteStream().use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
