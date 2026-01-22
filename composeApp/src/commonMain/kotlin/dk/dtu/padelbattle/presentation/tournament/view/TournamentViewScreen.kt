package dk.dtu.padelbattle.presentation.tournament.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsViewModel
import dk.dtu.padelbattle.presentation.tournament.settings.PointsChangeWarningDialog
import dk.dtu.padelbattle.presentation.tournament.config.PremiumPointsPickerDialog

@Composable
fun TournamentViewScreen(
    viewModel: TournamentViewModel,
    contentViewModel: TournamentContentViewModel,
    matchEditViewModel: MatchEditViewModel,
    settingsViewModel: SettingsViewModel,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    val revision by viewModel.revision.collectAsState()

    // Indlæs turneringen og sæt til første uafspillede runde ved første gang
    LaunchedEffect(tournament?.id) {
        tournament?.let {
            contentViewModel.loadTournament(it.matches)
        }
    }

    // Opdater kun kampene (uden at skifte runde) når turneringen genindlæses
    LaunchedEffect(revision) {
        tournament?.let {
            contentViewModel.updateMatches(it.matches)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        when (selectedTab) {
            0 -> {
                val currentTournament = tournament
                if (currentTournament != null) {
                    MatchListScreen(
                        matches = currentTournament.matches,
                        currentTournament = currentTournament,
                        matchEditViewModel = matchEditViewModel,
                        contentViewModel = contentViewModel,
                        onMatchUpdated = {
                            // Genindlæs turneringen fra databasen for at få nye kampe
                            viewModel.reloadFromDatabase()
                            // Opdater standings med de nyeste spillerdata
                            contentViewModel.setPlayers(currentTournament.players.map { it.copy() })
                        },
                        onTournamentCompleted = {
                            // Marker turneringen som completed (via ViewModel)
                            viewModel.updateTournament(currentTournament.copy(isCompleted = true))
                            viewModel.notifyTournamentUpdated()
                            // Skift til standings tab (genbruger logik fra bottom bar)
                            onTabSelected(1)
                        }
                    )
                } else {
                    // Vis loading eller tom tilstand
                    Text("Indlæser turnering...")
                }
            }
            1 -> {
                val isLoading by viewModel.isLoading.collectAsState()
                StandingsScreen(
                    players = tournament?.players ?: emptyList(),
                    contentViewModel = contentViewModel,
                    pointsPerMatch = tournament?.pointsPerMatch ?: 16,
                    revision = revision,
                    isCompleted = tournament?.isCompleted ?: false,
                    isLoading = isLoading,
                    onPlayerNameChanged = { player, newName ->
                        viewModel.updatePlayerName(player, newName)
                    },
                    onContinueTournament = {
                        viewModel.continueTournament {
                            // Genindlæs turneringen fra databasen for at få de nye kampe
                            viewModel.reloadFromDatabase { reloadedTournament ->
                                reloadedTournament?.let {
                                    // Opdater matches og naviger til første nye runde
                                    contentViewModel.loadTournament(it.matches)
                                }
                                // Naviger til kampe-tab
                                onTabSelected(0)
                            }
                        }
                    }
                )
            }
        }
    }

    // Settings dialogs
    val showPointsDialog by settingsViewModel.showPointsDialog.collectAsState()
    val showWarningDialog by settingsViewModel.showWarningDialog.collectAsState()
    val pendingPointsChange by settingsViewModel.pendingPointsChange.collectAsState()

    if (showPointsDialog) {
        PremiumPointsPickerDialog(
            currentValue = tournament?.pointsPerMatch ?: 16,
            onValueChange = { settingsViewModel.onPointsSelected(it) },
            onDismiss = { settingsViewModel.dismissPointsDialog() }
        )
    }

    if (showWarningDialog && pendingPointsChange != null) {
        PointsChangeWarningDialog(
            newPoints = pendingPointsChange!!,
            onConfirm = { settingsViewModel.confirmPointsChange() },
            onCancel = { settingsViewModel.cancelPointsChange() }
        )
    }
}
