package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.mapper.loadFullTournamentFromDao
import dk.dtu.padelbattle.data.mapper.toEntity
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
                val unplayedCount = matchDao.countUnplayedMatches(tournamentId)

                // Hvis der stadig er kampe tilbage i runden/turneringen, så fortsæt bare
                if (unplayedCount > 0) {
                    onComplete(match, false)
                    return@launch
                }

                // 3. Hvis 'unplayedCount == 0', skal vi vurdere om vi skal generere nyt eller afslutte
                // Hent hele turneringen for at køre logikken
                val tournament = loadFullTournamentFromDao(tournamentId, tournamentDao, playerDao, matchDao)

                var isCompleted = false

                if (tournament.type == TournamentType.MEXICANO) {
                    // Debug info
                    val playedMatches = tournament.matches.filter { it.isPlayed }
                    val matchesPerPlayer = tournament.players.associate { player ->
                        player.name to playedMatches.count { match ->
                            match.team1Player1.id == player.id ||
                            match.team1Player2.id == player.id ||
                            match.team2Player1.id == player.id ||
                            match.team2Player2.id == player.id
                        }
                    }
                    println("Mexicano status: ${playedMatches.size} kampe spillet")
                    println("Kampe per spiller: $matchesPerPlayer")

                    val minMatchesPlayed = matchesPerPlayer.values.minOrNull() ?: 0
                    println("Minimum kampe spillet af nogen spiller: $minMatchesPlayed")

                    // Tjek om alle har spillet min. 3 kampe
                    if (minMatchesPlayed >= 3) {
                        println("Alle spillere har spillet mindst 3 kampe - turneringen er færdig!")
                        isCompleted = true
                    } else {
                        println("Ikke alle har spillet 3 kampe endnu - genererer ny runde...")
                        // Generer næste runde automatisk
                        val addedNewRound = tournament.extendTournament()

                        if (addedNewRound) {
                            // Find de nye kampe (dem der ikke er spillet)
                            val newMatches = tournament.matches.filter { !it.isPlayed }

                            if (newMatches.isNotEmpty()) {
                                println("Genererer runde ${newMatches.first().roundNumber} med ${newMatches.size} kampe")
                                matchDao.insertMatches(newMatches.map { it.toEntity(tournamentId) })
                                // Turneringen fortsætter, så isCompleted forbliver false
                            } else {
                                println("ADVARSEL: extendTournament returnerede true men ingen nye kampe blev genereret")
                            }
                        } else {
                            println("ADVARSEL: extendTournament returnerede false")
                        }
                    }
                } else {
                    // For Americano er alt genereret fra start, så hvis unplayedCount == 0, er vi færdige
                    isCompleted = true
                }

                // 4. Opdater status og giv besked tilbage
                if (isCompleted) {
                    tournamentDao.updateTournamentCompleted(tournamentId, true)
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
        _scoreTeam1.value = 0
        _scoreTeam2.value = 0
        _currentMatch.value = null
    }
}

