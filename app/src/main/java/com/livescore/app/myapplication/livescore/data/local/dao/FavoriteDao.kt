package com.livescore.app.myapplication.livescore.data.local.dao

import androidx.room.*
import com.livescore.app.myapplication.livescore.data.local.entity.FavoriteLeagueEntity
import com.livescore.app.myapplication.livescore.data.local.entity.FavoriteTeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_teams")
    fun getAllFavoriteTeams(): Flow<List<FavoriteTeamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTeam(team: FavoriteTeamEntity)

    @Delete
    suspend fun deleteFavoriteTeam(team: FavoriteTeamEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_teams WHERE id = :teamId LIMIT 1)")
    suspend fun isTeamFavorite(teamId: Int): Boolean

    @Query("SELECT * FROM favorite_leagues")
    fun getAllFavoriteLeagues(): Flow<List<FavoriteLeagueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteLeague(league: FavoriteLeagueEntity)

    @Delete
    suspend fun deleteFavoriteLeague(league: FavoriteLeagueEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_leagues WHERE id = :leagueId LIMIT 1)")
    suspend fun isLeagueFavorite(leagueId: Int): Boolean
}
