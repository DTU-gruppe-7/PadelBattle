package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.mapper.toEntitiesWithRelations
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class TournamentConfigViewModel(
    private val tournamentDao: TournamentDao,
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao
) : ViewModel() {

    private val _tournamentName = MutableStateFlow("Turnering")
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

    /**
     * Opretter en turnering baseret på den valgte type og indtastede spillere.
     * Genererer automatisk kampe ved hjælp af turneringens startTournament() metode.
     * Gemmer turneringen til databasen.
     */
    fun createTournament(tournamentType: TournamentType, onSuccess: (Tournament) -> Unit) {
        if (!canStartTournament()) {
            _error.value = "Ugyldig konfiguration: Kræver navn og 4-16 spillere"
            return
        }

        viewModelScope.launch {
            try {
                // Opret Player-objekter fra navnene
                val players = _playerNames.value.map { Player(name = it) }.toMutableList()

                // Opret turnering med spillere
                val tournament = Tournament(
                    name = _tournamentName.value,
                    type = tournamentType,
                    dateCreated = Clock.System.now().toEpochMilliseconds(),
                    numberOfCourts = _numberOfCourts.value,
                    pointsPerMatch = _pointsPerMatch.value,
                    players = players
                )

                // Generer kampe
                tournament.startTournament()

                // Konverter til database entities
                val entities = tournament.toEntitiesWithRelations()

                // Gem til database
                tournamentDao.insertTournament(entities.tournament)
                playerDao.insertPlayers(entities.players)
                matchDao.insertMatches(entities.matches)

                // Kald success callback med den oprettede turnering
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
        _tournamentName.value = "Turnering"
        _playerNames.value = emptyList()
        _currentPlayerName.value = ""
        _numberOfCourts.value = 1
        _pointsPerMatch.value = 16
        _error.value = null
    }

    /**
     * Populerer konfigurationsfelterne med data fra en eksisterende turnering.
     * Bruges til at duplikere en turnering.
     *
     * @param tournamentId ID'et på turneringen der skal duplikeres
     */
    fun loadTournamentForDuplication(tournamentId: String) {
        viewModelScope.launch {
            try {
                // Hent turneringen fra databasen
                val tournamentEntity = tournamentDao.getTournamentById(tournamentId)
                if (tournamentEntity != null) {
                    // Hent spillere for turneringen
                    val playersInTournament = playerDao.getPlayersForTournament(tournamentId)

                    // Populer felterne med turneringens data
                    _tournamentName.value = "${tournamentEntity.name} (Kopi)"
                    _playerNames.value = playersInTournament.map { it.name }
                    _numberOfCourts.value = tournamentEntity.numberOfCourts
                    _pointsPerMatch.value = tournamentEntity.pointsPerMatch
                    _currentPlayerName.value = ""
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = "Kunne ikke indlæse turnering: ${e.message}"
            }
        }
    }
}