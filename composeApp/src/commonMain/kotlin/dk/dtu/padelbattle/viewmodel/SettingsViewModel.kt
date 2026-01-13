package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.util.DeleteConfirmationHandler
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.mapper.toEntity
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.view.SettingsMenuItem
import dk.dtu.padelbattle.view.navigation.Screen
import dk.dtu.padelbattle.view.navigation.TournamentView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class til at repræsentere forskellige dialog typer
 */
sealed class SettingsDialogType {
    data class EditTournamentName(val currentName: String, val tournamentId: String) : SettingsDialogType()
    // Tilføj flere dialog typer her efterhånden

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
    private val tournamentDao: TournamentDao,
    private val matchDao: MatchDao
) : ViewModel() {

    private val _menuItems = MutableStateFlow<List<SettingsMenuItem>?>(null)
    val menuItems: StateFlow<List<SettingsMenuItem>?> = _menuItems.asStateFlow()

    // Fælles handler til delete confirmation dialog
    val deleteConfirmation = DeleteConfirmationHandler()

    private var deleteAction: (() -> Unit)? = null

    private val _currentDialogType = MutableStateFlow<SettingsDialogType?>(null)
    val currentDialogType: StateFlow<SettingsDialogType?> = _currentDialogType.asStateFlow()

    // Reference til den aktuelle turnering (sættes fra TournamentViewModel)
    private var currentTournament: Tournament? = null
    private var onTournamentUpdated: (() -> Unit)? = null

    /**
     * Sætter den aktuelle turnering og callback for opdateringer
     */
    fun setCurrentTournament(tournament: Tournament?, onUpdated: (() -> Unit)?) {
        currentTournament = tournament
        onTournamentUpdated = onUpdated
    }

    private val _showPointsDialog = MutableStateFlow(false)
    val showPointsDialog: StateFlow<Boolean> = _showPointsDialog.asStateFlow()

    private val _showWarningDialog = MutableStateFlow(false)
    val showWarningDialog: StateFlow<Boolean> = _showWarningDialog.asStateFlow()

    private val _pendingPointsChange = MutableStateFlow<Int?>(null)
    val pendingPointsChange: StateFlow<Int?> = _pendingPointsChange.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    // 2. Lav en funktion, så App.kt kan "injecte" handlingen
    fun setOnDeleteTournament(action: () -> Unit) {
        deleteAction = action
    }
    /**
     * Opdaterer settings menu items baseret på den aktuelle skærm.
     * @param screen Den nuværende skærm
     * @param tournament Den aktuelle turnering (kun relevant for TournamentView)
     * @param onUpdate Callback når turneringen opdateres
     */
    fun updateScreen(
        screen: Screen,
        tournament: Tournament? = null,
        onUpdate: (() -> Unit)? = null
    ) {
        currentTournament = tournament
        onTournamentUpdated = onUpdate

        _menuItems.value = when (screen) {
            is TournamentView -> getTournamentViewMenuItems()
            else -> null // Ingen settings menu på andre skærme
        }
    }

    /**
     * Returnerer menu items for TournamentView skærmen.
     */
    private fun getTournamentViewMenuItems(): List<SettingsMenuItem> {
        return listOf(
            SettingsMenuItem("Ændr turneringsnavn") {
                currentTournament?.let { tournament ->
                    _currentDialogType.value = SettingsDialogType.EditTournamentName(
                        currentName = tournament.name,
                        tournamentId = tournament.id
                    )
                }
            },
            SettingsMenuItem("Ændre antal baner") {
                onChangeNumberOfCourts()
            },
            SettingsMenuItem("Ændre antal points") {
                onChangePointsPerMatch()
            },
            SettingsMenuItem("Slet turnering") {
                deleteConfirmation.show {
                    deleteAction?.invoke()
                }
            }
        )
    }

    /**
     * Lukker den aktuelle dialog
     */
    fun dismissDialog() {
        _currentDialogType.value = null
    }

    /**
     * Opdaterer turneringsnavnet i databasen og modellen
     */
    fun updateTournamentName(tournamentId: String, newName: String) {
        viewModelScope.launch {
            try {
                // Opdater i databasen
                tournamentDao.updateTournamentName(tournamentId, newName)

                // Opdater i den lokale model
                currentTournament?.name = newName

                // Notificer UI om ændringen
                onTournamentUpdated?.invoke()

                // Luk dialogen
                dismissDialog()
            } catch (e: Exception) {
                // TODO: Håndter fejl
            }
        }
    }

    /**
     * Håndterer ændring af antal baner.
     * Viser dialog til brugeren.
     */
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

    /**
     * Håndterer ændring af antal points per kamp.
     * Viser dialog til brugeren.
     */
    private fun onChangePointsPerMatch() {
        _showPointsDialog.value = true
    }

    /**
     * Kaldes når brugeren vælger et nyt antal points.
     * Tjekker om der er spillede kampe og viser advarsel hvis nødvendigt.
     */
    fun onPointsSelected(newPoints: Int) {
        val tournament = currentTournament ?: return

        if (tournament.hasPlayedMatches()) {
            // Der er spillede kampe - vis advarsel
            _pendingPointsChange.value = newPoints
            _showWarningDialog.value = true
        } else {
            // Ingen spillede kampe - opdater direkte
            applyPointsChange(newPoints)
        }

        _showPointsDialog.value = false
    }

    /**
     * Kaldes når brugeren bekræfter ændringen trods advarslen.
     */
    fun confirmPointsChange() {
        val newPoints = _pendingPointsChange.value ?: return
        applyPointsChange(newPoints)
        _showWarningDialog.value = false
        _pendingPointsChange.value = null
    }

    /**
     * Kaldes når brugeren annullerer ændringen.
     */
    fun cancelPointsChange() {
        _showWarningDialog.value = false
        _pendingPointsChange.value = null
    }

    /**
     * Anvender den nye points værdi på turneringen.
     */
    private fun applyPointsChange(newPoints: Int) {
        val tournament = currentTournament ?: return
        tournament.pointsPerMatch = newPoints
        onTournamentUpdated?.invoke()
    }


    /**
     * Tilføjer custom menu items for specifikke use cases.
     * Kan bruges til at tilføje skærm-specifikke menu items dynamisk.
     */
    fun setCustomMenuItems(items: List<SettingsMenuItem>?) {
        _menuItems.value = items
    }

    fun dismissPointsDialog() {
        _showPointsDialog.value = false
    }

    /**
     * Opdaterer antallet af baner for den aktuelle turnering.
     * Sletter alle eksisterende kampe og genstarter turneringen med det nye antal baner.
     * Dette er kun tilladt hvis ingen kampe er blevet spillet endnu.
     */
    fun updateNumberOfCourts(tournamentId: String, newCourts: Int) {
        val tournament = currentTournament ?: return

        // Sikkerhedstjek: Må kun ændre hvis ingen kampe er spillet
        if (tournament.hasPlayedMatches()) {
            return
        }

        viewModelScope.launch {
            try {
                // Luk dialogen først for bedre UX
                dismissDialog()

                // Opdater antallet af baner i modellen
                tournament.numberOfCourts = newCourts

                // Slet alle kampe fra databasen
                matchDao.deleteMatchesByTournament(tournamentId)

                // Ryd lokale kampe først
                tournament.matches.clear()

                // Genstart turneringen (genererer nye kampe) - kør på Default dispatcher for CPU-intensivt arbejde
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    tournament.startTournament()
                }

                // Opdater turneringen i databasen
                tournamentDao.updateNumberOfCourts(tournamentId, newCourts)

                // Gem de nye kampe til databasen
                val matchEntities = tournament.matches.map { it.toEntity(tournamentId) }
                matchDao.insertMatches(matchEntities)

                // Notificer UI om ændringen
                onTournamentUpdated?.invoke()
            } catch (e: Exception) {
                _error.value = "Kunne ikke ændre antal baner: ${e.message}"
            }
        }
    }
}
