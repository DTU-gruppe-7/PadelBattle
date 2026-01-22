package dk.dtu.padelbattle.presentation.tournament.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.domain.model.TournamentType
import dk.dtu.padelbattle.presentation.theme.*

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

    // Hent minimum spillere og canStart fra ViewModel (reaktive StateFlows)
    val minPlayers by viewModel.minimumPlayers.collectAsState()
    val canStartTournament by viewModel.canStartTournament.collectAsState()

    var showCourtsDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val playerInputBringIntoView = remember { BringIntoViewRequester() }
    val playerListBringIntoView = remember { BringIntoViewRequester() }
    val startButtonBringIntoView = remember { BringIntoViewRequester() }
    val playerListState = rememberLazyListState()

    LaunchedEffect(duplicateFromId) {
        if (duplicateFromId != null) {
            viewModel.loadTournamentForDuplication(duplicateFromId)
        }
    }

    // Scroll til bunden af spillerlisten når en ny spiller tilføjes
    LaunchedEffect(playerNames.size) {
        if (playerNames.isNotEmpty()) {
            delay(50)
            playerListState.scrollToItem(playerNames.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PadelOrange.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tournament Type Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon with gradient
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PadelOrange, DeepAmber)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = when (tournamentType) {
                                TournamentType.AMERICANO -> "Americano"
                                TournamentType.MEXICANO -> "Mexicano"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ny turnering",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PadelOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Settings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumSettingsCard(
                    title = "Baner",
                    value = numberOfCourts.toString(),
                    onClick = { showCourtsDialog = true },
                    modifier = Modifier.weight(1f)
                )

                PremiumSettingsCard(
                    title = "Point/Runde",
                    value = pointsPerRound.toString(),
                    onClick = { showPointsDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Player Input Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(playerInputBringIntoView)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Tilføj Spillere",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PadelOrange
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = currentPlayerName,
                            onValueChange = { viewModel.updateCurrentPlayerName(it) },
                            label = { Text("Spillernavn") },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusEvent { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            delay(100)
                                            //playerInputBringIntoView.bringIntoView()
                                            //playerListBringIntoView.bringIntoView()
                                            startButtonBringIntoView.bringIntoView()
                                        }
                                    }
                                },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PadelOrange,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        FilledIconButton(
                            onClick = { viewModel.addPlayer() },
                            enabled = currentPlayerName.isNotBlank() && playerNames.size < Tournament.MAX_PLAYERS,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = PadelOrange,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tilføj", modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Player count indicator - dynamisk minimum baseret på antal baner
                    val (countColor, countText) = when {
                        playerNames.size < minPlayers ->
                            MaterialTheme.colorScheme.error to "Mindst $minPlayers spillere (${playerNames.size}/$minPlayers)"
                        playerNames.size >= Tournament.MAX_PLAYERS ->
                            WarningAmber to "Maks ${Tournament.MAX_PLAYERS} spillere nået"
                        else -> 
                            SuccessGreen to "${playerNames.size} spillere tilføjet"
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelMedium,
                        color = countColor
                    )
                }
            }

            // Player List
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .bringIntoViewRequester(playerListBringIntoView)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                if (playerNames.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = PadelOrange.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ingen spillere endnu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = playerListState,
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(playerNames) { index, playerName ->
                            PremiumPlayerListItem(
                                index = index + 1,
                                name = playerName,
                                onRemove = { viewModel.removePlayer(index) }
                            )
                        }
                    }
                }
            }

            // Start Button
            Button(
                onClick = {
                    viewModel.createTournament(tournamentType, onSuccess = { tournament ->
                        onTournamentCreated(tournament)
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bringIntoViewRequester(startButtonBringIntoView),
                enabled = canStartTournament,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PadelOrange,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Start Turnering",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Dialogs
    if (showCourtsDialog) {
        PremiumNumberPickerDialog(
            title = "Antal Baner",
            currentValue = numberOfCourts,
            minValue = 1,
            maxValue = Tournament.MAX_COURTS,
            onValueChange = { viewModel.updateNumberOfCourts(it) },
            onDismiss = { showCourtsDialog = false }
        )
    }

    if (showPointsDialog) {
        PremiumPointsPickerDialog(
            currentValue = pointsPerRound,
            onValueChange = { viewModel.updatePointsPerMatch(it) },
            onDismiss = { showPointsDialog = false }
        )
    }
}

@Composable
private fun PremiumSettingsCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PadelOrange
            )
        }
    }
}

@Composable
private fun PremiumPlayerListItem(
    index: Int,
    name: String,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PadelOrange,
                    modifier = Modifier.width(32.dp)
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
                    contentDescription = "Fjern",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumNumberPickerDialog(
    title: String,
    currentValue: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempValue by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledIconButton(
                        onClick = { if (tempValue > minValue) tempValue-- },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = tempValue.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = PadelOrange,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    FilledIconButton(
                        onClick = { if (tempValue < maxValue) tempValue++ },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    TextButton(onClick = onDismiss) { Text("Annuller") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onValueChange(tempValue)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PadelOrange)
                    ) {
                        Text("Gem", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPointsPickerDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val availablePoints = listOf(16, 21, 24, 31, 32)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
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

                Spacer(modifier = Modifier.height(20.dp))

                availablePoints.forEach { value ->
                    Surface(
                        onClick = {
                            onValueChange(value)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (value == currentValue) 
                            PadelOrange.copy(alpha = 0.15f) 
                        else 
                            Color.Transparent,
                        border = if (value == currentValue) 
                            androidx.compose.foundation.BorderStroke(2.dp, PadelOrange) 
                        else 
                            null
                    ) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = if (value == currentValue) FontWeight.Bold else FontWeight.Medium,
                            color = if (value == currentValue) PadelOrange else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(vertical = 14.dp)
                                .fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Luk")
                }
            }
        }
    }
}
