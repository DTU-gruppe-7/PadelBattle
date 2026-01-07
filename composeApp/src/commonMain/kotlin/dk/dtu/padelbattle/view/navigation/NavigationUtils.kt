package dk.dtu.padelbattle.view.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute

/**
 * Utility function to determine the current screen from a navigation back stack entry.
 */
fun getCurrentScreen(backStackEntry: NavBackStackEntry?): Screen {
    val route = backStackEntry?.destination?.route ?: return Home

    return when (route) {
        Home::class.qualifiedName -> Home
        ChoosePlayer::class.qualifiedName -> ChoosePlayer
        ChooseTournament::class.qualifiedName -> ChooseTournament
        else -> {
            when {
                route.startsWith(Gameplay::class.qualifiedName ?: "") ->
                    backStackEntry.toRoute<Gameplay>()
                route.startsWith(TournamentConfig::class.qualifiedName ?: "") ->
                    backStackEntry.toRoute<TournamentConfig>()
                route.startsWith(TournamentView::class.qualifiedName ?: "") ->
                    backStackEntry.toRoute<TournamentView>()
                else -> Home
            }
        }
    }
}

