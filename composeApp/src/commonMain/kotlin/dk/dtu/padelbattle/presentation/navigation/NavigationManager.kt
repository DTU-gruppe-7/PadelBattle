package dk.dtu.padelbattle.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.presentation.home.HomeViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentContentViewModel
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsViewModel
import dk.dtu.padelbattle.presentation.tournament.config.TournamentConfigViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centraliseret navigation manager der håndterer al navigation-logik.
 * Reducerer koblingen mellem App.kt og navigation-komponenter.
 */
class NavigationManager(
    private val navController: NavHostController,
    private val homeViewModel: HomeViewModel,
    private val tournamentViewModel: TournamentViewModel,
    private val tournamentConfigViewModel: TournamentConfigViewModel,
    private val contentViewModel: TournamentContentViewModel,
    private val settingsViewModel: SettingsViewModel
) {
    // Tab state management
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun resetTab() {
        _selectedTab.value = 0
    }

    /**
     * Navigerer til en turnering baseret på ID.
     */
    fun navigateToTournament(tournamentId: String) {
        homeViewModel.tournaments.value.find { it.id == tournamentId }?.let { tournament ->
            tournamentViewModel.setTournament(tournament)
            navController.navigate(TournamentView(tournamentName = tournament.name))
        }
    }

    /**
     * Navigerer til duplikering af en turnering.
     */
    fun navigateToDuplicate(tournamentType: String, tournamentId: String) {
        navController.navigate(
            TournamentConfig(
                tournamentType = tournamentType,
                duplicateFromId = tournamentId
            )
        )
    }

    /**
     * Navigerer til valg af turneringstype.
     */
    fun navigateToChooseTournament() {
        navController.navigate(ChooseTournament)
    }

    /**
     * Navigerer til søgeskærmen.
     */
    fun navigateToSearch() {
        navController.navigate(SearchTournament)
    }

    /**
     * Navigerer til turneringskonfiguration.
     */
    fun navigateToTournamentConfig(tournamentType: String, duplicateFromId: String? = null) {
        navController.navigate(TournamentConfig(tournamentType = tournamentType, duplicateFromId = duplicateFromId))
    }

    /**
     * Håndterer når en turnering er oprettet.
     */
    fun onTournamentCreated(tournament: Tournament) {
        tournamentViewModel.setTournament(tournament)
        navController.navigate(TournamentView(tournamentName = tournament.name)) {
            popUpTo(Home) { inclusive = false }
        }
        tournamentConfigViewModel.reset()
    }

    /**
     * Navigerer tilbage fra turneringskonfiguration.
     */
    fun navigateBackFromConfig() {
        tournamentConfigViewModel.reset()
        navController.popBackStack()
    }

    /**
     * Navigerer tilbage (generel).
     */
    fun navigateBack() {
        navController.navigateUp()
    }

    /**
     * Navigerer tilbage til forrige skærm (pop).
     */
    fun popBackStack() {
        navController.popBackStack()
    }

    /**
     * Opdaterer navigation når turneringens navn ændres.
     */
    fun updateTournamentViewRoute(currentScreen: Screen, tournament: Tournament) {
        if (currentScreen is TournamentView && currentScreen.tournamentName != tournament.name) {
            navController.navigate(TournamentView(tournamentName = tournament.name)) {
                popUpTo(TournamentView::class) { inclusive = true }
            }
        }
    }

    /**
     * Håndterer genindlæsning fra database efter ændringer.
     */
    fun reloadTournamentAndUpdateMatches() {
        tournamentViewModel.reloadFromDatabase { reloadedTournament ->
            reloadedTournament?.let {
                contentViewModel.loadTournament(it.matches)
            }
        }
    }

    /**
     * Sætter callbacks for settings menu (delete/duplicate).
     */
    fun setupSettingsCallbacks() {
        settingsViewModel.setOnDeleteTournament {
            tournamentViewModel.deleteTournament(
                onSuccess = { popBackStack() }
            )
        }

        settingsViewModel.setOnDuplicateTournament {
            tournamentViewModel.tournament.value?.let { tournament ->
                navigateToDuplicate(tournament.type.name, tournament.id)
            }
        }
    }

    /**
     * Rydder callbacks når composable disposes.
     */
    fun clearCallbacks() {
        settingsViewModel.clearCallbacks()
    }

    /**
     * Tjekker om der kan navigeres tilbage.
     */
    fun canNavigateBack(): Boolean {
        return navController.previousBackStackEntry != null
    }

    /**
     * Synkroniserer settings menu med aktuel skærm.
     */
    fun syncSettingsWithScreen(currentScreen: Screen, currentTournament: Tournament?) {
        settingsViewModel.updateScreen(
            screen = currentScreen,
            tournament = currentTournament,
            onUpdate = { tournamentViewModel.notifyTournamentUpdated() },
            onCourtsUpdated = { reloadTournamentAndUpdateMatches() }
        )
    }

    /**
     * Håndterer skærmskift-logik (reset tab, reset config).
     */
    fun onScreenChanged(currentScreen: Screen) {
        if (currentScreen !is TournamentView) {
            resetTab()
        }
        if (currentScreen !is TournamentConfig) {
            tournamentConfigViewModel.reset()
        }
    }
}

/**
 * Composable der sætter alle NavigationManager side-effects op.
 * Bruges i App.kt for at holde den ren.
 */
@Composable
fun NavigationManagerEffects(navigationManager: NavigationManager, currentScreen: Screen) {
    val tournamentViewModel: TournamentViewModel = org.koin.compose.koinInject()
    val currentTournament by tournamentViewModel.tournament.collectAsState()
    val revision by tournamentViewModel.revision.collectAsState()

    // Setup og cleanup af callbacks
    DisposableEffect(navigationManager) {
        navigationManager.setupSettingsCallbacks()
        onDispose {
            navigationManager.clearCallbacks()
        }
    }

    // Håndter skærmskift
    LaunchedEffect(currentScreen) {
        navigationManager.onScreenChanged(currentScreen)
    }

    // Sync settings med skærm
    LaunchedEffect(currentScreen, currentTournament) {
        navigationManager.syncSettingsWithScreen(currentScreen, currentTournament)
    }

    // Håndter navigation når turneringens navn ændres
    LaunchedEffect(currentTournament?.name, revision) {
        currentTournament?.let { tournament ->
            navigationManager.updateTournamentViewRoute(currentScreen, tournament)
        }
    }
}
