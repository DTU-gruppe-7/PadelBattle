package dk.dtu.padelbattle.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.ui.theme.*

@Composable
fun MatchListScreen(
    matches: List<Match>,
    currentTournament: Tournament,
    matchEditViewModel: MatchEditViewModel,
    matchListViewModel: MatchListViewModel,
    onMatchUpdated: () -> Unit,
    onTournamentCompleted: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMatchIndex by remember { mutableStateOf(0) }

    val currentRound by matchListViewModel.currentRound.collectAsState()
    val revision by matchListViewModel.revision.collectAsState()

    LaunchedEffect(matches) {
        matchListViewModel.updateMatches(matches)
    }

    val maxRound = matches.maxOfOrNull { it.roundNumber } ?: 1
    val minRound = matches.minOfOrNull { it.roundNumber } ?: 1
    val currentRoundMatches = matches.filter { it.roundNumber == currentRound }

    key(revision) {
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Round Navigation Header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newRound = (currentRound - 1).coerceAtLeast(minRound)
                                matchListViewModel.setCurrentRound(newRound)
                            },
                            enabled = currentRound > minRound,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = PadelOrange,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(Icons.Default.ArrowBack, "Forrige runde")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RUNDE",
                                style = MaterialTheme.typography.labelSmall,
                                color = PadelOrange,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "$currentRound",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                val newRound = (currentRound + 1).coerceAtMost(maxRound)
                                matchListViewModel.setCurrentRound(newRound)
                            },
                            enabled = currentRound < maxRound,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = PadelOrange,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(Icons.Default.ArrowForward, "Næste runde")
                        }
                    }
                }

                // Beregn spillere der sidder over i denne runde via ViewModel
                val sittingOutPlayers = matchListViewModel.getSittingOutPlayers(
                    allPlayers = currentTournament.players,
                    roundMatches = currentRoundMatches
                )

                // Match List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(currentRoundMatches.size) { index ->
                        val match = currentRoundMatches[index]
                        PremiumMatchCard(
                            match = match,
                            onEditClick = {
                                selectedMatchIndex = matches.indexOfFirst { it.id == match.id }
                                showEditDialog = true
                            }
                        )
                    }

                    // Vis spillere der sidder over
                    if (sittingOutPlayers.isNotEmpty()) {
                        item {
                            SittingOutCard(players = sittingOutPlayers.map { it.name })
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && matches.isNotEmpty() && selectedMatchIndex >= 0) {
        val originalMatch = matches[selectedMatchIndex.coerceIn(0, matches.size - 1)]
        MatchEditDialog(
            match = originalMatch,
            currentTournament = currentTournament,
            viewModel = matchEditViewModel,
            onSave = {
                showEditDialog = false
                matchListViewModel.notifyMatchUpdated()
                onMatchUpdated()
            },
            onDismiss = { showEditDialog = false },
            onTournamentCompleted = onTournamentCompleted
        )
    }
}

@Composable
private fun PremiumMatchCard(
    match: Match,
    onEditClick: () -> Unit
) {
    Card(
        onClick = onEditClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Header strip with court number
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (match.isPlayed) MaterialTheme.colorScheme.surfaceVariant 
                        else PadelOrange
                    )
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "BANE ${match.courtNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (match.isPlayed) MaterialTheme.colorScheme.onSurfaceVariant 
                            else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                
                if (!match.isPlayed) {
                     Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).align(Alignment.CenterEnd),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // Teams and Score Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team 1
                TeamColumn(
                    player1 = match.team1Player1.name,
                    player2 = match.team1Player2.name,
                    isWinner = match.isPlayed && match.scoreTeam1 > match.scoreTeam2,
                    isLeft = true,
                    modifier = Modifier.weight(1f)
                )

                // Score Display
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreBox(
                        score = if (match.isPlayed) match.scoreTeam1.toString() else "-",
                        isWinner = match.isPlayed && match.scoreTeam1 > match.scoreTeam2,
                        isPlayed = match.isPlayed
                    )
                    
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )

                    ScoreBox(
                        score = if (match.isPlayed) match.scoreTeam2.toString() else "-",
                        isWinner = match.isPlayed && match.scoreTeam2 > match.scoreTeam1,
                        isPlayed = match.isPlayed
                    )
                }

                // Team 2
                TeamColumn(
                    player1 = match.team2Player1.name,
                    player2 = match.team2Player2.name,
                    isWinner = match.isPlayed && match.scoreTeam2 > match.scoreTeam1,
                    isLeft = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TeamColumn(
    player1: String,
    player2: String,
    isWinner: Boolean,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End
    ) {
        Text(
            text = player1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            color = if (isWinner) PadelOrange else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = player2,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            color = if (isWinner) PadelOrange else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ScoreBox(
    score: String,
    isWinner: Boolean,
    isPlayed: Boolean
) {
    Surface(
        color = when {
            isWinner -> PadelOrange
            isPlayed -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        contentColor = when {
            isWinner -> MaterialTheme.colorScheme.onPrimary
            isPlayed -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> PadelOrange.copy(alpha = 0.6f)
        },
        shape = RoundedCornerShape(10.dp),
        border = if (!isPlayed) androidx.compose.foundation.BorderStroke(2.dp, PadelOrange.copy(alpha = 0.4f)) else null,
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = score,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SittingOutCard(players: List<String>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "SIDDER OVER",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Players
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                players.forEachIndexed { index, playerName ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Person ikon med "x" overlay
                        Box(
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = PadelOrange.copy(alpha = 0.7f)
                            )
                            // Lille "x" i nederste højre hjørne
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = playerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < players.size - 1) {
                        Text(
                            text = "  •  ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

