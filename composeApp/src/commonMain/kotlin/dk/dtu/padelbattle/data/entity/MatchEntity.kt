package dk.dtu.padelbattle.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE  // Sletter kampe når turnering slettes
        )
    ],
    indices = [Index("tournamentId")]  // Gør søgning på tournamentId hurtigere
)
data class MatchEntity(
    @PrimaryKey
    val id: String,
    val tournamentId: String,
    val roundNumber: Int,
    val courtNumber: Int,
    val team1Player1Id: String,
    val team1Player2Id: String,
    val team2Player1Id: String,
    val team2Player2Id: String,
    val scoreTeam1: Int = 0,
    val scoreTeam2: Int = 0,
    val isPlayed: Boolean = false
)