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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.TournamentType

@Composable
fun TournamentConfigScreen(
    tournamentType: TournamentType,
    onStartTournament: (name: String, players: List<String>) -> Unit,
    onGoBack: () -> Unit
) {
    var tournamentName by remember { mutableStateOf("") }
    var playerNames by remember { mutableStateOf(listOf<String>()) }
    var currentPlayerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Opret Turnering",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (tournamentType) {
                        TournamentType.AMERICANO -> "Type: Americano"
                        TournamentType.MEXICANO -> "Type: Mexicano"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        OutlinedTextField(
            value = tournamentName,
            onValueChange = { tournamentName = it },
            label = { Text("Turneringsnavn") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tilføj Spillere",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentPlayerName,
                onValueChange = { currentPlayerName = it },
                label = { Text("Spillernavn") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (currentPlayerName.isNotBlank()) {
                        playerNames = playerNames + currentPlayerName.trim()
                        currentPlayerName = ""
                    }
                },
                enabled = currentPlayerName.isNotBlank() && playerNames.size < 16
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tilføj spiller")
            }
        }

        Text(
            text = when {
                playerNames.size < 4 -> "Mindst 4 spillere kræves (${playerNames.size}/4)"
                playerNames.size >= 16 -> "Maksimum 16 spillere nået"
                else -> "${playerNames.size} spillere tilføjet"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (playerNames.size < 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
            ) {
                itemsIndexed(playerNames) { index, playerName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${index + 1}. $playerName",
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = {
                            playerNames = playerNames.toMutableList().apply { removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Fjern spiller")
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (tournamentName.isNotBlank() && playerNames.size >= 4) {
                    onStartTournament(tournamentName, playerNames)
                }
            },
            enabled = tournamentName.isNotBlank() && playerNames.size in 4..16,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Turnering", style = MaterialTheme.typography.titleMedium)
        }
    }
}

