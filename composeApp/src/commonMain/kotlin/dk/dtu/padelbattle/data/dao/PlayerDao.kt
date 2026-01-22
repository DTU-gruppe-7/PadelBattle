package dk.dtu.padelbattle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dk.dtu.padelbattle.data.entity.PlayerEntity

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY totalPoints DESC")
    suspend fun getPlayersByTournamentOnce(tournamentId: String): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE tournamentId = :tournamentId ORDER BY name ASC")
    suspend fun getPlayersForTournament(tournamentId: String): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("SELECT COUNT(*) FROM players WHERE tournamentId = :tournamentId")
    suspend fun countPlayersForTournament(tournamentId: String): Int

    /**
     * Henter navne på spillere med højest points i en turnering (vindere).
     * Returnerer kun navne, ikke hele Player-objekter.
     */
    @Query("""
        SELECT name FROM players 
        WHERE tournamentId = :tournamentId 
        AND totalPoints = (SELECT MAX(totalPoints) FROM players WHERE tournamentId = :tournamentId)
        ORDER BY name
    """)
    suspend fun getWinnerNamesForTournament(tournamentId: String): List<String>
}