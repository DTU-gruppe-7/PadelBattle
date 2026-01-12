package dk.dtu.padelbattle.viewmodel

import androidx.lifecycle.ViewModel
import dk.dtu.padelbattle.view.SettingsMenuItem
import dk.dtu.padelbattle.view.navigation.Screen
import dk.dtu.padelbattle.view.navigation.TournamentView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel til at håndtere settings menu.
 * Bestemmer hvilke menu items der skal vises baseret på den aktuelle skærm.
 */
class SettingsViewModel : ViewModel() {

    private val _menuItems = MutableStateFlow<List<SettingsMenuItem>?>(null)
    val menuItems: StateFlow<List<SettingsMenuItem>?> = _menuItems.asStateFlow()

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
            SettingsMenuItem("Ændre antal baner") {
                // TODO: Funktionalitet implementeres senere
                onChangeNumberOfCourts()
            },
            SettingsMenuItem("Ændre antal points") {
                // TODO: Funktionalitet implementeres senere
                onChangePointsPerMatch()
            },
            SettingsMenuItem("Slet turnering") {
                // TODO: Funktionalitet implementeres senere
                onDeleteTournament()
            }
        )
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
     * Håndterer sletning af turnering.
     * TODO: Implementer funktionalitet
     */
    private fun onDeleteTournament() {
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

