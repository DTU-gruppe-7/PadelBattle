package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.mapper.loadFullTournamentFromDao
import dk.dtu.padelbattle.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val tournamentDao: TournamentDao,
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao
) : ViewModel() {

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

    /**
     * Genindlæser turneringen fra databasen.
     * Bruges når nye kampe er blevet genereret og gemt i databasen.
     */
    fun reloadFromDatabase() {
        val currentId = _tournament.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reloadedTournament = loadFullTournamentFromDao(
                    currentId,
                    tournamentDao,
                    playerDao,
                    matchDao
                )
                _tournament.value = reloadedTournament
                _revision.value++
            } catch (e: Exception) {
                _error.value = "Kunne ikke genindlæse turnering: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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
    fun deleteTournament(onSuccess: () -> Unit) {
        val currentId = _tournament.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Slet fra DB (Antager du har mapper funktionen eller kalder dao direkte)
                dk.dtu.padelbattle.data.mapper.deleteTournamentFromDao(currentId, tournamentDao)

                _tournament.value = null
                onSuccess() // Naviger væk
            } catch (e: Exception) {
                _error.value = "Kunne ikke slette: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun notifyTournamentUpdated() {
        _revision.value++
    }

    fun clearError() {
        _error.value = null
    }
}


