package dk.dtu.padelbattle.domain.model

import dk.dtu.padelbattle.domain.util.generateId

data class Player(
    val id: String = generateId(),
    val name: String,
    val totalPoints: Int = 0,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)
