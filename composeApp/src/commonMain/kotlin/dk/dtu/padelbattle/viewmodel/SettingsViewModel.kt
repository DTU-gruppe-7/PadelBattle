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
    private var duplicateAction: (() -> Unit)? = null

    private val _currentDialogType = MutableStateFlow<SettingsDialogType?>(null)
    val currentDialogType: StateFlow<SettingsDialogType?> = _currentDialogType.asStateFlow()

    // Reference til den aktuelle turnering (sættes fra updateScreen)
    private var currentTournament: Tournament? = null
    private var onTournamentUpdated: (() -> Unit)? = null
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

    // 2. Lav en funktion, så App.kt kan "injecte" handlingen
    /**
     * Rydder alle callbacks og referencer for at undgå memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        clearCallbacks()
    }

    /**
     * Rydder alle callbacks - kaldes ved navigation væk eller ViewModel cleanup.
     */
    fun clearCallbacks() {
        deleteAction = null
        duplicateAction = null
        currentTournament = null
        onTournamentUpdated = null
        onCourtsChanged = null
    }

    /**
     * Sætter callback-funktionen for at slette en turnering.
     * VIGTIGT: Kald clearCallbacks() når komponenten unmountes for at undgå memory leaks.
     */
    fun setOnDeleteTournament(action: () -> Unit) {
        deleteAction = action
    }

    /**
     * Sætter callback-funktionen for at duplikere en turnering.
     * Bruges til at navigere til TournamentConfigScreen med duplicate-parametrene.
     * VIGTIGT: Kald clearCallbacks() når komponenten unmountes for at undgå memory leaks.
     */
    fun setOnDuplicateTournament(action: () -> Unit) {
        duplicateAction = action
    }

    /**
     * Opdaterer settings menu items baseret på den aktuelle skærm.
     * @param screen Den nuværende skærm
     * @param tournament Den aktuelle turnering (kun relevant for TournamentView)
     * @param onUpdate Callback når turneringen opdateres
     * @param onCourtsUpdated Callback når antallet af baner ændres (brug dette til at resette til runde 1)
     */
    fun updateScreen(
        screen: Screen,
        tournament: Tournament? = null,
        onUpdate: (() -> Unit)? = null,
        onCourtsUpdated: (() -> Unit)? = null
    ) {
        currentTournament = tournament
        onTournamentUpdated = onUpdate
        onCourtsChanged = onCourtsUpdated

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
     * Gemmer ændringen i databasen, så den gælder for alle resterende runder
     * og eventuelle forlængelser af spillet.
     */
    private fun applyPointsChange(newPoints: Int) {
        val tournament = currentTournament ?: return

        viewModelScope.launch {
            try {
                // Gem ændringen i databasen
                tournamentDao.updatePointsPerMatch(tournament.id, newPoints)

                // Opdater modellen
                tournament.pointsPerMatch = newPoints

                // Notificer UI om ændringen
                onTournamentUpdated?.invoke()
            } catch (e: Exception) {
                _error.value = "Kunne ikke gemme ændring: ${e.message}"
            }
        }
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
     *
     * Bruger en transaktionel tilgang med rollback for at sikre data konsistens:
     * 1. Viser loading state i dialogen
     * 2. Tjekker i database om der er spillede kampe (for at undgå race condition)
     * 3. Gemmer backup af gamle kampe
     * 4. Genererer nye kampe i memory
     * 5. Udfører alle database-operationer atomært
     * 6. Ved fejl: ruller tilbage til original tilstand
     * 7. Lukker dialog kun når alt er færdigt eller ved fejl
     */
    fun updateNumberOfCourts(tournamentId: String, newCourts: Int) {
        val tournament = currentTournament ?: return

        // Sikkerhedstjek: Må kun ændre hvis ingen kampe er spillet (initial check)
        if (tournament.hasPlayedMatches()) {
            _error.value = "Kan ikke ændre antal baner når kampe er blevet spillet"
            return
        }

        viewModelScope.launch {
            // Gem backup af original tilstand for rollback (UDENFOR try-catch så det kan bruges i catch)
            val oldNumberOfCourts = tournament.numberOfCourts
            val oldMatches = tournament.matches.toList() // Lav en kopi af listen

            try {
                // Vis loading state
                _isUpdatingCourts.value = true

                // KRITISK: Tjek i databasen om der er spillede kampe for at undgå race condition
                // Dette sikrer at selv hvis en kamp bliver markeret som spillet i en anden coroutine,
                // så fanger vi det her
                val playedMatchesCount = matchDao.countPlayedMatches(tournamentId)
                if (playedMatchesCount > 0) {
                    _error.value = "Kan ikke ændre antal baner: $playedMatchesCount kamp(e) er allerede blevet spillet"
                    _isUpdatingCourts.value = false
                    dismissDialog()
                    return@launch
                }

                // Opdater antallet af baner i modellen midlertidigt
                tournament.numberOfCourts = newCourts

                // Ryd lokale kampe
                tournament.matches.clear()

                // Genstart turneringen (genererer nye kampe) - kør på Default dispatcher for CPU-intensivt arbejde
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val success = tournament.startTournament()
                    if (!success) {
                        throw IllegalStateException("Kunne ikke generere kampe for turneringen")
                    }
                }

                // Valider at der blev genereret kampe
                if (tournament.matches.isEmpty()) {
                    throw IllegalStateException("Ingen kampe blev genereret")
                }

                // Konverter kampe til entities først (før database-operationer)
                val newMatchEntities = tournament.matches.map { it.toEntity(tournamentId) }

                // Nu udfør alle database-operationer sekventielt med explicit error handling
                try {
                    // KRITISK: Double-check lige før vi sletter - sidste chance for at undgå race condition
                    val finalPlayedMatchesCount = matchDao.countPlayedMatches(tournamentId)
                    if (finalPlayedMatchesCount > 0) {
                        throw IllegalStateException("Kan ikke ændre antal baner: Kampe blev spillet under operationen")
                    }

                    // 1. Slet gamle kampe
                    matchDao.deleteMatchesByTournament(tournamentId)

                    // 2. Opdater antal baner
                    tournamentDao.updateNumberOfCourts(tournamentId, newCourts)

                    // 3. Indsæt nye kampe
                    matchDao.insertMatches(newMatchEntities)

                    // Hvis vi når hertil, er alle operationer lykkedes
                    // Notificer UI om ændringen
                    onTournamentUpdated?.invoke()

                    // Notificer specifikt om at baner er ændret (så UI kan resette til runde 1)
                    onCourtsChanged?.invoke()

                    // Skjul loading og luk dialog ved success
                    _isUpdatingCourts.value = false
                    dismissDialog()

                } catch (dbException: Exception) {
                    // Database-operation fejlede - forsøg rollback
                    throw Exception("Database-operation fejlede: ${dbException.message}", dbException)
                }

            } catch (e: Exception) {
                // Noget gik galt - rollback til original tilstand
                tournament.numberOfCourts = oldNumberOfCourts
                tournament.matches.clear()
                tournament.matches.addAll(oldMatches)

                // Forsøg at genoprette database-tilstanden
                try {
                    // Slet eventuelle delvist indsatte kampe
                    matchDao.deleteMatchesByTournament(tournamentId)

                    // Genopret originale kampe i databasen
                    val oldMatchEntities = oldMatches.map { it.toEntity(tournamentId) }
                    matchDao.insertMatches(oldMatchEntities)

                    // Genopret original numberOfCourts
                    tournamentDao.updateNumberOfCourts(tournamentId, oldNumberOfCourts)

                } catch (rollbackException: Exception) {
                    // Rollback fejlede også - dette er en kritisk fejl
                    _error.value = "Kritisk fejl: Kunne ikke rulle tilbage ændringer. ${rollbackException.message}"
                    _isUpdatingCourts.value = false
                    // Luk dialogen ved kritisk fejl
                    dismissDialog()
                    return@launch
                }

                // Vis fejlmeddelelse til brugeren
                _error.value = "Kunne ikke ændre antal baner: ${e.message}"
                _isUpdatingCourts.value = false
                // Luk dialogen ved fejl
                dismissDialog()
            }
        }
    }
}
