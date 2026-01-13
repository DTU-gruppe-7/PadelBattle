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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.viewmodel.PlayerStanding
import dk.dtu.padelbattle.viewmodel.StandingsViewModel

@Composable
fun StandingsScreen(
    players: List<Player>,
    viewModel: StandingsViewModel,
    pointsPerMatch: Int = 16,
    revision: Int = 0,
    isCompleted: Boolean = false,
    isLoading: Boolean = false,
    onPlayerNameChanged: (Player, String) -> Unit = { _, _ -> },  // Callback til at gemme navneændring
    onContinueTournament: () -> Unit = {}  // Callback til at fortsætte turneringen
) {
    // Opdater viewModel med spillere når de ændres - lav en ny liste med kopierede objekter for at trigger StateFlow
    // Brug revision som key for at sikre opdatering når kampe opdateres
    LaunchedEffect(players, revision, pointsPerMatch) {
        viewModel.setPlayers(players.map { it.copy() }, pointsPerMatch)
    }

    // Hent sorterede spillere fra viewModel StateFlow - opdateres automatisk
    val sortedPlayers by viewModel.sortedPlayers.collectAsState()
    
    // State for redigering af spillernavn
    val editingPlayer by viewModel.editingPlayer.collectAsState()
    val editingName by viewModel.editingName.collectAsState()

    // Vis edit dialog hvis en spiller er ved at blive redigeret
    if (editingPlayer != null) {
        PlayerNameEditDialog(
            currentName = editingName,
            onNameChange = { viewModel.updateEditingName(it) },
            onSave = { 
                viewModel.savePlayerName { player, newName ->
                    onPlayerNameChanged(player, newName)
                }
            },
            onDismiss = { viewModel.cancelEditing() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
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
                    text = "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.5f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Spiller",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "W-L-D",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.0f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "+Bonus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Diff",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Spillerliste
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val leaderTotal = sortedPlayers.firstOrNull()?.displayTotal ?: 0

            items(sortedPlayers.size) { index ->
                val standing = sortedPlayers[index]
                StandingRow(
                    standing = standing,
                    position = index + 1,
                    leaderTotal = leaderTotal,
                    onPlayerClick = { player -> viewModel.startEditingPlayer(player) }
                )
            }
        }

        // Fortsæt turnering knap - vises kun når turneringen er afsluttet
        if (isCompleted) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onContinueTournament,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (isLoading) "Genererer ny runde..." else "Fortsæt turnering",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Forklaring af kolonner
        StandingsLegend()
    }
}

@Composable
private fun StandingsLegend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        LegendItem(
            label = "W-L-D",
            description = "Vundne - Tabte - Uafgjorte kampe"
        )
        LegendItem(
            label = "+Bonus",
            description = "Midlertidige point for manglende kampe"
        )
        LegendItem(
            label = "Diff",
            description = "Afstand til førstepladsen"
        )
    }
}

@Composable
private fun LegendItem(label: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StandingRow(
    standing: PlayerStanding,
    position: Int,
    leaderTotal: Int,
    onPlayerClick: (Player) -> Unit = {}
) {
    val player = standing.player
    val difference = standing.displayTotal - leaderTotal

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (position) {
                1 -> Color(0xFFFFF8DC)  // Lys cremegul (guld-agtig)
                2 -> Color(0xFFE8E8E8)  // Lys grå (sølv-agtig)
                3 -> Color(0xFFFFE4C4)  // Lys beige (bronze-agtig)
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
            // Placering
            Column(
                modifier = Modifier.weight(0.5f),
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

            // Navn - klikbart for at redigere
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (position <= 3) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1.5f)
                    .clickable { onPlayerClick(player) }
            )

            // W-L-D (Wins-Losses-Draws)
            Text(
                text = "${player.wins}-${player.losses}-${player.draws}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.0f),
                textAlign = TextAlign.Center
            )

            // Bonus points (vises kun hvis der er bonus)
            Text(
                text = if (standing.bonusPoints > 0) "+${standing.bonusPoints}" else "-",
                style = MaterialTheme.typography.bodyMedium,
                color = if (standing.bonusPoints > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (standing.bonusPoints > 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.Center
            )

            // Difference fra førstepladsen
            Text(
                text = if (difference == 0) "-" else "$difference",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    difference == 0 -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )

            // Total points (inkl. bonus)
            Text(
                text = standing.displayTotal.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dialog til redigering af spillernavn.
 */
@Composable
fun PlayerNameEditDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rediger spillernavn") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Navn") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = currentName.isNotBlank()
            ) {
                Text("Gem")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuller")
            }
        }
    )
}
