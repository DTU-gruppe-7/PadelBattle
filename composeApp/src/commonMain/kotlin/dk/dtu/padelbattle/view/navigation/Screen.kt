package dk.dtu.padelbattle.view.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dk.dtu.padelbattle.view.ChooseTournamentScreen
import dk.dtu.padelbattle.viewmodel.ChooseTournamentViewModel
import kotlinx.serialization.Serializable

sealed interface Screen {
    val title: String
}

@Serializable
object Home: Screen {
    override val title = "Padel Battle"
}

@Serializable
object ChoosePlayer: Screen {
    override val title = "Vælg Spillere"
}

@Serializable
object ChooseTournament: Screen {
    override val title = "Vælg Turnering"
}

@Serializable
data class TournamentConfig(val tournamentType: String): Screen {
    override val title = "Opsæt Turnering"
}

@Serializable
object TournamentView: Screen {
    override val title = "Turnering"
}

@Serializable
data class Gameplay(val playerNames: String): Screen {
    override val title = "Kamp"
}

@Composable
fun AppNavigation(chooseTournamentViewModel: ChooseTournamentViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chooseTournament") {
        composable("chooseTournament") {
            ChooseTournamentScreen(
                viewModel = chooseTournamentViewModel,
                onNavigateToPlayers = {
                    navController.navigate("selectPlayers")
                }
            )
        }
        composable("selectPlayers") {
            // SelectPlayersScreen - can access tournamentViewModel.selectedTournamentType
        }
    }
}