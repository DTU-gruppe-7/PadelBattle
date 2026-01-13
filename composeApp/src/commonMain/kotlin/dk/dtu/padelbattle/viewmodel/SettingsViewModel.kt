package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.util.DeleteConfirmationHandler
import androidx.lifecycle.viewModelScope
import dk.dtu.padelbattle.data.dao.TournamentDao
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
}

/**
 * ViewModel til at håndtere settings menu.
 * Bestemmer hvilke menu items der skal vises baseret på den aktuelle skærm.
 */
class SettingsViewModel(
    private val tournamentDao: TournamentDao
) : ViewModel() {

    private val _menuItems = MutableStateFlow<List<SettingsMenuItem>?>(null)
    val menuItems: StateFlow<List<SettingsMenuItem>?> = _menuItems.asStateFlow()

    // Fælles handler til delete confirmation dialog
    val deleteConfirmation = DeleteConfirmationHandler()

    private var deleteAction: (() -> Unit)? = null

    private val _currentDialogType = MutableStateFlow<SettingsDialogType?>(null)
    val currentDialogType: StateFlow<SettingsDialogType?> = _currentDialogType.asStateFlow()

    // Reference til den aktuelle turnering (sættes fra updateScreen)
    private var currentTournament: Tournament? = null
    private var onTournamentUpdated: (() -> Unit)? = null

    private val _showPointsDialog = MutableStateFlow(false)
    val showPointsDialog: StateFlow<Boolean> = _showPointsDialog.asStateFlow()

    private val _showWarningDialog = MutableStateFlow(false)
    val showWarningDialog: StateFlow<Boolean> = _showWarningDialog.asStateFlow()

    private val _pendingPointsChange = MutableStateFlow<Int?>(null)
    val pendingPointsChange: StateFlow<Int?> = _pendingPointsChange.asStateFlow()

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

                // Notificer UI om ændringen (trigger revision opdatering)
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
     * TODO: Implementer funktionalitet
     */
    private fun onChangeNumberOfCourts() {
        // Placeholder - implementeres senere
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
}

