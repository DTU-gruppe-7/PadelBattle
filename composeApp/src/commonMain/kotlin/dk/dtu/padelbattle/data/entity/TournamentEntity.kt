package dk.dtu.padelbattle.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,  // "AMERICANO" eller "MEXICANO" (fra TournamentType enum)
    val dateCreated: Long,
    val numberOfCourts: Int,
    val isCompleted: Boolean
)