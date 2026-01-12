package dk.dtu.padelbattle.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel

@Composable
fun TournamentConfigScreen(
    tournamentType: TournamentType,
    viewModel: TournamentConfigViewModel,
    onTournamentCreated: (Tournament) -> Unit,
    onGoBack: () -> Unit,
    duplicateFromId: String? = null
) {
    val tournamentName by viewModel.tournamentName.collectAsState()
    val playerNames by viewModel.playerNames.collectAsState()
    val currentPlayerName by viewModel.currentPlayerName.collectAsState()
    val numberOfCourts by viewModel.numberOfCourts.collectAsState()
    val pointsPerRound by viewModel.pointsPerRound.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCourtsDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }

    // Load tournament data for duplication if duplicateFromId is provided
    // Only runs once when duplicateFromId changes (including initial composition)
    LaunchedEffect(duplicateFromId) {
        if (duplicateFromId != null) {
            viewModel.loadTournamentForDuplication(duplicateFromId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    OutlinedTextField(
                        value = tournamentName,
                        onValueChange = { viewModel.updateTournamentName(it) },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (tournamentType) {
                            TournamentType.AMERICANO -> "Type: Americano"
                            TournamentType.MEXICANO -> "Type: Mexicano"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Courts and Points buttons side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Number of Courts Button
                Button(
                    onClick = { showCourtsDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Antal Baner",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = numberOfCourts.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Points Per Round Button
                Button(
                    onClick = { showPointsDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Point pr. Runde",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pointsPerRound.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentPlayerName,
                    onValueChange = { viewModel.updateCurrentPlayerName(it) },
                    label = { Text("Spillernavn") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = { viewModel.addPlayer() },
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
                color = MaterialTheme.colorScheme.onSurface
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
                            IconButton(onClick = { viewModel.removePlayer(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Fjern spiller")
                            }
                        }
                    }
                }
            }
        }

        // Floating checkmark button in top-right corner
        FloatingActionButton(
            onClick = {
                viewModel.createTournament(tournamentType, onSuccess = { tournament ->
                    onTournamentCreated(tournament)
                })
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = if (viewModel.canStartTournament()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Start Turnering",
                tint = if (viewModel.canStartTournament()) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Courts Dialog
        if (showCourtsDialog) {
            NumberPickerDialog(
                title = "Antal Baner",
                currentValue = numberOfCourts,
                minValue = 1,
                maxValue = 4,
                onValueChange = { viewModel.updateNumberOfCourts(it) },
                onDismiss = { showCourtsDialog = false }
            )
        }

        // Points Dialog
        if (showPointsDialog) {
            PointsPickerDialog(
                currentValue = pointsPerRound,
                onValueChange = { viewModel.updatePointsPerMatch(it) },
                onDismiss = { showPointsDialog = false }
            )
        }
    }
}

@Composable
private fun NumberPickerDialog(
    title: String,
    currentValue: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempValue by remember { mutableStateOf(currentValue) }
    var showScrollPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Value picker with +/- buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            if (tempValue > minValue) {
                                tempValue--
                            }
                        }
                    ) {
                        Text("-", style = MaterialTheme.typography.displaySmall)
                    }

                    Text(
                        text = tempValue.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .clickable { showScrollPicker = true }
                    )

                    IconButton(
                        onClick = {
                            if (tempValue < maxValue) {
                                tempValue++
                            }
                        }
                    ) {
                        Text("+", style = MaterialTheme.typography.displaySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "($minValue - $maxValue)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuller")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onValueChange(tempValue)
                            onDismiss()
                        }
                    ) {
                        Text("Gem", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Scroll wheel picker as separate popup
    if (showScrollPicker) {
        ScrollWheelPickerPopup(
            currentValue = tempValue,
            minValue = minValue,
            maxValue = maxValue,
            onValueSelected = {
                tempValue = it
                showScrollPicker = false
            },
            onDismiss = { showScrollPicker = false }
        )
    }
}

@Composable
private fun ScrollWheelPickerPopup(
    currentValue: Int,
    minValue: Int,
    maxValue: Int,
    onValueSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to current value when popup opens
    LaunchedEffect(Unit) {
        listState.scrollToItem((currentValue - minValue).coerceIn(0, maxValue - minValue))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Vælg Antal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(maxValue - minValue + 1) { index ->
                            val value = minValue + index
                            Text(
                                text = value.toString(),
                                style = if (value == currentValue) {
                                    MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    MaterialTheme.typography.headlineLarge
                                },
                                color = if (value == currentValue) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .clickable { onValueSelected(value) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Luk")
                }
            }
        }
    }
}

@Composable
fun PointsPickerDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val availablePoints = listOf(16, 21, 24, 31, 32)
    val listState = rememberLazyListState()

    // Find the index of the current value, default to first option if not in list
    val currentIndex = availablePoints.indexOf(currentValue).takeIf { it >= 0 } ?: 0

    // Scroll to current value when dialog opens
    LaunchedEffect(Unit) {
        listState.scrollToItem(currentIndex)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Point pr. Runde",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(availablePoints.size) { index ->
                            val value = availablePoints[index]
                            Text(
                                text = value.toString(),
                                style = if (value == currentValue) {
                                    MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    MaterialTheme.typography.headlineLarge
                                },
                                color = if (value == currentValue) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        onValueChange(value)
                                        onDismiss()
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Luk")
                }
            }
        }
    }
}

