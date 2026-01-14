package dk.dtu.padelbattle.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsTennis
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
import androidx.compose.ui.window.Dialog
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel
import dk.dtu.padelbattle.ui.theme.*

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

    LaunchedEffect(duplicateFromId) {
        if (duplicateFromId != null) {
            viewModel.loadTournamentForDuplication(duplicateFromId)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            // Start Tournament FAB
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.createTournament(tournamentType, onSuccess = { tournament ->
                        onTournamentCreated(tournament)
                    })
                },
                containerColor = if (viewModel.canStartTournament()) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (viewModel.canStartTournament()) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                icon = { Icon(Icons.Default.Check, "Start") },
                text = { Text("Start Turnering", fontWeight = FontWeight.Bold) },
                expanded = viewModel.canStartTournament()
            )
        },
        floatingActionButtonPosition = FabPosition.End
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tournament Type Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (tournamentType) {
                                    TournamentType.AMERICANO -> "Americano"
                                    TournamentType.MEXICANO -> "Mexicano"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Ny turnering",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Tournament Name Input
                OutlinedTextField(
                    value = tournamentName,
                    onValueChange = { viewModel.updateTournamentName(it) },
                    label = { Text("Turneringsnavn") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Settings Row (Courts & Points)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Number of Courts
                    SettingsCard(
                        title = "Baner",
                        value = numberOfCourts.toString(),
                        onClick = { showCourtsDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Points Per Round
                    SettingsCard(
                        title = "Point/Runde",
                        value = pointsPerRound.toString(),
                        onClick = { showPointsDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Player Input Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tilføj Spillere",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
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
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            FilledIconButton(
                                onClick = { viewModel.addPlayer() },
                                enabled = currentPlayerName.isNotBlank() && playerNames.size < Tournament.MAX_PLAYERS,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tilføj spiller")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Player count indicator
                        val countColor = when {
                            playerNames.size < Tournament.MIN_PLAYERS -> MaterialTheme.colorScheme.error
                            playerNames.size >= Tournament.MAX_PLAYERS -> WarningAmber
                            else -> SuccessGreen
                        }
                        Text(
                            text = when {
                                playerNames.size < Tournament.MIN_PLAYERS -> 
                                    "Mindst ${Tournament.MIN_PLAYERS} spillere (${playerNames.size}/${Tournament.MIN_PLAYERS})"
                                playerNames.size >= Tournament.MAX_PLAYERS -> 
                                    "Maks ${Tournament.MAX_PLAYERS} spillere nået"
                                else -> 
                                    "${playerNames.size} spillere tilføjet"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = countColor
                        )
                    }
                }

                // Player List
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (playerNames.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ingen spillere endnu",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(playerNames) { index, playerName ->
                                PlayerListItem(
                                    index = index + 1,
                                    name = playerName,
                                    onRemove = { viewModel.removePlayer(index) }
                                )
                            }
                        }
                    }
                }
                
                // Bottom spacer for FAB
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Dialogs
    if (showCourtsDialog) {
        NumberPickerDialog(
            title = "Antal Baner",
            currentValue = numberOfCourts,
            minValue = 1,
            maxValue = Tournament.MAX_COURTS,
            onValueChange = { viewModel.updateNumberOfCourts(it) },
            onDismiss = { showCourtsDialog = false }
        )
    }

    if (showPointsDialog) {
        PointsPickerDialog(
            currentValue = pointsPerRound,
            onValueChange = { viewModel.updatePointsPerMatch(it) },
            onDismiss = { showPointsDialog = false }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun PlayerListItem(
    index: Int,
    name: String,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index.",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Fjern spiller",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
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
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledIconButton(
                        onClick = { if (tempValue > minValue) tempValue-- },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = tempValue.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .clickable { showScrollPicker = true }
                    )

                    FilledIconButton(
                        onClick = { if (tempValue < maxValue) tempValue++ },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "($minValue - $maxValue)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuller")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onValueChange(tempValue)
                        onDismiss()
                    }) {
                        Text("Gem")
                    }
                }
            }
        }
    }

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

    LaunchedEffect(Unit) {
        listState.scrollToItem((currentValue - minValue).coerceIn(0, maxValue - minValue))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        .height(200.dp),
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
                                    MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                                } else {
                                    MaterialTheme.typography.titleLarge
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
    val currentIndex = availablePoints.indexOf(currentValue).takeIf { it >= 0 } ?: 0

    LaunchedEffect(Unit) {
        listState.scrollToItem(currentIndex)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Point pr. Runde",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
                                    MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                                } else {
                                    MaterialTheme.typography.titleLarge
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
