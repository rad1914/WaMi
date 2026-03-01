// @path: app/src/main/java/com/radwrld/resonance/data/db/TrackResultDao.kt
package com.radwrld.resonance.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TrackResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TrackResult)

    @Query("SELECT * FROM track_results WHERE trackId = :id")
    suspend fun get(id: String): TrackResult?

    @Query("SELECT * FROM track_results")
    fun getAllLive(): LiveData<List<TrackResult>>
}
