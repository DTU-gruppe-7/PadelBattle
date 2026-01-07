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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MatchListScreen() {
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMatchIndex by remember { mutableStateOf(0) }

    // Mock data for visual prototype
    val mockMatches = listOf(
        MockMatch(1, 1, "Alice", "Bob", "Charlie", "David", 6, 4, true),
        MockMatch(1, 2, "Emma", "Frank", "Grace", "Henry", 0, 0, false),
        MockMatch(2, 1, "Alice", "Charlie", "Bob", "David", 0, 0, false)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val matchesByRound = mockMatches.groupBy { it.roundNumber }

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
                            selectedMatchIndex = index
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog && mockMatches.isNotEmpty()) {
        val match = mockMatches[selectedMatchIndex.coerceIn(0, mockMatches.size - 1)]
        MatchEditDialog(
            team1Player1 = match.team1Player1,
            team1Player2 = match.team1Player2,
            team2Player1 = match.team2Player1,
            team2Player2 = match.team2Player2,
            courtNumber = match.courtNumber,
            roundNumber = match.roundNumber,
            initialScoreTeam1 = match.scoreTeam1,
            initialScoreTeam2 = match.scoreTeam2,
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun MatchCard(
    match: MockMatch,
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
                        text = "${match.team1Player1} & ${match.team1Player2}",
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
                        text = "${match.team2Player1} & ${match.team2Player2}",
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

// Mock data class for visual prototype
private data class MockMatch(
    val roundNumber: Int,
    val courtNumber: Int,
    val team1Player1: String,
    val team1Player2: String,
    val team2Player1: String,
    val team2Player2: String,
    val scoreTeam1: Int,
    val scoreTeam2: Int,
    val isPlayed: Boolean
)

