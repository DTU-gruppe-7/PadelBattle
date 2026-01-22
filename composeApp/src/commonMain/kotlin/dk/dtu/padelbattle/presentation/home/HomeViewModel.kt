package dk.dtu.padelbattle.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.domain.model.TournamentSummary
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

    // Valgt fane på home screen (0 = igangværende, 1 = afsluttede)
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    // Søgetilstand
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Henter alle turneringer som letvægts-summaries.
     * Mere effektivt end at loade alle spillere og kampe for hver turnering.
     */
    val tournaments: StateFlow<List<TournamentSummary>> = repository.getAllTournamentSummaries()
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
    fun showDeleteConfirmationDialog(tournament: TournamentSummary) {
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
