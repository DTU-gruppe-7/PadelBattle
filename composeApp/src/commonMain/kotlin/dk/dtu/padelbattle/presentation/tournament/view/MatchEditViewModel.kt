package dk.dtu.padelbattle.presentation.tournament.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.domain.usecase.RecordMatchResultUseCase
import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.MatchResult
import dk.dtu.padelbattle.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel til at håndtere redigering af kampresultater.
 * Koordinerer UI-tilstand og delegerer forretningslogik til RecordMatchResultUseCase.
 */
class MatchEditViewModel(
    private val recordMatchResultUseCase: RecordMatchResultUseCase
) : ViewModel() {

    private val _pointsPerMatch = MutableStateFlow(1)
    val pointsPerMatch: StateFlow<Int> = _pointsPerMatch.asStateFlow()

    private val _scoreTeam1 = MutableStateFlow(0)
    val scoreTeam1: StateFlow<Int> = _scoreTeam1.asStateFlow()

    private val _scoreTeam2 = MutableStateFlow(0)
    val scoreTeam2: StateFlow<Int> = _scoreTeam2.asStateFlow()

    private val _currentMatch = MutableStateFlow<Match?>(null)
    val currentMatch: StateFlow<Match?> = _currentMatch.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<SaveMatchResult>>(UiState.Success(SaveMatchResult.Idle))
    val saveState: StateFlow<UiState<SaveMatchResult>> = _saveState.asStateFlow()

    /**
     * Resultat fra at gemme en kamp.
     */
    sealed class SaveMatchResult {
        data object Idle : SaveMatchResult()
        data class Saved(val match: Match, val tournamentCompleted: Boolean) : SaveMatchResult()
    }

    /**
     * Initialiserer viewModel med en kamp.
     * For uafspillede kampe sættes scores til halvdelen af pointsPerMatch.
     * For allerede spillede kampe vises de gemte scores.
     */
    fun setMatch(match: Match, pointsPerMatch: Int) {
        _pointsPerMatch.value = pointsPerMatch
        _currentMatch.value = match
        
        // Altid start med standard scores (halvdelen af pointsPerMatch)
        val defaultScore = pointsPerMatch / 2
        _scoreTeam1.value = defaultScore
        _scoreTeam2.value = pointsPerMatch - defaultScore  // Håndterer ulige pointsPerMatch
        
        // Kun overskriv med gemte scores hvis kampen faktisk er spillet
        if (match.isPlayed) {
            _scoreTeam1.value = match.scoreTeam1
            _scoreTeam2.value = match.scoreTeam2
        }
    }

    fun updateScoreTeam1(score: Int) {
        if (score >= 0 && score <= pointsPerMatch.value) {
            _scoreTeam1.value = score
            _scoreTeam2.value = pointsPerMatch.value - score
        }
    }

    fun updateScoreTeam2(score: Int) {
        if (score >= 0 && score <= pointsPerMatch.value) {
            _scoreTeam2.value = score
            _scoreTeam1.value = pointsPerMatch.value - score
        }
    }

    fun incrementScoreTeam1() {
        if (_scoreTeam1.value < pointsPerMatch.value) {
            _scoreTeam1.value++
            _scoreTeam2.value = pointsPerMatch.value - _scoreTeam1.value
        }
    }

    fun decrementScoreTeam1() {
        if (_scoreTeam1.value > 0) {
            _scoreTeam1.value--
            _scoreTeam2.value = pointsPerMatch.value - _scoreTeam1.value
        }
    }

    fun incrementScoreTeam2() {
        if (_scoreTeam2.value < pointsPerMatch.value) {
            _scoreTeam2.value++
            _scoreTeam1.value = pointsPerMatch.value - _scoreTeam2.value
        }
    }

    fun decrementScoreTeam2() {
        if (_scoreTeam2.value > 0) {
            _scoreTeam2.value--
            _scoreTeam1.value = pointsPerMatch.value - _scoreTeam2.value
        }
    }

    /**
     * Gemmer kampresultatet via RecordMatchResultUseCase.
     * @param tournamentId ID på turneringen som kampen tilhører
     * @param onComplete Callback når gemning er færdig - modtager Match og boolean der indikerer om turneringen er afsluttet
     */
    fun saveMatch(tournamentId: String, onComplete: (Match?, Boolean) -> Unit) {
        val match = _currentMatch.value
        if (match == null || tournamentId.isBlank()) {
            _saveState.value = UiState.Error("Ugyldig kamp eller turnering")
            onComplete(null, false)
            return
        }

        val newResult = MatchResult(
            scoreTeam1 = _scoreTeam1.value,
            scoreTeam2 = _scoreTeam2.value
        )

        _saveState.value = UiState.Loading

        viewModelScope.launch {
            when (val result = recordMatchResultUseCase(match, newResult, tournamentId)) {
                is RecordMatchResultUseCase.Result.MatchSaved -> {
                    _saveState.value = UiState.Success(SaveMatchResult.Saved(result.match, false))
                    onComplete(result.match, false)
                }
                is RecordMatchResultUseCase.Result.TournamentCompleted -> {
                    _saveState.value = UiState.Success(SaveMatchResult.Saved(result.match, true))
                    onComplete(result.match, true)
                }
                is RecordMatchResultUseCase.Result.NewRoundGenerated -> {
                    _saveState.value = UiState.Success(SaveMatchResult.Saved(result.match, false))
                    onComplete(result.match, false)
                }
                is RecordMatchResultUseCase.Result.Error -> {
                    _saveState.value = UiState.Error(result.message, result.exception)
                    onComplete(null, false)
                }
            }
        }
    }

    /**
     * Nulstiller save state til idle.
     */
    fun clearSaveState() {
        _saveState.value = UiState.Success(SaveMatchResult.Idle)
    }

    fun reset() {
        val defaultScore = _pointsPerMatch.value / 2
        _scoreTeam1.value = defaultScore
        _scoreTeam2.value = _pointsPerMatch.value - defaultScore
        _currentMatch.value = null
    }
}
