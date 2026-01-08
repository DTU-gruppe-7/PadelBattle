package dk.dtu.padelbattle.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel

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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${match.team1Player1.name} & ${match.team1Player2.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ScoreInput(
                        score = scoreTeam1,
                        onIncrement = { viewModel.incrementScoreTeam1() },
                        onDecrement = { viewModel.decrementScoreTeam1() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${match.team2Player1.name} & ${match.team2Player2.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ScoreInput(
                        score = scoreTeam2,
                        onIncrement = { viewModel.incrementScoreTeam2() },
                        onDecrement = { viewModel.decrementScoreTeam2() }
                    )
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
private fun ScoreInput(
    score: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onDecrement) {
            Text("-", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        IconButton(onClick = onIncrement) {
            Text("+", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
