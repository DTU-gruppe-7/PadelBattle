package dk.dtu.padelbattle.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.utils.formatDate
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import dk.dtu.padelbattle.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGoToTournamentScreen: () -> Unit,
    onTournamentClicked: (String) -> Unit,
    onDuplicateTournament: ((String, String) -> Unit)? = null, // (tournamentType, tournamentId) -> Unit
    onGoToSearchScreen: () -> Unit = {}
) {
    val tournaments by viewModel.tournaments.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val showDeleteConfirmation by viewModel.deleteConfirmation.showDeleteConfirmation.collectAsState()

    val activeTournaments = tournaments.filter { !it.isCompleted }
    val completedTournaments = tournaments.filter { it.isCompleted }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.deleteConfirmation.dismiss() },
            title = { Text("Slet turnering", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Er du sikker på, at du vil slette denne turnering? Denne handling kan ikke fortrydes.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteConfirmation.confirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Slet")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.deleteConfirmation.dismiss() }) {
                    Text("Annuller")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Scaffold(
        containerColor = Color.Transparent, // Vi bruger Box gradient nedenfor
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(16.dp)
            ) {
                // Søgeknap
                FloatingActionButton(
                    onClick = onGoToSearchScreen,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Søg turneringer")
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Opret turnering knap (Stor og tydelig)
                ExtendedFloatingActionButton(
                    onClick = onGoToTournamentScreen,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    icon = { Icon(Icons.Default.Add, "Opret") },
                    text = { Text("Ny Turnering", fontWeight = FontWeight.Bold) }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        // Baggrunds-box med gradient
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
                    .padding(horizontal = 16.dp) // Lidt tættere margin
            ) {
                // Header Sektion
                Column(modifier = Modifier.padding(vertical = 8.dp)) { // Mindre vertical padding
                    Text(
                        text = "Dine Turneringer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Administrer dine padel kampe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Custom Tabs
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp) // Mindre bund padding
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        indicator = {},
                        divider = {},
                        containerColor = Color.Transparent,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        val tabs = listOf(
                            "I gang (${activeTournaments.size})" to 0,
                            "Afsluttet (${completedTournaments.size})" to 1
                        )
                        
                        tabs.forEach { (title, index) ->
                            val selected = selectedTabIndex == index
                            val containerColor by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                            )
                            val contentColor by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val shadowElevation by animateFloatAsState(if (selected) 2f else 0f)

                            Tab(
                                selected = selected,
                                onClick = { selectedTabIndex = index },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(containerColor)
                                    .graphicsLayer {
                                        this.shadowElevation = shadowElevation
                                    }
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }

                // Liste indhold
                val currentTournaments = if (selectedTabIndex == 0) activeTournaments else completedTournaments

                if (currentTournaments.isEmpty()) {
                    EmptyStateView(selectedTabIndex)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp) // Plads til FABs
                    ) {
                        items(currentTournaments.size) { index ->
                            TournamentItemCard(
                                tournament = currentTournaments[index],
                                onClick = { onTournamentClicked(currentTournaments[index].id) },
                                onDuplicate = { tournament ->
                                    onDuplicateTournament?.invoke(tournament.type.name, tournament.id)
                                },
                                onDelete = { tournament -> viewModel.showDeleteConfirmationDialog(tournament) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(selectedTabIndex: Int) {
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (selectedTabIndex == 0) Icons.Default.SportsTennis else Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (selectedTabIndex == 0) "Ingen aktive turneringer" else "Ingen afsluttede turneringer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (selectedTabIndex == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Opret en ny turnering nedenfor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentItemCard(
    tournament: Tournament,
    onClick: () -> Unit,
    onDuplicate: ((Tournament) -> Unit),
    onDelete: ((Tournament) -> Unit)? = null
) {
    var hasDuplicated by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!hasDuplicated) {
                        hasDuplicated = true
                        onDuplicate.invoke(tournament)
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete?.invoke(tournament)
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { it * 0.25f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled) {
            hasDuplicated = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState) },
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Bruger border i stedet for elevation for renere look
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Ikon Container
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (tournament.isCompleted) 
                                    MaterialTheme.colorScheme.secondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tournament.isCompleted) Icons.Default.EmojiEvents else Icons.Default.SportsTennis,
                            contentDescription = null,
                            tint = if (tournament.isCompleted) 
                                MaterialTheme.colorScheme.onSecondaryContainer 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = tournament.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = tournament.type.name, 
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatDate(tournament.dateCreated),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Spiller og runde info (for aktive turneringer) eller vinder (for afsluttede)
                        if (tournament.isCompleted) {
                            val winners = tournament.players.let { players ->
                                val maxPoints = players.maxOfOrNull { it.totalPoints } ?: 0
                                players.filter { it.totalPoints == maxPoints }.map { it.name }
                            }
                            Text(
                                text = "🏆 ${winners.joinToString(" & ")}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text(
                                text = "${tournament.players.size} spillere • ${tournament.matches.maxOfOrNull { it.roundNumber } ?: 0} runder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val offset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
    
    val direction = when {
        offset > 0f -> SwipeToDismissBoxValue.StartToEnd
        offset < 0f -> SwipeToDismissBoxValue.EndToStart
        else -> SwipeToDismissBoxValue.Settled
    }

    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(
            MaterialTheme.colorScheme.secondaryContainer, 
            Icons.Default.ContentCopy, 
            Alignment.CenterStart
        )
        SwipeToDismissBoxValue.EndToStart -> Triple(
            MaterialTheme.colorScheme.errorContainer, 
            Icons.Default.Delete, 
            Alignment.CenterEnd
        )
        else -> Triple(Color.Transparent, null, Alignment.Center)
    }
    
    val alpha = (kotlin.math.abs(offset) / 100f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (direction == SwipeToDismissBoxValue.StartToEnd) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                else 
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { this.alpha = alpha }
            )
        }
    }
}
