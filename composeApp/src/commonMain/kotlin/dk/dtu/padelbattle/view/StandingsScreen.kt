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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onPlayerNameChanged: (Player, String) -> Unit = { _, _ -> }
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

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Reduceret top padding
            ) {
                // Header Table Row
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Spiller",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = "W-L-D",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "+Bonus",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Diff",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(0.6f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
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
                    
                    item {
                         StandingsLegend()
                    }
                }
            }
        }
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

    val (cardColor, contentColor, borderColor) = when (position) {
        1 -> Triple(
            MaterialTheme.colorScheme.surface, 
            MaterialTheme.colorScheme.onSurface,
            GoldPodium
        )
        2 -> Triple(
            MaterialTheme.colorScheme.surface, 
            MaterialTheme.colorScheme.onSurface,
            SilverPodium
        )
        3 -> Triple(
            MaterialTheme.colorScheme.surface, 
            MaterialTheme.colorScheme.onSurface,
            BronzePodium
        )
        else -> Triple(
            MaterialTheme.colorScheme.surface, 
            MaterialTheme.colorScheme.onSurface,
            Color.Transparent
        )
    }

    val elevation = if (position <= 3) 2.dp else 1.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(elevation),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = MaterialTheme.shapes.extraSmall, // Smaller shape for table look
        border = if (position <= 3) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlayerClick(player) }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position (0.5f)
            Box(
                modifier = Modifier.weight(0.5f),
                contentAlignment = Alignment.Center
            ) {
                if (position <= 3) {
                     val emoji = when (position) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> ""
                    }
                    Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Spiller (1.5f)
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (position <= 3) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1.5f),
                maxLines = 1
            )

            // W-L-D (1.0f)
            Text(
                text = "${player.wins}-${player.losses}-${player.draws}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.0f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            // +Bonus (0.8f)
            Text(
                text = if (standing.bonusPoints > 0) "+${standing.bonusPoints}" else "-",
                style = MaterialTheme.typography.bodyMedium,
                color = if (standing.bonusPoints > 0) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = if (standing.bonusPoints > 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.Center
            )

            // Diff (0.6f)
            Text(
                text = if (difference == 0) "-" else "$difference",
                style = MaterialTheme.typography.bodyMedium,
                color = if (difference == 0) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )

            // Total (0.7f)
            Text(
                text = "${standing.displayTotal}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StandingsLegend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Forklaring", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        
        LegendItem("W-L-D", "Vundne - Tabte - Uafgjorte")
        LegendItem("+Bonus", "Pointkompensation for færre kampe")
        LegendItem("Diff", "Pointforskel til førstepladsen")
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
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
        title = { Text("Rediger spillernavn") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Navn") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        confirmButton = {
            Button(
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
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
