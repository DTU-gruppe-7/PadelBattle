package dk.dtu.padelbattle.presentation.tournament.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.Player
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
 * Kombineret ViewModel til at håndtere turneringsindhold:
 * - Kampeliste og runde-navigation (tidligere MatchListViewModel)
 * - Standings/stilling og bonus-beregning (tidligere StandingsViewModel)
 * - Winner celebration
 */
class TournamentContentViewModel : ViewModel() {

    // ==================== Match List (tidligere MatchListViewModel) ====================

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

        // Find den første kamp, der IKKE er spillet
        val firstUnplayedMatch = matches
            .sortedBy { it.roundNumber }
            .firstOrNull { !it.isPlayed }

        // Bestem hvilken runde der skal vises
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

    fun notifyMatchUpdated() {
        _revision.value++
    }

    // ==================== Standings (tidligere StandingsViewModel) ====================

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    private val _pointsPerMatch = MutableStateFlow(16)

    // ==================== Winner Celebration ====================

    private val _showWinnerCelebration = MutableStateFlow(false)
    val showWinnerCelebration: StateFlow<Boolean> = _showWinnerCelebration.asStateFlow()

    // Holder styr på sidste vinder-ID for at vise popup igen ved ny vinder
    private var lastWinnerId: String? = null
    private var lastCompletedRevision: Int = -1

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
     *
     * @param players Liste af spillere
     * @param pointsPerMatch Point per kamp
     * @param isCompleted Om turneringen er afsluttet
     * @param revision Turneringens revision - bruges til at detektere forlængelser
     */
    fun setPlayers(
        players: List<Player>,
        pointsPerMatch: Int = 16,
        isCompleted: Boolean = false,
        revision: Int = 0
    ) {
        _pointsPerMatch.value = pointsPerMatch
        _players.value = players

        // Tjek om vi skal vise vinder celebration
        if (isCompleted && players.isNotEmpty()) {
            val currentWinnerId = players.maxByOrNull { it.totalPoints }?.id

            // Vis popup hvis:
            // 1. Det er en ny afsluttet revision
            // 2. Eller vinderen har ændret sig
            val isNewCompletion = revision != lastCompletedRevision
            val isNewWinner = currentWinnerId != lastWinnerId

            if (isNewCompletion || isNewWinner) {
                lastWinnerId = currentWinnerId
                lastCompletedRevision = revision
                _showWinnerCelebration.value = true
            }
        }
    }

    /**
     * Luk vinder celebration popup
     */
    fun dismissWinnerCelebration() {
        _showWinnerCelebration.value = false
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
     * @param onSave callback der kaldes med spilleren og det nye navn.
     */
    fun savePlayerName(onSave: (Player, String) -> Unit) {
        val player = _editingPlayer.value ?: return
        val newName = _editingName.value.trim()
        if (newName.isNotBlank()) {
            onSave(player, newName)
        }
        cancelEditing()
    }

    // ==================== Reset ====================

    /**
     * Nulstiller al state i ViewModel.
     * Bruges når man navigerer væk fra en turnering for at undgå stale data
     * og forvirrende winner celebration ved næste turnering.
     */
    fun reset() {
        // Match state
        _matches.value = emptyList()
        _revision.value = 0
        _currentRound.value = 1
        
        // Player/standings state
        _players.value = emptyList()
        _pointsPerMatch.value = 16
        
        // Winner celebration state
        _showWinnerCelebration.value = false
        lastWinnerId = null
        lastCompletedRevision = -1
        
        // Player editing state
        _editingPlayer.value = null
        _editingName.value = ""
    }
}
