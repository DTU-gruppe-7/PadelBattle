package dk.dtu.padelbattle.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dk.dtu.padelbattle.data.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Query("SELECT * FROM tournaments ORDER BY dateCreated DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: String): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity)

    @Query("UPDATE tournaments SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTournamentCompleted(id: String, isCompleted: Boolean)

    @Query("UPDATE tournaments SET name = :name WHERE id = :id")
    suspend fun updateTournamentName(id: String, name: String)

    @Query("UPDATE tournaments SET numberOfCourts = :numberOfCourts WHERE id = :id")
    suspend fun updateNumberOfCourts(id: String, numberOfCourts: Int)

    @Query("UPDATE tournaments SET pointsPerMatch = :pointsPerMatch WHERE id = :id")
    suspend fun updatePointsPerMatch(id: String, pointsPerMatch: Int)

    @Query("UPDATE tournaments SET extensionRoundsRemaining = :rounds WHERE id = :id")
    suspend fun updateExtensionRoundsRemaining(id: String, rounds: Int)

    @Query("SELECT extensionRoundsRemaining FROM tournaments WHERE id = :id")
    suspend fun getExtensionRoundsRemaining(id: String): Int?

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournamentById(id: String)
}