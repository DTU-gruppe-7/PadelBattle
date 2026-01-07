package dk.dtu.padelbattle.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TournamentViewModel : ViewModel() {

    private val _tournament = MutableStateFlow<Tournament?>(null)
    val tournament: StateFlow<Tournament?> = _tournament.asStateFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setTournament(tournament: Tournament) {
        _tournament.value = tournament
    }

    fun updateTournament(updatedTournament: Tournament) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _tournament.value = updatedTournament
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Notificerer at turneringens data er blevet opdateret (f.eks. kampresultater).
     * Dette trigger en recomposition af UI'et ved at inkrementere revision counter.
     */
    fun notifyTournamentUpdated() {
        _revision.value++
    }

    fun clearError() {
        _error.value = null
    }
}


