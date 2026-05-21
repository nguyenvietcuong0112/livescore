package com.livescore.app.myapplication.livescore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM cached_matches ORDER BY dateTimestamp ASC")
    fun getAllCachedMatches(): Flow<List<CachedMatchEntity>>

    @Query("SELECT * FROM cached_matches WHERE statusShort = '1H' OR statusShort = '2H' OR statusShort = 'HT' OR statusShort = 'ET' OR statusShort = 'BT' OR statusShort = 'P' ORDER BY dateTimestamp ASC")
    fun getLiveCachedMatches(): Flow<List<CachedMatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<CachedMatchEntity>)

    @Query("DELETE FROM cached_matches")
    suspend fun clearAllMatches()
}
