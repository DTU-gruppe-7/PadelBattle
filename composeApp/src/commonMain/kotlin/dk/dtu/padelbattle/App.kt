package dk.dtu.padelbattle

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dk.dtu.padelbattle.data.PadelBattleDatabase
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.data.repository.TournamentRepositoryImpl
import dk.dtu.padelbattle.ui.theme.PadelBattleTheme
import dk.dtu.padelbattle.view.SettingsDialogs
import dk.dtu.padelbattle.view.navigation.BottomNavigationBar
import dk.dtu.padelbattle.view.navigation.NavigationGraph
import dk.dtu.padelbattle.view.navigation.TopBar
import dk.dtu.padelbattle.view.navigation.TournamentConfig
import dk.dtu.padelbattle.view.navigation.TournamentView
import dk.dtu.padelbattle.view.navigation.getCurrentScreen
import dk.dtu.padelbattle.viewmodel.ChooseTournamentViewModel
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.SettingsViewModel
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    database: PadelBattleDatabase
) {
    // Opret repository (én gang per app-session)
    val repository: TournamentRepository = remember {
        TournamentRepositoryImpl(
            tournamentDao = database.tournamentDao(),
            playerDao = database.playerDao(),
            matchDao = database.matchDao()
        )
    }

    // ViewModels bruger nu repository i stedet for DAOs
    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(repository) }
    val chooseTournamentViewModel: ChooseTournamentViewModel = viewModel { ChooseTournamentViewModel() }
    val tournamentConfigViewModel: TournamentConfigViewModel = viewModel { TournamentConfigViewModel(repository) }
    val tournamentViewModel: TournamentViewModel = viewModel { TournamentViewModel(repository) }
    val standingsViewModel: StandingsViewModel = viewModel { StandingsViewModel() }
    val matchEditViewModel: MatchEditViewModel = viewModel { MatchEditViewModel(repository) }
    val matchListViewModel: MatchListViewModel = viewModel { MatchListViewModel() }
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(repository) }

    PadelBattleTheme {
        val navController = rememberNavController()

        // Sæt delete og duplicate callbacks
        DisposableEffect(navController) {
            settingsViewModel.setOnDeleteTournament {
                tournamentViewModel.deleteTournament(
                    onSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            settingsViewModel.setOnDuplicateTournament {
                tournamentViewModel.tournament.value?.let { tournament ->
                    navController.navigate(
                        TournamentConfig(
                            tournamentType = tournament.type.name,
                            duplicateFromId = tournament.id
                        )
                    )
                }
            }

            onDispose {
                settingsViewModel.clearCallbacks()
            }
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentScreen = getCurrentScreen(backStackEntry)
        var selectedTab by remember { mutableStateOf(0) }

        val onTabSelected: (Int) -> Unit = { selectedTab = it }

        // Reset selectedTab when navigating away from TournamentView
        LaunchedEffect(currentScreen) {
            if (currentScreen !is TournamentView) {
                selectedTab = 0
            }
            if (currentScreen !is TournamentConfig) {
                tournamentConfigViewModel.reset()
            }
        }

        val settingsMenuItems by settingsViewModel.menuItems.collectAsState()
        val currentTournament by tournamentViewModel.tournament.collectAsState()

        settingsViewModel.updateScreen(
            screen = currentScreen,
            tournament = currentTournament,
            onUpdate = { tournamentViewModel.notifyTournamentUpdated() },
            onCourtsUpdated = {
                currentTournament?.let { tournament ->
                    matchListViewModel.loadTournament(tournament.matches)
                }
            }
        )

        // Håndter navigation når turneringens navn ændres
        val revision by tournamentViewModel.revision.collectAsState()
        LaunchedEffect(currentTournament?.name, revision) {
            currentTournament?.let { tournament ->
                if (currentScreen is TournamentView && currentScreen.tournamentName != tournament.name) {
                    navController.navigate(TournamentView(tournamentName = tournament.name)) {
                        popUpTo(TournamentView::class) { inclusive = true }
                    }
                }
            }
        }

        // Vis settings dialoger
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
