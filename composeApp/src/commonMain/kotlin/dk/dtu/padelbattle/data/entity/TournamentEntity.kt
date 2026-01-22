package dk.dtu.padelbattle.data.entity

import androidx.room.ColumnInfo
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
    val pointsPerMatch: Int,
    val isCompleted: Boolean,
    /**
     * Antal runder der skal spilles før Mexicano-turneringen kan afsluttes.
     * Bruges til at sikre mindst 2 runder spilles efter en udvidelse.
     * 0 = kan afsluttes, >0 = skal spille flere runder
     */
    @ColumnInfo(defaultValue = "0")
    val extensionRoundsRemaining: Int = 0
)