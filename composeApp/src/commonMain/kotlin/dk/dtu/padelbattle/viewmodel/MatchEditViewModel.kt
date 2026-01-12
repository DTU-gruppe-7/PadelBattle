package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.MatchResult
import dk.dtu.padelbattle.model.utils.MatchResultService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel til at håndtere redigering af kampresultater.
 * Koordinerer UI-tilstand og delegerer forretningslogik til MatchResultService.
 */
class MatchEditViewModel(
    private val matchDao: MatchDao,
    private val playerDao: PlayerDao,
    private val tournamentDao: TournamentDao
) : ViewModel() {

    private val _pointsPerMatch = MutableStateFlow(1)
    val pointsPerMatch: StateFlow<Int> = _pointsPerMatch.asStateFlow()

    private val matchResultService = MatchResultService(matchDao, playerDao)

    private val _scoreTeam1 = MutableStateFlow(0)
    val scoreTeam1: StateFlow<Int> = _scoreTeam1.asStateFlow()

    private val _scoreTeam2 = MutableStateFlow(0)
    val scoreTeam2: StateFlow<Int> = _scoreTeam2.asStateFlow()

    private val _currentMatch = MutableStateFlow<Match?>(null)
    val currentMatch: StateFlow<Match?> = _currentMatch.asStateFlow()

    /**
     * Initialiserer viewModel med en kamp
     */
    fun setMatch(match: Match, pointsPerMatch: Int) {
        _pointsPerMatch.value = pointsPerMatch
        _currentMatch.value = match
        if (match.isPlayed) {
            _scoreTeam1.value = match.scoreTeam1
            _scoreTeam2.value = match.scoreTeam2
        } else {
            // Standardværdier for nye kampe
            _scoreTeam1.value = (pointsPerMatch / 2)
            _scoreTeam2.value = (pointsPerMatch / 2)
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
     * Gemmer kampresultatet via MatchResultService.
     * Returnerer den opdaterede kamp.
     * @param tournamentId ID på turneringen som kampen tilhører
     * @param onComplete Callback når gemning er færdig - modtager Match og boolean der indikerer om turneringen er afsluttet
     */
    fun saveMatch(tournamentId: String, onComplete: (Match?, Boolean) -> Unit) {
        val match = _currentMatch.value
        if (match == null) {
            onComplete(null, false)
            return
        }

        // Valider tournamentId
        if (tournamentId.isBlank()) {
            println("ERROR: tournamentId is blank, cannot save match")
            onComplete(null, false)
            return
        }

        val newResult = MatchResult(
            scoreTeam1 = _scoreTeam1.value,
            scoreTeam2 = _scoreTeam2.value
        )

        // Delegér forretningslogik til service med fejlhåndtering
        viewModelScope.launch {
            try {
                matchResultService.recordMatchResult(match, newResult, tournamentId)

                // Tjek om dette var den sidste ukampede kamp
                val unplayedCount = matchDao.countUnplayedMatches(tournamentId)
                val isTournamentCompleted = unplayedCount == 0

                if (isTournamentCompleted) {
                    // Marker turneringen som completed
                    tournamentDao.updateTournamentCompleted(tournamentId, true)
                    println("Tournament $tournamentId marked as completed")
                }

                onComplete(match, isTournamentCompleted)
            } catch (e: Exception) {
                println("ERROR saving match: ${e.message}")
                e.printStackTrace()
                onComplete(null, false)
            }
        }

    }

    fun reset() {
        _scoreTeam1.value = 0
        _scoreTeam2.value = 0
        _currentMatch.value = null
    }
}

