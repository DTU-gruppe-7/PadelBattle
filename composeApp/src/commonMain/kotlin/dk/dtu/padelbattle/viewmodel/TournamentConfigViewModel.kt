package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

class TournamentConfigViewModel : ViewModel() {

    private val _tournamentName = MutableStateFlow("")
    val tournamentName: StateFlow<String> = _tournamentName.asStateFlow()

    private val _playerNames = MutableStateFlow<List<String>>(emptyList())
    val playerNames: StateFlow<List<String>> = _playerNames.asStateFlow()

    private val _currentPlayerName = MutableStateFlow("")
    val currentPlayerName: StateFlow<String> = _currentPlayerName.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    //husk at spørg de andre om det skal ligge nede i modellen istedet
    private val _pointsToWin = MutableStateFlow(32)

    private val _numberOfCourts = MutableStateFlow(1)
    val numberOfCourts: StateFlow<Int> = _numberOfCourts.asStateFlow()


    fun updateTournamentName(name: String) {
        _tournamentName.value = name
    }

    fun updateCurrentPlayerName(name: String) {
        _currentPlayerName.value = name
    }

    // Metode til UI'en der gør at vi kan ændre antal point der skal til for at vinde
    fun setPointsToWin(points: Int) {
        _pointsToWin.value = points
    }

    // Metode til UI'en der gør at vi kan ændre antal baner der skal bruges
    fun setNumberOfCourts(count: Int) {
        val maxCourts = calculateMaxCourts(_playerNames.value.size)
        // Vi sikrer, at man mindst vælger 1 og højest det tilladte antal
        _numberOfCourts.value = count.coerceIn(1, maxCourts)
    }

    fun addPlayer() {
        val name = _currentPlayerName.value.trim()
        if (name.isNotBlank() && _playerNames.value.size < 16) {
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

            //Dette sikrer at vi ikke kan slwtte en spiller hvis det vil resultere i et ulovligt antal baner. Eksempelvis hvis man ville slette den 8. spiller og havde opgivet to baner
            val maxAllowedCourts = calculateMaxCourts(currentList.size)
            if (_numberOfCourts.value > maxAllowedCourts) {
                _numberOfCourts.value = maxAllowedCourts
            }
        }
    }

    private fun calculateMaxCourts(playerCount: Int): Int {
        return (playerCount / 4).coerceAtLeast(1)
    }

    fun canStartTournament(): Boolean {
        return _tournamentName.value.isNotBlank() && _playerNames.value.size >= 4
    }

    fun createTournament(tournamentType: TournamentType): Tournament? {
        if (!canStartTournament()) {
            _error.value = "Ugyldig konfiguration: Kræver navn og mindst 4 spillere."
            return null
        }

        return try {
            val players = _playerNames.value.map { Player(name = it) }.toMutableList()

            val tournament = Tournament(
                name = _tournamentName.value,
                type = tournamentType,
                dateCreated = 0L,
                numberOfCourts = _numberOfCourts.value,
                winScore = _pointsToWin.value,
                players = players,
            )

            tournament.startTournament()
            tournament
        } catch (e: Exception) {
            _error.value = "Fejl ved oprettelse: ${e.message}"
            null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun reset() {
        _tournamentName.value = ""
        _playerNames.value = emptyList()
        _currentPlayerName.value = ""
        _numberOfCourts.value = 1
        _pointsToWin.value = 32
        _error.value = null
    }
}