package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Data class der holder en spiller med beregnede bonus points.
 * Bruges til at vise "midlertidig" fair stilling når spillere er en runde bagud.
 */
data class PlayerStanding(
    val player: Player,
    val bonusPoints: Int,       // Bonus for manglende kampe
    val displayTotal: Int       // totalPoints + bonusPoints (bruges til sortering)
)

/**
 * ViewModel til at håndtere standings/stilling logik.
 * Henter spillere og sorterer dem efter points.
 * Beregner bonus points for spillere der er en runde bagud.
 */
class StandingsViewModel : ViewModel() {

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    private val _pointsPerMatch = MutableStateFlow(16)

    /**
     * Sorterede spillere med bonus points - beregnes automatisk når _players eller _pointsPerMatch ændres.
     * 
     * Bonus beregning:
     * - Find maksimalt antal kampe spillet af en spiller
     * - Spillere der har spillet færre kampe får bonus = (maxKampe - spillerKampe) × (pointsPerMatch / 2)
     * - Sorteres efter displayTotal (totalPoints + bonus), derefter wins
     */
    val sortedPlayers: StateFlow<List<PlayerStanding>> = combine(_players, _pointsPerMatch) { players, pointsPerMatch ->
        val maxGamesPlayed = players.maxOfOrNull { it.gamesPlayed } ?: 0
        val drawPoints = pointsPerMatch / 2  // Halvdelen af pointsPerMatch (uafgjort point)
        
        players.map { player ->
            val gamesBehind = maxGamesPlayed - player.gamesPlayed
            val bonus = gamesBehind * drawPoints
            PlayerStanding(
                player = player,
                bonusPoints = bonus,
                displayTotal = player.totalPoints + bonus
            )
        }.sortedWith(
            compareByDescending<PlayerStanding> { it.displayTotal }
                .thenByDescending { it.player.wins }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Opdaterer listen af spillere og pointsPerMatch fra turneringen.
     * Bonus og sortering beregnes automatisk via sortedPlayers flow.
     */
    fun setPlayers(players: List<Player>, pointsPerMatch: Int = 16) {
        _pointsPerMatch.value = pointsPerMatch
        _players.value = players
    }

}

