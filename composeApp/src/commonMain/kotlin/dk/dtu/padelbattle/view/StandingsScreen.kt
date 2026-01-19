package dk.dtu.padelbattle.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.viewmodel.PlayerStanding
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.ui.theme.*

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
    LaunchedEffect(players, revision, pointsPerMatch) {
        viewModel.setPlayers(players.map { it.copy() }, pointsPerMatch)
    }

    val sortedPlayers by viewModel.sortedPlayers.collectAsState()
    val editingPlayer by viewModel.editingPlayer.collectAsState()
    val editingName by viewModel.editingName.collectAsState()

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

    // Warm gradient background matching HomeScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PadelOrange.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = PadelOrange
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(0.5f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Spiller",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "W-L-D",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1.0f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "+Bonus",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Diff",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(0.6f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val leaderTotal = sortedPlayers.firstOrNull()?.displayTotal ?: 0

                items(sortedPlayers.size) { index ->
                    val standing = sortedPlayers[index]
                    PremiumStandingRow(
                        standing = standing,
                        position = index + 1,
                        leaderTotal = leaderTotal,
                        onPlayerClick = { player -> viewModel.startEditingPlayer(player) }
                    )
                }
                
                item {
                    StandingsLegend()
                }
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
private fun PremiumStandingRow(
    standing: PlayerStanding,
    position: Int,
    leaderTotal: Int,
    onPlayerClick: (Player) -> Unit = {}
) {
    val player = standing.player
    val difference = standing.displayTotal - leaderTotal

    val (borderColor, isPodium) = when (position) {
        1 -> GoldPodium to true
        2 -> SilverPodium to true
        3 -> BronzePodium to true
        else -> Color.Transparent to false
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPodium) Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (isPodium) 
            androidx.compose.foundation.BorderStroke(2.dp, borderColor) 
        else 
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlayerClick(player) }
                .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position with medal
            Box(
                modifier = Modifier.weight(0.5f),
                contentAlignment = Alignment.Center
            ) {
                when (position) {
                    1 -> Text("🥇", style = MaterialTheme.typography.titleLarge)
                    2 -> Text("🥈", style = MaterialTheme.typography.titleLarge)
                    3 -> Text("🥉", style = MaterialTheme.typography.titleLarge)
                    else -> Text(
                        text = "$position",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Player Name
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPodium) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.5f),
                maxLines = 1
            )

            // W-L-D
            Text(
                text = "${player.wins}-${player.losses}-${player.draws}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.0f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Bonus
            Text(
                text = if (standing.bonusPoints > 0) "+${standing.bonusPoints}" else "-",
                style = MaterialTheme.typography.bodyMedium,
                color = if (standing.bonusPoints > 0) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontWeight = if (standing.bonusPoints > 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.Center
            )

            // Diff (forskel fra lederen)
            Text(
                text = if (difference == 0) "-" else "$difference",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    difference == 0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    difference < 0 -> MaterialTheme.colorScheme.error
                    else -> SuccessGreen
                },
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )

            // Total Points
            Text(
                text = "${standing.displayTotal}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = PadelOrange,
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StandingsLegend() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = PadelOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Forklaring",
                    style = MaterialTheme.typography.labelLarge,
                    color = PadelOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            LegendItem("W-L-D", "Vundne - Tabte - Uafgjorte")
            LegendItem("+Bonus", "Pointkompensation for færre kampe")
            LegendItem("Diff", "Difference fra førende spiller")
        }
    }
}

@Composable
private fun LegendItem(label: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PlayerNameEditDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("Rediger spillernavn", fontWeight = FontWeight.Bold) 
        },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Navn") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PadelOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = currentName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PadelOrange)
            ) {
                Text("Gem", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuller")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
