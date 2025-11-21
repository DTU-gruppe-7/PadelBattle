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
import dk.dtu.padelbattle.ui.ChoosePlayerScreen
import dk.dtu.padelbattle.ui.ChooseTournamentScreen
import dk.dtu.padelbattle.ui.GamePlayScreen
import dk.dtu.padelbattle.ui.HomeScreen
import dk.dtu.padelbattle.ui.navigation.ChoosePlayer
import dk.dtu.padelbattle.ui.navigation.ChooseTournament
import dk.dtu.padelbattle.ui.navigation.Gameplay
import dk.dtu.padelbattle.ui.navigation.Home
import dk.dtu.padelbattle.ui.navigation.Screen
import dk.dtu.padelbattle.ui.navigation.TopBar
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()

        val route = backStackEntry?.destination?.route
        val currentScreen: Screen = when (route) {
            Home::class.qualifiedName -> Home
            ChoosePlayer::class.qualifiedName -> ChoosePlayer
            ChooseTournament::class.qualifiedName -> ChooseTournament
            else -> {
                if (route?.startsWith(Gameplay::class.qualifiedName!!) == true) {
                    backStackEntry?.toRoute<Gameplay>() ?: Home
                } else {
                    Home
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
                        onGoToAmericano = {
                            navController.navigate(ChoosePlayer)
                        },
                        onGoToMexicano = {
                            navController.navigate(ChoosePlayer)
                        },
                        onGoBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<ChoosePlayer> {
                    ChoosePlayerScreen(
                        onGoToStart = { playerNames ->
                            navController.navigate(Gameplay(playerNames))
                        },
                        onGoBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable<Gameplay> { backStackEntry ->
                    val gameplay = backStackEntry.toRoute<Gameplay>()
                    GamePlayScreen(
                        playerNames = gameplay.playerNames.split(","),
                        onGoBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}