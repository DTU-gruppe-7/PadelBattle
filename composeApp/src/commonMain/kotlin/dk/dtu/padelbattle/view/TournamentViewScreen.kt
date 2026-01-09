package dk.dtu.padelbattle.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel

@Composable
fun TournamentViewScreen(
    viewModel: TournamentViewModel,
    standingsViewModel: StandingsViewModel,
    matchEditViewModel: MatchEditViewModel,
    matchListViewModel: MatchListViewModel,
    onGoBack: () -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    val revision by viewModel.revision.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    key(revision) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Kampe",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Stilling",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

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
}
