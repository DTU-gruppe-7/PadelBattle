package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.MatchResult
import dk.dtu.padelbattle.model.utils.MatchResultService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel til at håndtere redigering af kampresultater.
 * Koordinerer UI-tilstand og delegerer forretningslogik til MatchResultService.
 */
class MatchEditViewModel : ViewModel() {

    private val matchResultService = MatchResultService()

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
     * Gemmer kampresultatet via MatchResultService.
     * Returnerer den opdaterede kamp.
     */
    fun saveMatch(): Match? {
        val match = _currentMatch.value ?: return null
        val newResult = MatchResult(
            scoreTeam1 = _scoreTeam1.value,
            scoreTeam2 = _scoreTeam2.value
        )

        // Delegér forretningslogik til service
        matchResultService.recordMatchResult(match, newResult)

        return match
    }

    fun reset() {
        _scoreTeam1.value = 0
        _scoreTeam2.value = 0
        _currentMatch.value = null
    }
}

