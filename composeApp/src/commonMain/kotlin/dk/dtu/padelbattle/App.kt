package dk.dtu.padelbattle

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dk.dtu.padelbattle.presentation.theme.PadelBattleTheme
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsDialogs
import dk.dtu.padelbattle.presentation.navigation.BottomNavigationBar
import dk.dtu.padelbattle.presentation.navigation.NavigationGraph
import dk.dtu.padelbattle.presentation.navigation.NavigationManager
import dk.dtu.padelbattle.presentation.navigation.NavigationManagerEffects
import dk.dtu.padelbattle.presentation.navigation.TopBar
import dk.dtu.padelbattle.presentation.navigation.TournamentView
import dk.dtu.padelbattle.presentation.navigation.getCurrentScreen
import dk.dtu.padelbattle.presentation.home.HomeViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentContentViewModel
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsViewModel
import dk.dtu.padelbattle.presentation.tournament.config.TournamentConfigViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentViewModel
import org.koin.compose.koinInject

/**
 * Main App composable.
 * ViewModels injiceres automatisk via Koin DI.
 */
@Composable
fun App() {
    val homeViewModel: HomeViewModel = koinInject()
    val tournamentConfigViewModel: TournamentConfigViewModel = koinInject()
    val tournamentViewModel: TournamentViewModel = koinInject()
    val contentViewModel: TournamentContentViewModel = koinInject()
    val settingsViewModel: SettingsViewModel = koinInject()

    PadelBattleTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentScreen = getCurrentScreen(backStackEntry)

        val navigationManager = remember(navController) {
            NavigationManager(
                navController = navController,
                homeViewModel = homeViewModel,
                tournamentViewModel = tournamentViewModel,
                tournamentConfigViewModel = tournamentConfigViewModel,
                contentViewModel = contentViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        // Alle side-effects håndteres her
        NavigationManagerEffects(navigationManager, currentScreen)

        val selectedTab by navigationManager.selectedTab.collectAsState()
        val settingsMenuItems by settingsViewModel.menuItems.collectAsState()
        val currentTournament by tournamentViewModel.tournament.collectAsState()

        SettingsDialogs(settingsViewModel)

        // Brug turneringens navn direkte som titel når vi er på TournamentView
        val titleOverride = if (currentScreen is TournamentView) currentTournament?.name else null

        Scaffold(
            topBar = {
                TopBar(
                    currentScreen = currentScreen,
                    canNavigateBack = navigationManager.canNavigateBack(),
                    navigateUp = { navigationManager.navigateBack() },
                    settingsMenuItems = settingsMenuItems,
                    settingsViewModel = settingsViewModel,
                    titleOverride = titleOverride
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentScreen is TournamentView) {
                    BottomNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { navigationManager.selectTab(it) }
                    )
                }
            }
        ) { contentPadding ->
            NavigationGraph(
                navController = navController,
                navigationManager = navigationManager,
                selectedTab = selectedTab,
                onTabSelected = { navigationManager.selectTab(it) },
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}
