package dk.dtu.padelbattle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dk.dtu.padelbattle.data.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundNumber, courtNumber")
    fun getMatchesByTournament(tournamentId: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY roundNumber, courtNumber")
    suspend fun getMatchesByTournamentOnce(tournamentId: String): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("UPDATE matches SET scoreTeam1 = :score1, scoreTeam2 = :score2, isPlayed = 1 WHERE id = :matchId")
    suspend fun updateMatchScore(matchId: String, score1: Int, score2: Int)

    @Query("SELECT COUNT(*) FROM matches WHERE tournamentId = :tournamentId AND isPlayed = 0")
    suspend fun countUnplayedMatches(tournamentId: String): Int

    @Query("DELETE FROM matches WHERE tournamentId = :tournamentId")
    suspend fun deleteMatchesByTournament(tournamentId: String)
}