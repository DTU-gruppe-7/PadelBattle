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
import dk.dtu.padelbattle.view.TournamentConfigScreen
import dk.dtu.padelbattle.view.TournamentViewScreen
import dk.dtu.padelbattle.viewmodel.ChooseTournamentViewModel
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
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
    selectedTab: Int = 0,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                viewModel = homeViewModel,
                onGoToTournamentScreen = {
                    navController.navigate(ChooseTournament)
                },
                onTournamentClicked = { tournamentId ->
                    // Find the tournament from the current state
                    val tournament = homeViewModel.tournaments.value.find { it.id == tournamentId }
                    if (tournament != null) {
                        tournamentViewModel.setTournament(tournament)
                        navController.navigate(TournamentView(tournamentName = tournament.name))
                    }
                }
            )
        }

        composable<ChooseTournament> {
            ChooseTournamentScreen(
                viewModel = chooseTournamentViewModel,
                onNavigateToPlayers = {
                    val typeName = chooseTournamentViewModel.selectedTournamentType.value?.name ?: "AMERICANO"
                    navController.navigate(TournamentConfig(tournamentType = typeName))
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
                selectedTab = selectedTab,
                onGoBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

