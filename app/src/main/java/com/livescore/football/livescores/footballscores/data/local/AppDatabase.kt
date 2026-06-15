package com.livescore.football.livescores.footballscores.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.livescore.football.livescores.footballscores.data.local.dao.FavoriteDao
import com.livescore.football.livescores.footballscores.data.local.dao.MatchDao
import com.livescore.football.livescores.footballscores.data.local.entity.CachedMatchEntity
import com.livescore.football.livescores.footballscores.data.local.entity.FavoriteLeagueEntity
import com.livescore.football.livescores.footballscores.data.local.entity.FavoriteTeamEntity

@Database(
    entities = [
        FavoriteTeamEntity::class,
        FavoriteLeagueEntity::class,
        CachedMatchEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun matchDao(): MatchDao
}
