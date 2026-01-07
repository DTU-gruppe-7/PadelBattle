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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel

@Composable
fun MatchListScreen(
    matches: List<Match>,
    matchEditViewModel: MatchEditViewModel,
    matchListViewModel: MatchListViewModel,
    onMatchUpdated: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMatchIndex by remember { mutableStateOf(0) }

    // Track revision for recomposition when matches are updated in-place
    val revision by matchListViewModel.revision.collectAsState()

    // Opdater viewModel når matches ændres
    LaunchedEffect(matches) {
        matchListViewModel.setMatches(matches)
    }

    // Brug key() til at tvinge recomposition ved revision changes
    key(revision) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val matchesByRound = matches.groupBy { it.roundNumber }

            matchesByRound.forEach { (round, roundMatches) ->
                item {
                    Text(
                        text = "Runde $round",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(roundMatches.size) { index ->
                    val match = roundMatches[index]
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
    }

    if (showEditDialog && matches.isNotEmpty() && selectedMatchIndex >= 0) {
        // Brug det originale Match-objekt fra turneringen
        val originalMatch = matches[selectedMatchIndex.coerceIn(0, matches.size - 1)]
        MatchEditDialog(
            match = originalMatch,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bane ${match.courtNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Rediger resultat"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${match.team1Player1.name} & ${match.team1Player2.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (match.isPlayed) {
                    Text(
                        text = match.scoreTeam1.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${match.team2Player1.name} & ${match.team2Player2.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (match.isPlayed) {
                    Text(
                        text = match.scoreTeam2.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            if (!match.isPlayed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kamp ikke afviklet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}


