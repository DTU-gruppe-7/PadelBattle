package dk.dtu.padelbattle.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGoToTournamentScreen: () -> Unit,
    onTournamentClicked: (String) -> Unit,
    onDuplicateTournament: ((String, String) -> Unit)? = null // (tournamentType, tournamentId) -> Unit
) {
    val tournaments by viewModel.tournaments.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }

    val activeTournaments = tournaments.filter { !it.isCompleted }
    val completedTournaments = tournaments.filter { it.isCompleted }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onGoToTournamentScreen,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Opret Turnering")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Dine Turneringer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("I gang (${activeTournaments.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Afsluttet (${completedTournaments.size})") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentTournaments = if (selectedTabIndex == 0) activeTournaments else completedTournaments

            if (currentTournaments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTabIndex == 0) Icons.Default.SportsTennis else Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "Ingen aktive turneringer" else "Ingen afsluttede turneringer",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedTabIndex == 0) {
                            Text(
                                text = "Tryk på + for at starte en ny kamp!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(currentTournaments.size) { index ->
                        TournamentItemCard(
                            tournament = currentTournaments[index],
                            onClick = { onTournamentClicked(currentTournaments[index].id) },
                            onDuplicate = { tournament ->
                                val (tournamentType, tournamentId) = viewModel.getDuplicationNavigationData(tournament)
                                onDuplicateTournament?.invoke(tournamentType, tournamentId)
                            },
                            onDelete = { tournament ->
                                // TODO: Implementer sletning
                                viewModel.deleteTournament(tournament)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentItemCard(
    tournament: Tournament,
    onClick: () -> Unit,
    onDuplicate: ((Tournament) -> Unit)? = null, // TODO: Implementer duplikerings-funktionalitet
    onDelete: ((Tournament) -> Unit)? = null // TODO: Implementer sletnings-funktionalitet
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe til højre -> Dupliker turnering
                    onDuplicate?.invoke(tournament)
                    false // Return false to prevent dismissal and reset state
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe til venstre -> Slet turnering
                    onDelete?.invoke(tournament)
                    false // Return false to prevent dismissal and reset state
                }
                else -> false
            }
        },
        positionalThreshold = { it * 0.25f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeBackground(dismissState)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (tournament.isCompleted) Icons.Default.EmojiEvents else Icons.Default.SportsTennis,
                        contentDescription = null,
                        tint = if (tournament.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = tournament.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tournament.type.name} • ${formatDate(tournament.dateCreated)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (tournament.isCompleted) {
                            Text(
                                text = "Afsluttet",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "I gang",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Gå til turnering",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {

    val offset = try {
        dismissState.requireOffset()
    } catch (e: Exception) {
        0f
    }

    val direction = when {
        offset > 0f -> SwipeToDismissBoxValue.StartToEnd
        offset < 0f -> SwipeToDismissBoxValue.EndToStart
        else -> SwipeToDismissBoxValue.Settled
    }

    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.ContentCopy
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        SwipeToDismissBoxValue.Settled -> null
    }

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }
    
    val alpha = (kotlin.math.abs(offset) / 100f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                    else -> Color.Transparent
                },
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

fun formatDate(timestamp: Long): String {
    return "ID: ${timestamp.toString().takeLast(4)}"
}
