package dk.dtu.padelbattle

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dk.dtu.padelbattle.data.PadelBattleDatabase
import dk.dtu.padelbattle.view.navigation.BottomNavigationBar
import dk.dtu.padelbattle.view.navigation.NavigationGraph
import dk.dtu.padelbattle.view.navigation.TopBar
import dk.dtu.padelbattle.view.navigation.TournamentView
import dk.dtu.padelbattle.view.navigation.getCurrentScreen
import dk.dtu.padelbattle.viewmodel.ChooseTournamentViewModel
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    database: PadelBattleDatabase
) {
    // Brug viewModel() til at bevare ViewModels ved configuration changes
    val chooseTournamentViewModel: ChooseTournamentViewModel = viewModel { ChooseTournamentViewModel() }
    val tournamentConfigViewModel: TournamentConfigViewModel = viewModel { TournamentConfigViewModel() }
    val tournamentViewModel: TournamentViewModel = viewModel { TournamentViewModel() }
    val standingsViewModel: StandingsViewModel = viewModel { StandingsViewModel() }
    val matchEditViewModel: MatchEditViewModel = viewModel { MatchEditViewModel() }
    val matchListViewModel: MatchListViewModel = viewModel { MatchListViewModel() }

    MaterialTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentScreen = getCurrentScreen(backStackEntry)

        Scaffold(
            topBar = {
                TopBar(
                    currentScreen = currentScreen,
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navigateUp = { navController.navigateUp() }
                )
            },
            containerColor = Color.LightGray,
            bottomBar = {
                // Vis kun bottom bar på TournamentView skærmen
                if (currentScreen is TournamentView) {
                    BottomNavigationBar()
                }
            }
        ) { contentPadding ->
            NavigationGraph(
                navController = navController,
                chooseTournamentViewModel = chooseTournamentViewModel,
                tournamentConfigViewModel = tournamentConfigViewModel,
                tournamentViewModel = tournamentViewModel,
                standingsViewModel = standingsViewModel,
                matchEditViewModel = matchEditViewModel,
                matchListViewModel = matchListViewModel,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}