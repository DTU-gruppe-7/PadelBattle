package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TournamentConfigViewModel : ViewModel() {

    private val _tournamentName = MutableStateFlow("Turnering")
    val tournamentName: StateFlow<String> = _tournamentName.asStateFlow()

    private val _playerNames = MutableStateFlow<List<String>>(emptyList())
    val playerNames: StateFlow<List<String>> = _playerNames.asStateFlow()

    private val _currentPlayerName = MutableStateFlow("")
    val currentPlayerName: StateFlow<String> = _currentPlayerName.asStateFlow()

    private val _numberOfCourts = MutableStateFlow(1)
    val numberOfCourts: StateFlow<Int> = _numberOfCourts.asStateFlow()

    private val _pointsPerRound = MutableStateFlow(16)
    val pointsPerRound: StateFlow<Int> = _pointsPerRound.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateTournamentName(name: String) {
        _tournamentName.value = name
    }

    fun updateCurrentPlayerName(name: String) {
        _currentPlayerName.value = name
    }

    fun updateNumberOfCourts(courts: Int) {
        _numberOfCourts.value = courts.coerceIn(1, 4)
    }

    fun updatePointsPerRound(points: Int) {
        _pointsPerRound.value = points.coerceIn(1, 100)
    }

    fun addPlayer() {
        val name = _currentPlayerName.value.trim()
        if (name.isNotBlank() && _playerNames.value.size < 16) {
            _playerNames.value = _playerNames.value + name
            _currentPlayerName.value = ""
        }
    }

    fun removePlayer(index: Int) {
        _playerNames.value = _playerNames.value.toMutableList().apply { removeAt(index) }
    }

    fun canStartTournament(): Boolean {
        return _tournamentName.value.isNotBlank() && _playerNames.value.size in 4..16
    }

    /**
     * Opretter en turnering baseret på den valgte type og indtastede spillere.
     * Genererer automatisk kampe ved hjælp af turneringens startTournament() metode.
     */
    fun createTournament(tournamentType: TournamentType): Tournament? {
        if (!canStartTournament()) {
            _error.value = "Ugyldig konfiguration: Kræver navn og 4-16 spillere"
            return null
        }

        return try {
            // Opret Player-objekter fra navnene
            val players = _playerNames.value.map { Player(name = it) }.toMutableList()

            // Opret turnering med spillere
            val tournament = Tournament(
                name = _tournamentName.value,
                type = tournamentType,
                dateCreated = 0L, // TODO: Tilføj kotlinx-datetime for rigtig tidsstempel
                numberOfCourts = (players.size / 4).coerceIn(1, 4),
                players = players
            )

            // Generer kampe
            tournament.startTournament()

            tournament
        } catch (e: Exception) {
            _error.value = e.message
            null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun reset() {
        _tournamentName.value = "Tournement"
        _playerNames.value = emptyList()
        _currentPlayerName.value = ""
        _numberOfCourts.value = 1
        _pointsPerRound.value = 16
        _error.value = null
    }
}