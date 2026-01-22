package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MatchListViewModel : ViewModel() {

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound.asStateFlow()

    fun setCurrentRound(round: Int) {
        _currentRound.value = round
    }

    /**
     * Beregner hvilke spillere der sidder over i en given runde.
     * En spiller sidder over hvis de ikke deltager i nogen kamp i runden.
     */
    fun getSittingOutPlayers(allPlayers: List<Player>, roundMatches: List<Match>): List<Player> {
        val playersInRound = roundMatches.flatMap { match ->
            listOf(
                match.team1Player1.id,
                match.team1Player2.id,
                match.team2Player1.id,
                match.team2Player2.id
            )
        }.toSet()

        return allPlayers.filter { player -> player.id !in playersInRound }
    }

    /**
     * Kaldes når man går ind på en turnering.
     * Sætter kampene OG nulstiller visningen til den første runde, der ikke er færdigspillet.
     */
    fun loadTournament(matches: List<Match>) {
        _matches.value = matches.toList()
        _revision.value++

        // 1. Sorter kampene efter runde (for en sikkerheds skyld) og find den første kamp, der IKKE er spillet
        val firstUnplayedMatch = matches
            .sortedBy { it.roundNumber }
            .firstOrNull { !it.isPlayed }

        // 2. Bestem hvilken runde der skal vises:
        // - Hvis vi fandt en uspillet kamp -> Brug dens runde.
        // - Hvis alle er spillet (firstUnplayedMatch er null) -> Brug den sidste runde (max).
        // - Hvis listen er tom -> Brug runde 1.
        val targetRound = firstUnplayedMatch?.roundNumber
            ?: matches.maxOfOrNull { it.roundNumber }
            ?: 1

        _currentRound.value = targetRound
    }

    /**
     * Bruges til løbende opdateringer (f.eks. score-indtastning),
     * uden at ændre hvilken runde brugeren kigger på.
     */
    fun updateMatches(matches: List<Match>) {
        _matches.value = matches.toList()
        _revision.value++
    }

    // Behold notifyMatchUpdated hvis du bruger den til in-place ændringer
    fun notifyMatchUpdated() {
        _revision.value++
    }

    /**
     * Navigerer til den første uspillede runde.
     * Bruges efter at turneringen er blevet udvidet med nye runder.
     */
    fun navigateToFirstUnplayedRound() {
        val firstUnplayedMatch = _matches.value
            .sortedBy { it.roundNumber }
            .firstOrNull { !it.isPlayed }

        val targetRound = firstUnplayedMatch?.roundNumber
            ?: _matches.value.maxOfOrNull { it.roundNumber }
            ?: 1

        _currentRound.value = targetRound
    }
}