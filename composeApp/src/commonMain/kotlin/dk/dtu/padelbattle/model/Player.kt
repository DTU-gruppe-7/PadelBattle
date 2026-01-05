package dk.dtu.padelbattle.model

import dk.dtu.padelbattle.model.Utils.generateId

data class Player(val id: String = generateId(), // Genererer automatisk unikt ID
                  val name: String,
                  var totalPoints: Int = 0,
                  var gamesPlayed: Int = 0)
