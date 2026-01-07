package dk.dtu.padelbattle.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.viewmodel.StandingsViewModel

@Composable
fun StandingsScreen(
    players: List<Player>,
    viewModel: StandingsViewModel,
    revision: Int = 0
) {
    // Opdater viewModel med spillere når de ændres - lav en ny liste med kopierede objekter for at trigger StateFlow
    // Brug revision som key for at sikre opdatering når kampe opdateres
    LaunchedEffect(players, revision) {
        viewModel.setPlayers(players.map { it.copy() })
    }

    // Hent sorterede spillere fra viewModel StateFlow - opdateres automatisk
    val sortedPlayers by viewModel.sortedPlayers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Plac.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = "Spiller",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Point",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = "Kampe",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedPlayers.size) { index ->
                val player = sortedPlayers[index]
                StandingRow(player = player, position = index + 1)
            }
        }
    }
}

@Composable
private fun StandingRow(
    player: Player,
    position: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (position) {
                1 -> MaterialTheme.colorScheme.tertiaryContainer
                2 -> MaterialTheme.colorScheme.surfaceVariant
                3 -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (position <= 3) {
                    val emoji = when (position) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> ""
                    }
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (position <= 3) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(2f)
            )

            Text(
                text = player.totalPoints.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(0.8f)
            )

            Text(
                text = player.gamesPlayed.toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}


