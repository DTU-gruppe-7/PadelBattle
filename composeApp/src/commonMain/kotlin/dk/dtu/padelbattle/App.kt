package dk.dtu.padelbattle

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dk.dtu.padelbattle.data.PadelBattleDatabase
import dk.dtu.padelbattle.view.navigation.BottomNavigationBar
import dk.dtu.padelbattle.view.navigation.NavigationGraph
import dk.dtu.padelbattle.view.navigation.TopBar
import dk.dtu.padelbattle.view.navigation.TournamentConfig
import dk.dtu.padelbattle.view.navigation.TournamentView
import dk.dtu.padelbattle.view.navigation.getCurrentScreen
import dk.dtu.padelbattle.view.SettingsDialogs
import dk.dtu.padelbattle.viewmodel.ChooseTournamentViewModel
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel
import dk.dtu.padelbattle.viewmodel.SettingsViewModel
import dk.dtu.padelbattle.ui.theme.PadelBattleTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    database: PadelBattleDatabase
) {
    // Brug viewModel() til at bevare ViewModels ved configuration changes
    val homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(
            database.tournamentDao(),
            database.playerDao(),
            database.matchDao()
        )
    }
    val chooseTournamentViewModel: ChooseTournamentViewModel = viewModel { ChooseTournamentViewModel() }
    val tournamentConfigViewModel: TournamentConfigViewModel = viewModel {
        TournamentConfigViewModel(
            database.tournamentDao(),
            database.playerDao(),
            database.matchDao()
        )
    }
    val tournamentViewModel: TournamentViewModel = viewModel {
        TournamentViewModel(
            database.tournamentDao(),
            database.playerDao(),
            database.matchDao()
        )
    }
    val standingsViewModel: StandingsViewModel = viewModel { StandingsViewModel() }
    val matchEditViewModel: MatchEditViewModel = viewModel {
        MatchEditViewModel(
            database.matchDao(),
            database.playerDao(),
            database.tournamentDao()
        )
    }
    val matchListViewModel: MatchListViewModel = viewModel { MatchListViewModel() }
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(database.tournamentDao(), database.matchDao())
    }

    PadelBattleTheme {
        val navController = rememberNavController()

        // Sæt delete og duplicate callbacks - disse opdateres når navController ændres
        // Bruger DisposableEffect for at sikre cleanup ved unmount
        androidx.compose.runtime.DisposableEffect(navController) {
            settingsViewModel.setOnDeleteTournament {
                tournamentViewModel.deleteTournament(
                    onSuccess = {
                        // Naviger tilbage til start, når sletningen er færdig
                        navController.popBackStack()
                    }
                )
            }

            settingsViewModel.setOnDuplicateTournament {
                tournamentViewModel.tournament.value?.let { tournament ->
                    // Naviger til TournamentConfig med duplikerings-parametre
                    navController.navigate(
                        TournamentConfig(
                            tournamentType = tournament.type.name,
                            duplicateFromId = tournament.id
                        )
                    )
                }
            }

            onDispose {
                // Ryd callbacks for at undgå memory leaks
                settingsViewModel.clearCallbacks()
            }
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentScreen = getCurrentScreen(backStackEntry)
        var selectedTab by remember { mutableStateOf(0) }

        // Callback til at opdatere selectedTab (bruges af både BottomNavigationBar og TournamentViewScreen)
        val onTabSelected: (Int) -> Unit = { selectedTab = it }

        // Reset selectedTab when navigating away from TournamentView
        LaunchedEffect(currentScreen) {
            if (currentScreen !is TournamentView) {
                selectedTab = 0
            }
            // Reset tournamentConfigViewModel when navigating away from TournamentConfig
            if (currentScreen !is TournamentConfig) {
                tournamentConfigViewModel.reset()
            }
        }

        // Hent turnering og opdater settings menu items baseret på current screen
        val settingsMenuItems by settingsViewModel.menuItems.collectAsState()
        val currentTournament by tournamentViewModel.tournament.collectAsState()

        settingsViewModel.updateScreen(
            screen = currentScreen,
            tournament = currentTournament,
            onUpdate = { tournamentViewModel.notifyTournamentUpdated() },
            onCourtsUpdated = {
                // Når antallet af baner ændres, skal vi resette til runde 1
                currentTournament?.let { tournament ->
                    matchListViewModel.loadTournament(tournament.matches)
                }
            }
        )

        // Håndter navigation når turneringens navn ændres
        val revision by tournamentViewModel.revision.collectAsState()
        LaunchedEffect(currentTournament?.name, revision) {
            currentTournament?.let { tournament ->
                // Naviger til TournamentView med det nye navn for at opdatere topbaren
                if (currentScreen is TournamentView && currentScreen.tournamentName != tournament.name) {
                    navController.navigate(TournamentView(tournamentName = tournament.name)) {
                        popUpTo(TournamentView::class) { inclusive = true }
                    }
                }
            }
        }

        // Vis settings dialoger (håndteres af SettingsDialogs composable)
        SettingsDialogs(settingsViewModel)

        Scaffold(
            topBar = {
                TopBar(
                    currentScreen = currentScreen,
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navigateUp = { navController.navigateUp() },
                    settingsMenuItems = settingsMenuItems,
                    settingsViewModel = settingsViewModel
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // Vis kun bottom bar på TournamentView skærmen
                if (currentScreen is TournamentView) {
                    BottomNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected
                    )
                }
            }
        ) { contentPadding ->
            NavigationGraph(
                navController = navController,
                homeViewModel = homeViewModel,
                chooseTournamentViewModel = chooseTournamentViewModel,
                tournamentConfigViewModel = tournamentConfigViewModel,
                tournamentViewModel = tournamentViewModel,
                standingsViewModel = standingsViewModel,
                matchEditViewModel = matchEditViewModel,
                matchListViewModel = matchListViewModel,
                settingsViewModel = settingsViewModel,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}
