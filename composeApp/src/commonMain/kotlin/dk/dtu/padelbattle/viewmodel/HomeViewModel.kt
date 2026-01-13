package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.mapper.deleteTournamentFromDao
import dk.dtu.padelbattle.data.mapper.getAllTournamentsWithDetails
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.util.DeleteConfirmationHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val tournamentDao: TournamentDao,
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao
) : ViewModel() {

    // Søgetilstand
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Henter fulde turneringer med spillere og kampe
    val tournaments: StateFlow<List<Tournament>> = tournamentDao.getAllTournamentsWithDetails(playerDao, matchDao)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // Fælles handler til delete confirmation dialog
    val deleteConfirmation = DeleteConfirmationHandler()

    /**
     * Viser bekræftelsesdialog for sletning af turnering.
     */
    fun showDeleteConfirmationDialog(tournament: Tournament) {
        deleteConfirmation.show {
            viewModelScope.launch {
                try {
                    deleteTournamentFromDao(tournament.id, tournamentDao)
                    // Listen opdateres automatisk via StateFlow
                } catch (e: Exception) {
                    // Log fejl hvis nødvendigt
                    println("Fejl ved sletning af turnering: ${e.message}")
                }
            }
        }
    }
}