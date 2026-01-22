package dk.dtu.padelbattle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dk.dtu.padelbattle.data.entity.MatchEntity

@Dao
interface MatchDao {

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundNumber, courtNumber")
    suspend fun getMatchesByTournamentOnce(tournamentId: String): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("SELECT COUNT(*) FROM matches WHERE tournamentId = :tournamentId AND isPlayed = 0")
    suspend fun countUnplayedMatches(tournamentId: String): Int

    @Query("SELECT COUNT(*) FROM matches WHERE tournamentId = :tournamentId AND isPlayed = 1")
    suspend fun countPlayedMatches(tournamentId: String): Int

    @Query("DELETE FROM matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesByTournament(tournamentId: String)

    @Query("SELECT COUNT(*) FROM matches WHERE tournamentId = :tournamentId")
    suspend fun countMatchesForTournament(tournamentId: String): Int

    @Query("SELECT MAX(roundNumber) FROM matches WHERE tournamentId = :tournamentId")
    suspend fun getMaxRoundNumber(tournamentId: String): Int?
}