package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.mapper.getAllTournamentsWithDetails
import dk.dtu.padelbattle.model.Tournament
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val tournamentDao: TournamentDao,
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao
) : ViewModel() {

    // Henter fulde turneringer med spillere og kampe
    val tournaments: StateFlow<List<Tournament>> = tournamentDao.getAllTournamentsWithDetails(playerDao, matchDao)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Sletter en turnering fra databasen.
     * TODO: Implementer denne metode når slet-funktionaliteten skal laves
     *
     * @param tournament Turneringen der skal slettes
     */
    fun deleteTournament(tournament: Tournament) {//TODO: DELETE WHEN MERGED
        viewModelScope.launch {
            // TODO: Implementer sletning af turnering
            // tournamentDao.deleteTournamentById(tournament.id)
            // playerDao.deletePlayersByTournament(tournament.id)
            // matchDao.deleteMatchesByTournament(tournament.id)
        }
    }
}