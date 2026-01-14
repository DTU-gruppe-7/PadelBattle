package dk.dtu.padelbattle.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dk.dtu.padelbattle.model.TournamentType
import dk.dtu.padelbattle.view.ChooseTournamentScreen
import dk.dtu.padelbattle.view.HomeScreen
import dk.dtu.padelbattle.view.SearchScreen
import dk.dtu.padelbattle.view.TournamentConfigScreen
import dk.dtu.padelbattle.view.TournamentViewScreen
import dk.dtu.padelbattle.viewmodel.ChooseTournamentViewModel
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.SettingsViewModel
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    chooseTournamentViewModel: ChooseTournamentViewModel,
    tournamentConfigViewModel: TournamentConfigViewModel,
    tournamentViewModel: TournamentViewModel,
    standingsViewModel: StandingsViewModel,
    matchEditViewModel: MatchEditViewModel,
    matchListViewModel: MatchListViewModel,
    settingsViewModel: SettingsViewModel,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Fælles navigation callbacks
    val navigateToTournament: (String) -> Unit = { tournamentId ->
        homeViewModel.tournaments.value.find { it.id == tournamentId }?.let { tournament ->
            tournamentViewModel.setTournament(tournament)
            navController.navigate(TournamentView(tournamentName = tournament.name))
        }
    }

    val navigateToDuplicate: (String, String) -> Unit = { tournamentType, tournamentId ->
        navController.navigate(
            TournamentConfig(
                tournamentType = tournamentType,
                duplicateFromId = tournamentId
            )
        )
    }

    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                viewModel = homeViewModel,
                onGoToTournamentScreen = { navController.navigate(ChooseTournament) },
                onTournamentClicked = navigateToTournament,
                onDuplicateTournament = navigateToDuplicate,
                onGoToSearchScreen = { navController.navigate(SearchTournament) }
            )
        }

        composable<SearchTournament> {
            SearchScreen(
                viewModel = homeViewModel,
                onTournamentClicked = navigateToTournament,
                onDuplicateTournament = navigateToDuplicate
            )
        }

        composable<ChooseTournament> {
            ChooseTournamentScreen(
                viewModel = chooseTournamentViewModel,
                onNavigateToPlayers = {
                    val typeName = chooseTournamentViewModel.selectedTournamentType.value?.name ?: "AMERICANO"
                    navController.navigate(TournamentConfig(tournamentType = typeName, duplicateFromId = null))
                }
            )
        }

        composable<TournamentConfig> { backStackEntry ->
            val config = backStackEntry.toRoute<TournamentConfig>()
            val tournamentType = when (config.tournamentType) {
                "MEXICANO" -> TournamentType.MEXICANO
                else -> TournamentType.AMERICANO
            }

            TournamentConfigScreen(
                tournamentType = tournamentType,
                viewModel = tournamentConfigViewModel,
                duplicateFromId = config.duplicateFromId,
                onTournamentCreated = { tournament ->
                    // Gem turneringen i viewmodel
                    tournamentViewModel.setTournament(tournament)

                    // Naviger til turneringsoversigt
                    navController.navigate(TournamentView(tournamentName = tournament.name)) {
                        popUpTo(Home) { inclusive = false }
                    }

                    // Reset config viewmodel til næste turnering
                    tournamentConfigViewModel.reset()
                },
                onGoBack = {
                    tournamentConfigViewModel.reset()
                    navController.popBackStack()
                }
            )
        }

        composable<TournamentView> {
            TournamentViewScreen(
                viewModel = tournamentViewModel,
                standingsViewModel = standingsViewModel,
                matchEditViewModel = matchEditViewModel,
                matchListViewModel = matchListViewModel,
                settingsViewModel = settingsViewModel,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onGoBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

