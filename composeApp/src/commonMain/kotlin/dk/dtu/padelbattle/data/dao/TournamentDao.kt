package dk.dtu.padelbattle.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dk.dtu.padelbattle.data.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Query("SELECT * FROM tournaments ORDER BY dateCreated DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE isCompleted = 0 ORDER BY dateCreated DESC")
    fun getActiveTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: String): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity)

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Query("UPDATE tournaments SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTournamentCompleted(id: String, isCompleted: Boolean)

    @Query("UPDATE tournaments SET name = :name WHERE id = :id")
    suspend fun updateTournamentName(id: String, name: String)

    @Query("UPDATE tournaments SET numberOfCourts = :numberOfCourts WHERE id = :id")
    suspend fun updateNumberOfCourts(id: String, numberOfCourts: Int)

    @Delete
    suspend fun deleteTournament(tournament: TournamentEntity)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournamentById(id: String)
}