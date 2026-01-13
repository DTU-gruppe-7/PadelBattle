package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // ==================== Player Name Editing ====================

    private val _editingPlayer = MutableStateFlow<Player?>(null)
    val editingPlayer: StateFlow<Player?> = _editingPlayer.asStateFlow()

    private val _editingName = MutableStateFlow("")
    val editingName: StateFlow<String> = _editingName.asStateFlow()

    /**
     * Start redigering af en spillers navn.
     */
    fun startEditingPlayer(player: Player) {
        _editingPlayer.value = player
        _editingName.value = player.name
    }

    /**
     * Opdater det indtastede navn.
     */
    fun updateEditingName(name: String) {
        _editingName.value = name
    }

    /**
     * Annuller redigering.
     */
    fun cancelEditing() {
        _editingPlayer.value = null
        _editingName.value = ""
    }

    /**
     * Gem det nye navn.
     * @param onSave callback der kaldes med spilleren og det nye navn - bruges til at persistere ændringen.
     */
    fun savePlayerName(onSave: (Player, String) -> Unit) {
        val player = _editingPlayer.value ?: return
        val newName = _editingName.value.trim()
        if (newName.isNotBlank()) {
            onSave(player, newName)
        }
        cancelEditing()
    }

}

