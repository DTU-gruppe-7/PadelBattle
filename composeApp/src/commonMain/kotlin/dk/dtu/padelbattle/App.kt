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
import dk.dtu.padelbattle.ui.theme.PadelBattleTheme
import dk.dtu.padelbattle.view.SettingsDialogs
import dk.dtu.padelbattle.view.navigation.BottomNavigationBar
import dk.dtu.padelbattle.view.navigation.NavigationGraph
import dk.dtu.padelbattle.view.navigation.NavigationManager
import dk.dtu.padelbattle.view.navigation.NavigationManagerEffects
import dk.dtu.padelbattle.view.navigation.TopBar
import dk.dtu.padelbattle.view.navigation.TournamentView
import dk.dtu.padelbattle.view.navigation.getCurrentScreen
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.SettingsViewModel
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel
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
    val matchListViewModel: MatchListViewModel = koinInject()
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
                matchListViewModel = matchListViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        // Alle side-effects håndteres her
        NavigationManagerEffects(navigationManager, currentScreen)

        val selectedTab by navigationManager.selectedTab.collectAsState()
        val settingsMenuItems by settingsViewModel.menuItems.collectAsState()

        SettingsDialogs(settingsViewModel)

        Scaffold(
            topBar = {
                TopBar(
                    currentScreen = currentScreen,
                    canNavigateBack = navigationManager.canNavigateBack(),
                    navigateUp = { navigationManager.navigateBack() },
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
