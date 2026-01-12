package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
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
    private var deleteAction: (() -> Unit)? = null

    private val _currentDialogType = MutableStateFlow<SettingsDialogType?>(null)
    val currentDialogType: StateFlow<SettingsDialogType?> = _currentDialogType.asStateFlow()

    // Reference til den aktuelle turnering (sættes fra TournamentViewModel)
    private var currentTournament: Tournament? = null
    private var onTournamentUpdated: ((String) -> Unit)? = null

    /**
     * Sætter den aktuelle turnering og callback for opdateringer
     */
    fun setCurrentTournament(tournament: Tournament?, onUpdated: ((String) -> Unit)?) {
        currentTournament = tournament
        onTournamentUpdated = onUpdated
    }

    // 2. Lav en funktion, så App.kt kan "injecte" handlingen
    fun setOnDeleteTournament(action: () -> Unit) {
        deleteAction = action
    }
    /**
     * Opdaterer settings menu items baseret på den aktuelle skærm.
     * @param screen Den nuværende skærm
     */
    fun updateScreen(screen: Screen) {
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
                // TODO: Funktionalitet implementeres senere
                onChangeNumberOfCourts()
            },
            SettingsMenuItem("Ændre antal points") {
                // TODO: Funktionalitet implementeres senere
                onChangePointsPerMatch()
            },
            SettingsMenuItem("Slet turnering") {
                deleteAction?.invoke()
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
                onTournamentUpdated?.invoke(newName)

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
     * TODO: Implementer funktionalitet
     */
    private fun onChangePointsPerMatch() {
        // Placeholder - implementeres senere
    }


    /**
     * Tilføjer custom menu items for specifikke use cases.
     * Kan bruges til at tilføje skærm-specifikke menu items dynamisk.
     */
    fun setCustomMenuItems(items: List<SettingsMenuItem>?) {
        _menuItems.value = items
    }
}

