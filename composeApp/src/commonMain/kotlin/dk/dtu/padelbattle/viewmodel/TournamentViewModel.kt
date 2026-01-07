package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dk.dtu.padelbattle.model.TournamentType

class TournamentViewModel : ViewModel() {

    private val _selectedTournamentType = MutableStateFlow<TournamentType?>(null)
    val selectedTournamentType: StateFlow<TournamentType?> = _selectedTournamentType.asStateFlow()

    fun selectTournamentType(type: TournamentType) {
        _selectedTournamentType.value = type
    }

    fun clearSelection() {
        _selectedTournamentType.value = null
    }
}