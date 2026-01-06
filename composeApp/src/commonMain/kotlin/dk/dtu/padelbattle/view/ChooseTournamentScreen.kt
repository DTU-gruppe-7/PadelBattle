package dk.dtu.padelbattle.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.viewmodel.TournamentType
import dk.dtu.padelbattle.viewmodel.TournamentViewModel

@Composable
fun ChooseTournamentScreen(
    viewModel: TournamentViewModel,
    onNavigateToPlayers: () -> Unit
) {
    val selectedType by viewModel.selectedTournamentType.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Vælg turneringstype",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.selectTournamentType(TournamentType.AMERICANO)
                onNavigateToPlayers()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Americano")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.selectTournamentType(TournamentType.MEXICANO)
                onNavigateToPlayers()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Mexicano")
        }
    }
}