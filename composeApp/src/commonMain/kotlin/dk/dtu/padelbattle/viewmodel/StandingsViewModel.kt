package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel til at håndtere standings/stilling logik.
 * Henter spillere og sorterer dem efter points.
 */
class StandingsViewModel : ViewModel() {

    private val _players = MutableStateFlow<List<Player>>(emptyList())

    /**
     * Sorterede spillere - beregnes automatisk når _players ændres.
     * Bruger stateIn for at lave en derived state flow.
     */
    val sortedPlayers: StateFlow<List<Player>> = _players
        .map { players -> players.sortedByDescending { it.totalPoints } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Opdaterer listen af spillere fra turneringen.
     * Sorteres automatisk via sortedPlayers flow.
     */
    fun setPlayers(players: List<Player>) {
        _players.value = players
    }

}

