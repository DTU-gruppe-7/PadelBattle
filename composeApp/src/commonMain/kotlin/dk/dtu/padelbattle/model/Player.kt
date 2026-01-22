package dk.dtu.padelbattle.model

import dk.dtu.padelbattle.model.utils.generateId

data class Player(
    val id: String = generateId(),
    val name: String,
    val totalPoints: Int = 0,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)
