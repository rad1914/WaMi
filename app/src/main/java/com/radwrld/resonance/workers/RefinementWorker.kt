// @path: app/src/main/java/com/radwrld/resonance/workers/RefinementWorker.kt
package com.radwrld.resonance.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.radwrld.resonance.TrackProcessor
import com.radwrld.resonance.data.db.AppDatabase
import android.net.Uri

class RefinementWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).trackResultDao()

        val uris = inputData.getStringArray("uris") ?: arrayOf()
        try {
            for (uriStr in uris) {
                val proc = TrackProcessor(applicationContext, Uri.parse(uriStr))
                val result = proc.processTrack()
                dao.insert(result)
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
