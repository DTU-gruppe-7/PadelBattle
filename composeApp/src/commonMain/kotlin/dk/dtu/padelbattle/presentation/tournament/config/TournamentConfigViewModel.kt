package dk.dtu.padelbattle.presentation.tournament.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.domain.model.Player
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.domain.model.TournamentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class TournamentConfigViewModel(
    private val repository: TournamentRepository
) : ViewModel() {

    // === Tournament Type Selection (tidligere ChooseTournamentViewModel) ===
    private val _selectedTournamentType = MutableStateFlow<TournamentType?>(null)
    val selectedTournamentType: StateFlow<TournamentType?> = _selectedTournamentType.asStateFlow()

    fun selectTournamentType(type: TournamentType) {
        _selectedTournamentType.value = type
    }

    // === Tournament Configuration ===
    private val _tournamentName = MutableStateFlow("")
    val tournamentName: StateFlow<String> = _tournamentName.asStateFlow()

    private val _playerNames = MutableStateFlow<List<String>>(emptyList())
    val playerNames: StateFlow<List<String>> = _playerNames.asStateFlow()

    private val _currentPlayerName = MutableStateFlow("")
    val currentPlayerName: StateFlow<String> = _currentPlayerName.asStateFlow()

    private val _numberOfCourts = MutableStateFlow(1)
    val numberOfCourts: StateFlow<Int> = _numberOfCourts.asStateFlow()

    private val _pointsPerMatch = MutableStateFlow(16)
    val pointsPerRound: StateFlow<Int> = _pointsPerMatch.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Minimum antal spillere baseret på antal baner (4 spillere per bane).
     */
    val minimumPlayers: StateFlow<Int> = _numberOfCourts
        .map { courts -> courts * 4 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    /**
     * Om turneringen kan startes (har navn og nok spillere).
     */
    val canStartTournament: StateFlow<Boolean> = combine(
        _tournamentName,
        _playerNames,
        minimumPlayers
    ) { name, players, minPlayers ->
        name.isNotBlank() && players.size >= minPlayers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateTournamentName(name: String) {
        _tournamentName.value = name
    }

    fun updateCurrentPlayerName(name: String) {
        _currentPlayerName.value = name
    }

    fun updateNumberOfCourts(courts: Int) {
        _numberOfCourts.value = courts.coerceIn(1, Tournament.MAX_COURTS)
    }

    fun updatePointsPerMatch(points: Int) {
        _pointsPerMatch.value = points.coerceIn(1, 100)
    }

    fun addPlayer() {
        val name = _currentPlayerName.value.trim()
        if (name.isNotBlank() && _playerNames.value.size < Tournament.MAX_PLAYERS) {
            val newPlayerList = _playerNames.value + name
            _playerNames.value = newPlayerList
            _currentPlayerName.value = ""
        }
    }

    fun removePlayer(index: Int) {
        val currentList = _playerNames.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _playerNames.value = currentList
        }
    }

    /**
     * Opretter en turnering og gemmer til databasen.
     */
    fun createTournament(tournamentType: TournamentType, onSuccess: (Tournament) -> Unit) {
        if (!canStartTournament.value) {
            val minPlayers = minimumPlayers.value
            _error.value = "Ugyldig konfiguration: Kræver navn og mindst $minPlayers spillere for ${_numberOfCourts.value} baner"
            return
        }

        viewModelScope.launch {
            try {
                // Opret Player-objekter fra navnene
                val players = _playerNames.value.map { Player(name = it) }

                // Opret turnering med spillere
                val tournamentConfig = Tournament(
                    name = _tournamentName.value,
                    type = tournamentType,
                    dateCreated = Clock.System.now().toEpochMilliseconds(),
                    numberOfCourts = _numberOfCourts.value,
                    pointsPerMatch = _pointsPerMatch.value,
                    players = players
                )

                // Generer kampe (returnerer ny immutable Tournament)
                val tournament = tournamentConfig.generateInitialMatches()

                // Gem til database via repository
                repository.saveTournament(tournament)

                // Kald success callback
                onSuccess(tournament)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun reset() {
        _selectedTournamentType.value = null
        _tournamentName.value = ""
        _playerNames.value = emptyList()
        _currentPlayerName.value = ""
        _numberOfCourts.value = 1
        _pointsPerMatch.value = 16
        _error.value = null
    }

    /**
     * Populerer konfigurationsfelterne med data fra en eksisterende turnering.
     * Bruges til at duplikere en turnering.
     */
    fun loadTournamentForDuplication(tournamentId: String) {
        viewModelScope.launch {
            try {
                // Hent turneringen fra databasen
                val tournament = repository.getTournamentById(tournamentId)
                if (tournament != null) {
                    // Hent spillere for turneringen
                    val playersInTournament = repository.getPlayersForTournament(tournamentId)

                    // Populer felterne med turneringens data
                    _tournamentName.value = "${tournament.name} (Kopi)"
                    _playerNames.value = playersInTournament.map { it.name }
                    _numberOfCourts.value = tournament.numberOfCourts
                    _pointsPerMatch.value = tournament.pointsPerMatch
                    _currentPlayerName.value = ""
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = "Kunne ikke indlæse turnering: ${e.message}"
            }
        }
    }
}
