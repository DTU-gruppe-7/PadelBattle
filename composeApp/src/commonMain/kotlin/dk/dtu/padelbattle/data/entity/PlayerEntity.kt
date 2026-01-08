package dk.dtu.padelbattle.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE  // Sletter spillere når turnering slettes
        )
    ],
    indices = [Index("tournamentId")]  // Gør søgning på tournamentId hurtigere
)
data class PlayerEntity(
    @PrimaryKey
    val id: String,
    val tournamentId: String,
    val name: String,
    val totalPoints: Int = 0,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)