package dk.dtu.padelbattle.viewModel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.model.Match
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel til at håndtere redigering af kampresultater.
 * Opdaterer kampens score og spillernes statistikker (points, wins, losses, draws).
 */
class MatchEditViewModel : ViewModel() {

    private val _scoreTeam1 = MutableStateFlow(0)
    val scoreTeam1: StateFlow<Int> = _scoreTeam1.asStateFlow()

    private val _scoreTeam2 = MutableStateFlow(0)
    val scoreTeam2: StateFlow<Int> = _scoreTeam2.asStateFlow()

    private val _currentMatch = MutableStateFlow<Match?>(null)
    val currentMatch: StateFlow<Match?> = _currentMatch.asStateFlow()

    /**
     * Initialiserer viewModel med en kamp
     */
    fun setMatch(match: Match) {
        _currentMatch.value = match
        _scoreTeam1.value = match.scoreTeam1
        _scoreTeam2.value = match.scoreTeam2
    }

    fun updateScoreTeam1(score: Int) {
        if (score >= 0) {
            _scoreTeam1.value = score
        }
    }

    fun updateScoreTeam2(score: Int) {
        if (score >= 0) {
            _scoreTeam2.value = score
        }
    }

    fun incrementScoreTeam1() {
        _scoreTeam1.value++
    }

    fun decrementScoreTeam1() {
        if (_scoreTeam1.value > 0) {
            _scoreTeam1.value--
        }
    }

    fun incrementScoreTeam2() {
        _scoreTeam2.value++
    }

    fun decrementScoreTeam2() {
        if (_scoreTeam2.value > 0) {
            _scoreTeam2.value--
        }
    }

    /**
     * Gemmer kampresultatet og opdaterer spillernes statistikker.
     * Returnerer den opdaterede kamp.
     */
    fun saveMatch(): Match? {
        val match = _currentMatch.value ?: return null
        val newScoreTeam1 = _scoreTeam1.value
        val newScoreTeam2 = _scoreTeam2.value

        // Hvis kampen allerede er spillet, fjern de gamle statistikker først
        if (match.isPlayed) {
            revertPlayerStats(match)
        }

        // Opdater kampens score
        match.scoreTeam1 = newScoreTeam1
        match.scoreTeam2 = newScoreTeam2
        match.isPlayed = true

        // Opdater spillernes statistikker baseret på resultatet
        updatePlayerStats(match)

        return match
    }

    /**
     * Opdaterer spillernes statistikker baseret på kampresultatet.
     */
    private fun updatePlayerStats(match: Match) {
        val team1Players = listOf(match.team1Player1, match.team1Player2)
        val team2Players = listOf(match.team2Player1, match.team2Player2)

        team1Players.forEach { player ->
            player.totalPoints += match.scoreTeam1
        }
        team2Players.forEach { player ->
            player.totalPoints += match.scoreTeam2
        }

        when {
            match.scoreTeam1 > match.scoreTeam2 -> {
                // Team 1 vinder
                team1Players.forEach { player ->
                    player.wins++
                    player.gamesPlayed++
                }
                team2Players.forEach { player ->
                    player.losses++
                    player.gamesPlayed++
                }
            }
            match.scoreTeam2 > match.scoreTeam1 -> {
                // Team 2 vinder
                team2Players.forEach { player ->
                    player.wins++
                    player.gamesPlayed++
                }
                team1Players.forEach { player ->
                    player.losses++
                    player.gamesPlayed++
                }
            }
            else -> {
                // Uafgjort
                (team1Players + team2Players).forEach { player ->
                    player.draws++
                    player.gamesPlayed++
                }
            }
        }
    }

    /**
     * Fjerner de gamle statistikker fra spillerne (bruges når en kamp redigeres).
     */
    private fun revertPlayerStats(match: Match) {
        val team1Players = listOf(match.team1Player1, match.team1Player2)
        val team2Players = listOf(match.team2Player1, match.team2Player2)

        // Træk point tilbage
        team1Players.forEach { player ->
            player.totalPoints -= match.scoreTeam1
        }
        team2Players.forEach { player ->
            player.totalPoints -= match.scoreTeam2
        }

        when {
            match.scoreTeam1 > match.scoreTeam2 -> {
                // Team 1 havde vundet
                team1Players.forEach { player ->
                    player.wins--
                    player.gamesPlayed--
                }
                team2Players.forEach { player ->
                    player.losses--
                    player.gamesPlayed--
                }
            }
            match.scoreTeam2 > match.scoreTeam1 -> {
                // Team 2 havde vundet
                team2Players.forEach { player ->
                    player.wins--
                    player.gamesPlayed--
                }
                team1Players.forEach { player ->
                    player.losses--
                    player.gamesPlayed--
                }
            }
            else -> {
                // Var uafgjort
                (team1Players + team2Players).forEach { player ->
                    player.draws--
                    player.gamesPlayed--
                }
            }
        }
    }

    fun reset() {
        _scoreTeam1.value = 0
        _scoreTeam2.value = 0
        _currentMatch.value = null
    }
}