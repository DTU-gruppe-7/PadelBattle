package dk.dtu.padelbattle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dk.dtu.padelbattle.data.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY totalPoints DESC")
    fun getPlayersByTournament(tournamentId: String): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY totalPoints DESC")
    suspend fun getPlayersByTournamentOnce(tournamentId: String): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("UPDATE players SET totalPoints = :points, gamesPlayed = :gamesPlayed WHERE id = :playerId")
    suspend fun updatePlayerScore(playerId: String, points: Int, gamesPlayed: Int)

    @Query("DELETE FROM players WHERE tournamentId = :tournamentId")
    suspend fun deletePlayersByTournament(tournamentId: String)
}