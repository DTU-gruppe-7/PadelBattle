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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel

@Composable
fun MatchListScreen(
    matches: List<Match>,
    currentTournament: Tournament,
    matchEditViewModel: MatchEditViewModel,
    matchListViewModel: MatchListViewModel,
    onMatchUpdated: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMatchIndex by remember { mutableStateOf(0) }

    // Brug currentRound fra ViewModel for at bevare state gennem recompositions
    val currentRound by matchListViewModel.currentRound.collectAsState()

    // Track revision for recomposition when matches are updated in-place
    val revision by matchListViewModel.revision.collectAsState()

    // Opdater viewModel når matches ændres - brug updateMatches for at bevare nuværende runde
    LaunchedEffect(matches) {
        matchListViewModel.updateMatches(matches)
    }

    // Beregn antallet af runder
    val maxRound = matches.maxOfOrNull { it.roundNumber } ?: 1
    val minRound = matches.minOfOrNull { it.roundNumber } ?: 1

    // Filtrer matches for den aktuelle runde
    val currentRoundMatches = matches.filter { it.roundNumber == currentRound }

    // Brug key() til at tvinge recomposition ved revision changes
    key(revision) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Navigation header med pile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newRound = (currentRound - 1).coerceAtLeast(minRound)
                        matchListViewModel.setCurrentRound(newRound)
                    },
                    enabled = currentRound > minRound
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Forrige runde",
                        tint = if (currentRound > minRound)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Text(
                    text = "Runde $currentRound",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = {
                        val newRound = (currentRound + 1).coerceAtMost(maxRound)
                        matchListViewModel.setCurrentRound(newRound)
                    },
                    enabled = currentRound < maxRound
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Næste runde",
                        tint = if (currentRound < maxRound)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Liste med kampe for den aktuelle runde
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentRoundMatches.size) { index ->
                    val match = currentRoundMatches[index]
                    MatchCard(
                        match = match,
                        onEditClick = {
                            // Find index i originale matches liste
                            selectedMatchIndex = matches.indexOfFirst { it.id == match.id }
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog && matches.isNotEmpty() && selectedMatchIndex >= 0) {
        // Brug det originale Match-objekt fra turneringen
        val originalMatch = matches[selectedMatchIndex.coerceIn(0, matches.size - 1)]
        MatchEditDialog(
            match = originalMatch,
            currentTournament = currentTournament,
            viewModel = matchEditViewModel,
            onSave = {
                showEditDialog = false
                // Notificer om opdatering for at trigger recomposition
                matchListViewModel.notifyMatchUpdated()
                onMatchUpdated()
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun MatchCard(
    match: Match,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (match.isPlayed) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header med bane nummer (centreret)
            Text(
                text = "Bane ${match.courtNumber}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
            )

            // Hovedlayout: Hold 1 til venstre, scores i midten, Hold 2 til højre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hold 1 (venstre side)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = match.team1Player1.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = match.team1Player2.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Score bokse i midten (klikbare)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Card(
                        onClick = onEditClick,
                        modifier = Modifier.padding(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = if (match.isPlayed) match.scoreTeam1.toString() else "-",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        onClick = onEditClick,
                        modifier = Modifier.padding(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = if (match.isPlayed) match.scoreTeam2.toString() else "-",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Hold 2 (højre side)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = match.team2Player1.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = match.team2Player2.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!match.isPlayed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Klik på score for at indtaste resultat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}


