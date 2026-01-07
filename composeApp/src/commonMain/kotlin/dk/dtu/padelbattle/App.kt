package dk.dtu.padelbattle

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dk.dtu.padelbattle.data.PadelBattleDatabase
import dk.dtu.padelbattle.model.TournamentType
import dk.dtu.padelbattle.view.ChooseTournamentScreen
import dk.dtu.padelbattle.viewModel.ChooseTournamentViewModel
import dk.dtu.padelbattle.view.HomeScreen
import dk.dtu.padelbattle.view.TournamentConfigScreen
import dk.dtu.padelbattle.view.TournamentViewScreen
import dk.dtu.padelbattle.view.navigation.ChoosePlayer
import dk.dtu.padelbattle.view.navigation.ChooseTournament
import dk.dtu.padelbattle.view.navigation.Gameplay
import dk.dtu.padelbattle.view.navigation.Home
import dk.dtu.padelbattle.view.navigation.Screen
import dk.dtu.padelbattle.view.navigation.TopBar
import dk.dtu.padelbattle.view.navigation.TournamentConfig
import dk.dtu.padelbattle.view.navigation.TournamentView
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(database: PadelBattleDatabase, chooseTournamentViewModel: ChooseTournamentViewModel = ChooseTournamentViewModel()) {

    MaterialTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()

        val route = backStackEntry?.destination?.route
        val currentScreen: Screen = when (route) {
            Home::class.qualifiedName -> Home
            ChoosePlayer::class.qualifiedName -> ChoosePlayer
            ChooseTournament::class.qualifiedName -> ChooseTournament
            TournamentView::class.qualifiedName -> TournamentView
            else -> {
                when {
                    route?.startsWith(Gameplay::class.qualifiedName!!) == true ->
                        backStackEntry?.toRoute<Gameplay>() ?: Home
                    route?.startsWith(TournamentConfig::class.qualifiedName!!) == true ->
                        backStackEntry?.toRoute<TournamentConfig>() ?: Home
                    else -> Home
                }
            }
        }

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
                // 2. Definer selve bund-baren
                BottomAppBar {
                    NavigationBar {
                        // 3. Tilføj navigations-elementer
                        // De har en tom onClick-handling, så de "ikke gør noget"

                        NavigationBarItem(
                            selected = true, // Vi lader som om "Spil" er valgt
                            onClick = { /* Gør ingenting */ },
                            icon = {},
                            label = { Text("Spil") }
                        )

                        NavigationBarItem(
                            selected = false,
                            onClick = { /* Gør ingenting */ },
                            icon = {},
                            label = { Text("Scoreboard") }
                        )

                        NavigationBarItem(
                            selected = false,
                            onClick = { /* Gør ingenting */ },
                            icon = {},
                            label = { Text("Indstillinger") }
                        )
                    }
                }
            }
        ) { contentPadding ->

            NavHost(
                navController = navController,
                startDestination = Home,
                modifier = Modifier.padding(contentPadding)
            )
            {
                composable<Home> {
                    HomeScreen(
                        onGoToTournamentScreen = {
                            navController.navigate(ChooseTournament)
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
                        onStartTournament = { name, players ->
                            // Just navigate to tournament view (visual only)
                            navController.navigate(TournamentView) {
                                popUpTo(Home) { inclusive = false }
                            }
                        },
                        onGoBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<TournamentView> {
                    TournamentViewScreen(
                        onGoBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}