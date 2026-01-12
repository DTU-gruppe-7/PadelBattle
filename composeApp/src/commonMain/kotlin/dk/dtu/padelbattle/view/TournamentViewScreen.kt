package dk.dtu.padelbattle.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel
import dk.dtu.padelbattle.viewmodel.SettingsViewModel

@Composable
fun TournamentViewScreen(
    viewModel: TournamentViewModel,
    standingsViewModel: StandingsViewModel,
    matchEditViewModel: MatchEditViewModel,
    matchListViewModel: MatchListViewModel,
    settingsViewModel: SettingsViewModel,
    selectedTab: Int,
    onGoBack: () -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    val revision by viewModel.revision.collectAsState()

    key(revision) {
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
                            matchListViewModel = matchListViewModel,
                            onMatchUpdated = {
                                viewModel.notifyTournamentUpdated()
                                // Opdater standings med de nyeste spillerdata - lav en ny liste med kopierede objekter for at trigger StateFlow
                                standingsViewModel.setPlayers(currentTournament.players.map { it.copy() })
                            }
                        )
                    } else {
                        // Vis loading eller tom tilstand
                        Text("Indlæser turnering...")
                    }
                }
                1 -> StandingsScreen(
                    players = tournament?.players ?: emptyList(),
                    viewModel = standingsViewModel,
                    revision = revision
                )
            }
        }
    }

    // Settings dialogs
    val showPointsDialog by settingsViewModel.showPointsDialog.collectAsState()
    val showWarningDialog by settingsViewModel.showWarningDialog.collectAsState()
    val pendingPointsChange by settingsViewModel.pendingPointsChange.collectAsState()

    if (showPointsDialog) {
        PointsPickerDialog(
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
