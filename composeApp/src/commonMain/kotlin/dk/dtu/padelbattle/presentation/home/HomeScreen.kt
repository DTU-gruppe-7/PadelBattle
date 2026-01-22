package dk.dtu.padelbattle.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.domain.util.formatDate
import dk.dtu.padelbattle.presentation.home.HomeViewModel
import dk.dtu.padelbattle.presentation.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGoToTournamentScreen: () -> Unit,
    onTournamentClicked: (String) -> Unit,
    onDuplicateTournament: ((String, String) -> Unit)? = null,
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
            text = { Text("Er du sikker på, at du vil slette denne turnering?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteConfirmation.confirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Slet") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.deleteConfirmation.dismiss() }) { Text("Annuller") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background layer that fills entire screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PadelOrange.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Text(
                text = "Dine Turneringer",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // Premium Segmented Tab Control
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    listOf(
                        "I gang (${activeTournaments.size})" to 0,
                        "Afsluttet (${completedTournaments.size})" to 1
                    ).forEach { (title, index) ->
                        val selected = selectedTabIndex == index
                        val bgColor by animateColorAsState(
                            targetValue = if (selected) PadelOrange else Color.Transparent,
                            animationSpec = tween(200)
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(200)
                        )

                        Surface(
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = bgColor
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tournament List
            val currentTournaments = if (selectedTabIndex == 0) activeTournaments else completedTournaments

            if (currentTournaments.isEmpty()) {
                EmptyStateView(selectedTabIndex)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(currentTournaments.size) { index ->
                        GlassmorphismTournamentCard(
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

        // FAB Row
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Search FAB
            FloatingActionButton(
                onClick = onGoToSearchScreen,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Søg")
            }

            // Create Tournament FAB
            ExtendedFloatingActionButton(
                onClick = onGoToTournamentScreen,
                containerColor = PadelOrange,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                icon = { Icon(Icons.Default.Add, "Opret") },
                text = { Text("Ny Turnering", fontWeight = FontWeight.Bold) }
            )
        }
    }
}

@Composable
fun EmptyStateView(selectedTabIndex: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (selectedTabIndex == 0) Icons.Default.SportsTennis else Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = PadelOrange.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (selectedTabIndex == 0) "Ingen aktive turneringer" else "Ingen afsluttede turneringer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedTabIndex == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tryk 'Ny Turnering' for at starte",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassmorphismTournamentCard(
    tournament: Tournament,
    onClick: () -> Unit,
    onDuplicate: ((Tournament) -> Unit),
    onDelete: ((Tournament) -> Unit)? = null
) {
    var hasDuplicated by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.25f }
    )

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                if (!hasDuplicated) {
                    hasDuplicated = true
                    onDuplicate.invoke(tournament)
                }
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete?.invoke(tournament)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> {
                hasDuplicated = false
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Glassmorphism Card
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (tournament.isCompleted)
                                    listOf(WarmGold.copy(alpha = 0.8f), DeepAmber.copy(alpha = 0.6f))
                                else
                                    listOf(PadelOrange.copy(alpha = 0.8f), PadelOrangeLight.copy(alpha = 0.6f))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tournament.isCompleted) Icons.Default.EmojiEvents else Icons.Default.SportsTennis,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Type Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PadelOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = tournament.type.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PadelOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

                    // Info row
                    if (tournament.isCompleted) {
                        val winners = tournament.players.let { players ->
                            val maxPoints = players.maxOfOrNull { it.totalPoints } ?: 0
                            players.filter { it.totalPoints == maxPoints }.map { it.name }
                        }
                        Text(
                            text = "🏆 ${winners.joinToString(" & ")}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarmGold
                        )
                    } else {
                        Text(
                            text = "${tournament.players.size} spillere • ${tournament.matches.maxOfOrNull { it.roundNumber } ?: 0} runder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = PadelOrange.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
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
            SuccessGreen.copy(alpha = 0.9f),
            Icons.Default.ContentCopy,
            Alignment.CenterStart
        )
        SwipeToDismissBoxValue.EndToStart -> Triple(
            ErrorRed.copy(alpha = 0.9f),
            Icons.Default.Delete,
            Alignment.CenterEnd
        )
        else -> Triple(Color.Transparent, null, Alignment.Center)
    }

    val alpha = (kotlin.math.abs(offset) / 100f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { this.alpha = alpha }
            )
        }
    }
}
