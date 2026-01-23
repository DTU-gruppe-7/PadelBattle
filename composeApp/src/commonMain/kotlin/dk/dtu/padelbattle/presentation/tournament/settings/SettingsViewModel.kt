package dk.dtu.padelbattle.presentation.tournament.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.presentation.common.DeleteConfirmationHandler
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsMenuItem
import dk.dtu.padelbattle.presentation.navigation.Screen
import dk.dtu.padelbattle.presentation.navigation.TournamentView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sealed class til at repræsentere forskellige dialog typer
 */
sealed class SettingsDialogType {
    data class EditTournamentName(val currentName: String, val tournamentId: String) : SettingsDialogType()

    data class EditNumberOfCourts(
        val currentCourts: Int,
        val maxCourts: Int,
        val tournamentId: String,
        val hasPlayedMatches: Boolean
    ) : SettingsDialogType()
}

/**
 * ViewModel til at håndtere settings menu.
 * Bestemmer hvilke menu items der skal vises baseret på den aktuelle skærm.
 */
class SettingsViewModel(
    private val repository: TournamentRepository
) : ViewModel() {

    private val _menuItems = MutableStateFlow<List<SettingsMenuItem>?>(null)
    val menuItems: StateFlow<List<SettingsMenuItem>?> = _menuItems.asStateFlow()

    // Fælles handler til delete confirmation dialog
    val deleteConfirmation = DeleteConfirmationHandler()

    private var deleteAction: (() -> Unit)? = null
    private var duplicateAction: (() -> Unit)? = null

    private val _currentDialogType = MutableStateFlow<SettingsDialogType?>(null)
    val currentDialogType: StateFlow<SettingsDialogType?> = _currentDialogType.asStateFlow()

    // Reference til den aktuelle turnering (sættes fra updateScreen)
    private var currentTournament: Tournament? = null
    private var onTournamentUpdated: ((Tournament?) -> Unit)? = null
    private var onCourtsChanged: (() -> Unit)? = null

    private val _showPointsDialog = MutableStateFlow(false)
    val showPointsDialog: StateFlow<Boolean> = _showPointsDialog.asStateFlow()

    private val _showWarningDialog = MutableStateFlow(false)
    val showWarningDialog: StateFlow<Boolean> = _showWarningDialog.asStateFlow()

    private val _pendingPointsChange = MutableStateFlow<Int?>(null)
    val pendingPointsChange: StateFlow<Int?> = _pendingPointsChange.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isUpdatingCourts = MutableStateFlow(false)
    val isUpdatingCourts: StateFlow<Boolean> = _isUpdatingCourts.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        clearCallbacks()
    }

    fun clearCallbacks() {
        deleteAction = null
        duplicateAction = null
        currentTournament = null
        onTournamentUpdated = null
        onCourtsChanged = null
    }

    fun setOnDeleteTournament(action: () -> Unit) {
        deleteAction = action
    }

    fun setOnDuplicateTournament(action: () -> Unit) {
        duplicateAction = action
    }

    fun updateScreen(
        screen: Screen,
        tournament: Tournament? = null,
        onUpdate: ((Tournament?) -> Unit)? = null,
        onCourtsUpdated: (() -> Unit)? = null
    ) {
        currentTournament = tournament
        onTournamentUpdated = onUpdate
        onCourtsChanged = onCourtsUpdated

        _menuItems.value = when (screen) {
            is TournamentView -> getTournamentViewMenuItems()
            else -> null
        }
    }

    private fun getTournamentViewMenuItems(): List<SettingsMenuItem> {
        return listOf(
            SettingsMenuItem("Ændre Turneringsnavn") {
                currentTournament?.let { tournament ->
                    _currentDialogType.value = SettingsDialogType.EditTournamentName(
                        currentName = tournament.name,
                        tournamentId = tournament.id
                    )
                }
            },
            SettingsMenuItem("Ændre Antal Baner") {
                onChangeNumberOfCourts()
            },
            SettingsMenuItem("Ændre Antal Points") {
                onChangePointsPerMatch()
            },
            SettingsMenuItem("Kopier Turnering") {
                duplicateAction?.invoke()
            },
            SettingsMenuItem("Slet Turnering") {
                deleteConfirmation.show { deleteAction?.invoke() }
            }
        )
    }

    fun dismissDialog() {
        _currentDialogType.value = null
    }

    fun updateTournamentName(tournamentId: String, newName: String) {
        viewModelScope.launch {
            try {
                repository.updateTournamentName(tournamentId, newName)
                currentTournament = currentTournament?.copy(name = newName)
                onTournamentUpdated?.invoke(currentTournament)
                dismissDialog()
            } catch (e: Exception) {
                _error.value = "Kunne ikke opdatere navn: ${e.message}"
            }
        }
    }

    private fun onChangeNumberOfCourts() {
        currentTournament?.let { tournament ->
            _currentDialogType.value = SettingsDialogType.EditNumberOfCourts(
                currentCourts = tournament.numberOfCourts,
                maxCourts = tournament.getMaxCourts(),
                tournamentId = tournament.id,
                hasPlayedMatches = tournament.hasPlayedMatches()
            )
        }
    }

    private fun onChangePointsPerMatch() {
        _showPointsDialog.value = true
    }

    fun onPointsSelected(newPoints: Int, tournament: Tournament? = null) {
        // Brug det angivne tournament, ellers fald tilbage til currentTournament
        val effectiveTournament = tournament ?: currentTournament
        if (effectiveTournament == null) {
            _error.value = "Kunne ikke ændre points: Ingen turnering valgt"
            _showPointsDialog.value = false
            return
        }

        // Opdater currentTournament hvis det blev angivet udefra
        if (tournament != null) {
            currentTournament = tournament
        }

        if (effectiveTournament.hasPlayedMatches()) {
            _pendingPointsChange.value = newPoints
            _showWarningDialog.value = true
        } else {
            applyPointsChange(newPoints)
        }

        _showPointsDialog.value = false
    }

    fun confirmPointsChange() {
        val newPoints = _pendingPointsChange.value ?: return
        applyPointsChange(newPoints)
        _showWarningDialog.value = false
        _pendingPointsChange.value = null
    }

    fun cancelPointsChange() {
        _showWarningDialog.value = false
        _pendingPointsChange.value = null
    }

    private fun applyPointsChange(newPoints: Int) {
        val tournament = currentTournament ?: return

        viewModelScope.launch {
            try {
                repository.updatePointsPerMatch(tournament.id, newPoints)
                currentTournament = tournament.copy(pointsPerMatch = newPoints)
                onTournamentUpdated?.invoke(currentTournament)
            } catch (e: Exception) {
                _error.value = "Kunne ikke gemme ændring: ${e.message}"
            }
        }
    }

    fun dismissPointsDialog() {
        _showPointsDialog.value = false
    }

    /**
     * Opdaterer antallet af baner for den aktuelle turnering.
     * Sletter alle eksisterende kampe og genstarter turneringen med det nye antal baner.
     */
    fun updateNumberOfCourts(tournamentId: String, newCourts: Int) {
        val tournament = currentTournament ?: return

        if (tournament.hasPlayedMatches()) {
            _error.value = "Kan ikke ændre antal baner når kampe er blevet spillet"
            return
        }

        viewModelScope.launch {
            try {
                _isUpdatingCourts.value = true

                // Tjek i databasen om der er spillede kampe
                val playedMatchesCount = repository.countPlayedMatches(tournamentId)
                if (playedMatchesCount > 0) {
                    _error.value = "Kan ikke ændre antal baner: $playedMatchesCount kamp(e) er allerede blevet spillet"
                    _isUpdatingCourts.value = false
                    dismissDialog()
                    return@launch
                }

                // Opret ny turnering med opdateret antal baner og generer kampe
                val updatedTournament = withContext(Dispatchers.Default) {
                    tournament
                        .copy(numberOfCourts = newCourts, matches = emptyList())
                        .generateInitialMatches()
                }

                if (updatedTournament.matches.isEmpty()) {
                    throw IllegalStateException("Ingen kampe blev genereret")
                }

                // Double-check før database-operationer
                val finalPlayedMatchesCount = repository.countPlayedMatches(tournamentId)
                if (finalPlayedMatchesCount > 0) {
                    throw IllegalStateException("Kan ikke ændre antal baner: Kampe blev spillet under operationen")
                }

                // Udfør database-operationer
                repository.deleteMatchesByTournament(tournamentId)
                repository.updateNumberOfCourts(tournamentId, newCourts)
                repository.insertMatches(updatedTournament.matches, tournamentId)

                // Opdater lokal reference
                currentTournament = updatedTournament

                onTournamentUpdated?.invoke(currentTournament)
                onCourtsChanged?.invoke()

                _isUpdatingCourts.value = false
                dismissDialog()

            } catch (e: Exception) {
                // Ved fejl forbliver currentTournament uændret (immutable)
                _error.value = "Kunne ikke ændre antal baner: ${e.message}"
                _isUpdatingCourts.value = false
                dismissDialog()
            }
        }
    }
}
