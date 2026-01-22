package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.MatchResult
import dk.dtu.padelbattle.model.TournamentType
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
    private val repository: TournamentRepository
) : ViewModel() {

    private val _pointsPerMatch = MutableStateFlow(1)
    val pointsPerMatch: StateFlow<Int> = _pointsPerMatch.asStateFlow()

    private val matchResultService = MatchResultService(repository)

    private val _scoreTeam1 = MutableStateFlow(0)
    val scoreTeam1: StateFlow<Int> = _scoreTeam1.asStateFlow()

    private val _scoreTeam2 = MutableStateFlow(0)
    val scoreTeam2: StateFlow<Int> = _scoreTeam2.asStateFlow()

    private val _currentMatch = MutableStateFlow<Match?>(null)
    val currentMatch: StateFlow<Match?> = _currentMatch.asStateFlow()

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
     * Gemmer kampresultatet via MatchResultService.
     * @param tournamentId ID på turneringen som kampen tilhører
     * @param onComplete Callback når gemning er færdig - modtager Match og boolean der indikerer om turneringen er afsluttet
     */
    fun saveMatch(tournamentId: String, onComplete: (Match?, Boolean) -> Unit) {
        val match = _currentMatch.value
        if (match == null || tournamentId.isBlank()) {
            onComplete(null, false)
            return
        }

        val newResult = MatchResult(
            scoreTeam1 = _scoreTeam1.value,
            scoreTeam2 = _scoreTeam2.value
        )

        viewModelScope.launch {
            try {
                // 1. Gem resultatet
                matchResultService.recordMatchResult(match, newResult, tournamentId)

                // 2. Tjek hvor mange kampe der mangler
                val unplayedCount = repository.countUnplayedMatches(tournamentId)

                // Hvis der stadig er kampe tilbage i runden/turneringen, så fortsæt bare
                if (unplayedCount > 0) {
                    onComplete(match, false)
                    return@launch
                }

                // 3. Hvis 'unplayedCount == 0', skal vi vurdere om vi skal generere nyt eller afslutte
                val tournament = repository.getTournamentById(tournamentId)
                    ?: throw IllegalStateException("Tournament not found: $tournamentId")

                var isCompleted = false

                if (tournament.type == TournamentType.MEXICANO) {
                    val playedMatches = tournament.matches.filter { it.isPlayed }
                    val matchesPerPlayer = tournament.players.associate { player ->
                        player.name to playedMatches.count { m ->
                            m.team1Player1.id == player.id ||
                            m.team1Player2.id == player.id ||
                            m.team2Player1.id == player.id ||
                            m.team2Player2.id == player.id
                        }
                    }
                    println("Mexicano status: ${playedMatches.size} kampe spillet")
                    println("Kampe per spiller: $matchesPerPlayer")

                    val minMatchesPlayed = matchesPerPlayer.values.minOrNull() ?: 0
                    val maxMatchesPlayed = matchesPerPlayer.values.maxOrNull() ?: 0
                    val allHaveEqualMatches = minMatchesPlayed == maxMatchesPlayed

                    println("Minimum kampe spillet: $minMatchesPlayed, Maximum: $maxMatchesPlayed")
                    println("Alle har lige mange kampe: $allHaveEqualMatches")

                    val canCompleteFromTracker = repository.decrementExtensionRounds(tournamentId)
                    println("Extension tracker: canComplete = $canCompleteFromTracker")

                    if (minMatchesPlayed >= 3 && allHaveEqualMatches && canCompleteFromTracker) {
                        println("Alle spillere har spillet mindst 3 kampe og alle har lige mange - turneringen er færdig!")
                        isCompleted = true
                        repository.clearExtensionTracking(tournamentId)
                    } else {
                        println("Turneringen fortsætter - genererer ny runde...")
                        val extendedTournament = tournament.generateExtensionMatches()

                        if (extendedTournament != null) {
                            // Find de nye kampe (forskellen mellem gammel og ny)
                            val existingMatchIds = tournament.matches.map { it.id }.toSet()
                            val newMatches = extendedTournament.matches.filter { it.id !in existingMatchIds }

                            if (newMatches.isNotEmpty()) {
                                println("Genererer runde ${newMatches.first().roundNumber} med ${newMatches.size} kampe")
                                repository.insertMatches(newMatches, tournamentId)
                            } else {
                                println("ADVARSEL: generateExtensionMatches returnerede tournament men ingen nye kampe")
                            }
                        } else {
                            println("ADVARSEL: generateExtensionMatches returnerede null")
                        }
                    }
                } else {
                    // For Americano er alt genereret fra start, så hvis unplayedCount == 0, er vi færdige
                    isCompleted = true
                }

                // 4. Opdater status og giv besked tilbage
                if (isCompleted) {
                    repository.updateTournamentCompleted(tournamentId, true)
                    println("Tournament $tournamentId marked as completed")
                }

                onComplete(match, isCompleted)

            } catch (e: Exception) {
                println("ERROR saving match: ${e.message}")
                e.printStackTrace()
                onComplete(null, false)
            }
        }
    }

    fun reset() {
        val defaultScore = _pointsPerMatch.value / 2
        _scoreTeam1.value = defaultScore
        _scoreTeam2.value = _pointsPerMatch.value - defaultScore
        _currentMatch.value = null
    }
}
