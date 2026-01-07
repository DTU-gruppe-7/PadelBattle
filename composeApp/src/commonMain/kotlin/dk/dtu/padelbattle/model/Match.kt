package dk.dtu.padelbattle.model
import dk.dtu.padelbattle.model.Utils.generateId

data class Match(val id: String = generateId(),
                 val matchNumber: Int,
                 val roundNumber: Int,
                 val courtNumber: Int,

    // Hold 1
                 val team1Player1: Player,
                 val team1Player2: Player,

    // Hold 2
                 val team2Player1: Player,
                 val team2Player2: Player,

    // Resultater
                 var scoreTeam1: Int = 0,
                 var scoreTeam2: Int = 0,
                 var isPlayed: Boolean = false)
