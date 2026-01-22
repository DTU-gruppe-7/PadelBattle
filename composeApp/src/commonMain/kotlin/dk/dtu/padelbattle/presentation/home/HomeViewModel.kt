package dk.dtu.padelbattle.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.presentation.common.DeleteConfirmationHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TournamentRepository
) : ViewModel() {

    // Søgetilstand
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Henter alle turneringer via repository
    val tournaments: StateFlow<List<Tournament>> = repository.getAllTournaments()
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
                    repository.deleteTournament(tournament.id)
                    // Listen opdateres automatisk via StateFlow
                } catch (_: Exception) {
                    // Fejl ved sletning ignoreres - brugeren kan prøve igen
                }
            }
        }
    }
}
