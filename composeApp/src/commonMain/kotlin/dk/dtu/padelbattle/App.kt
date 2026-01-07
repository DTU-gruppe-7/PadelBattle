package dk.dtu.padelbattle

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dk.dtu.padelbattle.data.PadelBattleDatabase
import dk.dtu.padelbattle.view.navigation.BottomNavigationBar
import dk.dtu.padelbattle.view.navigation.NavigationGraph
import dk.dtu.padelbattle.view.navigation.TopBar
import dk.dtu.padelbattle.view.navigation.TournamentView
import dk.dtu.padelbattle.view.navigation.getCurrentScreen
import dk.dtu.padelbattle.viewModel.ChooseTournamentViewModel
import dk.dtu.padelbattle.viewModel.MatchEditViewModel
import dk.dtu.padelbattle.viewModel.MatchListViewModel
import dk.dtu.padelbattle.viewModel.StandingsViewModel
import dk.dtu.padelbattle.viewModel.TournamentConfigViewModel
import dk.dtu.padelbattle.viewModel.TournamentViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    database: PadelBattleDatabase
) {
    // Brug remember til at bevare ViewModels ved recomposition
    val chooseTournamentViewModel = remember { ChooseTournamentViewModel() }
    val tournamentConfigViewModel = remember { TournamentConfigViewModel() }
    val tournamentViewModel = remember { TournamentViewModel() }
    val standingsViewModel = remember { StandingsViewModel() }
    val matchEditViewModel = remember { MatchEditViewModel() }
    val matchListViewModel = remember { MatchListViewModel() }

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