package dk.dtu.padelbattle.view

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MatchEditDialog(
    match: Match,
    tournamentId: String,
    viewModel: MatchEditViewModel,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val scoreTeam1 by viewModel.scoreTeam1.collectAsState()
    val scoreTeam2 by viewModel.scoreTeam2.collectAsState()
    var isSaving by remember { mutableStateOf(false) }

    // Initialiser viewModel med kampen
    LaunchedEffect(match) {
        viewModel.setMatch(match)
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
                    text = "Rediger Resultat",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bane ${match.courtNumber} - Runde ${match.roundNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Horisontalt layout med hold 1 til venstre, scores i midten, hold 2 til højre
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

                    // Score inputs i midten
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreWheelPicker(
                            score = scoreTeam1,
                            onScoreChange = { viewModel.updateScoreTeam1(it) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "-",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        ScoreWheelPicker(
                            score = scoreTeam2,
                            onScoreChange = { viewModel.updateScoreTeam2(it) }
                        )
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

                Spacer(modifier = Modifier.height(24.dp))

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
                            if (!isSaving) {
                                isSaving = true
                                viewModel.saveMatch(tournamentId) { savedMatch ->
                                    isSaving = false
                                    if (savedMatch != null) {
                                        onSave()
                                    } else {
                                        // Fejl skete - lad dialogen være åben
                                        println("Failed to save match")
                                    }
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Text(
                            if (isSaving) "Gemmer..." else "Gem",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreWheelPicker(
    score: Int,
    onScoreChange: (Int) -> Unit,
    maxScore: Int = 16
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = score)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Update score when scrolling
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index in 0..maxScore) {
                    onScoreChange(index)
                }
            }
    }

    // Scroll to score when it changes externally
    LaunchedEffect(score) {
        if (listState.firstVisibleItemIndex != score) {
            listState.scrollToItem(score)
        }
    }

    Box(
        modifier = Modifier
            .height(180.dp)  // Øget fra 120dp
            .width(100.dp)   // Øget fra 80dp
    ) {
        // Top fade indicator
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            thickness = 2.dp
        )

        // Bottom fade indicator
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            thickness = 2.dp
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Spacer items at top and bottom for centering
            items(1) {
                Spacer(modifier = Modifier.height(70.dp))  // Øget fra 40dp
            }

            items(maxScore + 1) { index ->
                val isSelected = index == listState.firstVisibleItemIndex
                Card(
                    modifier = Modifier
                        .padding(vertical = 6.dp)  // Øget fra 4dp
                        .size(width = 70.dp, height = 50.dp),  // Øget fra 60x40dp
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 4.dp else 1.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.headlineLarge,  // Øget fra headlineMedium
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .padding(8.dp)
                                .alpha(if (isSelected) 1f else 0.6f),  // Øget fra 0.5f
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(1) {
                Spacer(modifier = Modifier.height(70.dp))  // Øget fra 40dp
            }
        }
    }
}
