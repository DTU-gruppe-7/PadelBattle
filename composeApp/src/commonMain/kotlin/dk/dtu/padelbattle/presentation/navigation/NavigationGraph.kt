package dk.dtu.padelbattle.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dk.dtu.padelbattle.domain.model.TournamentType
import dk.dtu.padelbattle.presentation.tournament.choose.ChooseTournamentScreen
import dk.dtu.padelbattle.presentation.home.HomeScreen
import dk.dtu.padelbattle.presentation.search.SearchScreen
import dk.dtu.padelbattle.presentation.tournament.config.TournamentConfigScreen
import dk.dtu.padelbattle.presentation.tournament.view.TournamentViewScreen
import dk.dtu.padelbattle.presentation.home.HomeViewModel
import dk.dtu.padelbattle.presentation.tournament.view.MatchEditViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentContentViewModel
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsViewModel
import dk.dtu.padelbattle.presentation.tournament.config.TournamentConfigViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentViewModel
import org.koin.compose.koinInject

// Animation specifikationer
private const val ANIMATION_DURATION_MS = 350

/**
 * Navigation graph for the app.
 * ViewModels injiceres via Koin for at reducere parameter-bloat.
 */
@Composable
fun NavigationGraph(
    navController: NavHostController,
    navigationManager: NavigationManager,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ViewModels injiceres via Koin
    val homeViewModel: HomeViewModel = koinInject()
    val tournamentConfigViewModel: TournamentConfigViewModel = koinInject()
    val tournamentViewModel: TournamentViewModel = koinInject()
    val contentViewModel: TournamentContentViewModel = koinInject()
    val matchEditViewModel: MatchEditViewModel = koinInject()
    val settingsViewModel: SettingsViewModel = koinInject()

    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
        // Globale animations for alle skærme
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(ANIMATION_DURATION_MS))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(ANIMATION_DURATION_MS))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(ANIMATION_DURATION_MS))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(ANIMATION_DURATION_MS))
        }
    ) {
        composable<Home> {
            HomeScreen(
                viewModel = homeViewModel,
                onGoToTournamentScreen = { navigationManager.navigateToChooseTournament() },
                onTournamentClicked = { navigationManager.navigateToTournament(it) },
                onDuplicateTournament = { type, id -> navigationManager.navigateToDuplicate(type, id) },
                onGoToSearchScreen = { navigationManager.navigateToSearch() }
            )
        }

        composable<SearchTournament> {
            SearchScreen(
                viewModel = homeViewModel,
                onTournamentClicked = { navigationManager.navigateToTournament(it) },
                onDuplicateTournament = { type, id -> navigationManager.navigateToDuplicate(type, id) }
            )
        }

        composable<ChooseTournament> {
            ChooseTournamentScreen(
                viewModel = tournamentConfigViewModel,
                onNavigateToPlayers = {
                    val typeName = tournamentConfigViewModel.selectedTournamentType.value?.name ?: "AMERICANO"
                    navigationManager.navigateToTournamentConfig(typeName)
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
                    navigationManager.onTournamentCreated(tournament)
                },
                onGoBack = {
                    navigationManager.navigateBackFromConfig()
                }
            )
        }

        composable<TournamentView> {
            TournamentViewScreen(
                viewModel = tournamentViewModel,
                contentViewModel = contentViewModel,
                matchEditViewModel = matchEditViewModel,
                settingsViewModel = settingsViewModel,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    }
}
