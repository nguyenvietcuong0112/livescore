package com.livescore.app.myapplication.livescore.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.livescore.app.myapplication.livescore.data.local.dao.FavoriteDao
import com.livescore.app.myapplication.livescore.data.local.dao.MatchDao
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import com.livescore.app.myapplication.livescore.data.local.entity.FavoriteLeagueEntity
import com.livescore.app.myapplication.livescore.data.local.entity.FavoriteTeamEntity

@Database(
    entities = [
        FavoriteTeamEntity::class,
        FavoriteLeagueEntity::class,
        CachedMatchEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun matchDao(): MatchDao
}
