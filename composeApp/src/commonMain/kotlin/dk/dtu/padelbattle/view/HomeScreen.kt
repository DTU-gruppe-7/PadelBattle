package dk.dtu.padelbattle.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import androidx.compose.ui.graphics.Color // HUSK at importere denne!

// --- HER STARTER TRIN 3 (Selve hovedskærmen) ---
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGoToTournamentScreen: () -> Unit,
    onTournamentClicked: (String) -> Unit
) {
    val tournaments by viewModel.tournaments.collectAsState()

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

            if (tournaments.isEmpty()) {
                // Empty State - vises hvis listen er tom
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ingen turneringer endnu",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tryk på + for at starte en ny kamp!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Listen med turneringer
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Plads til FAB knappen
                ) {
                    items(tournaments.size) { index ->
                        // Her bruger vi hjælpe-komponenten fra Trin 2
                        TournamentItemCard(
                            tournament = tournaments[index],
                            onClick = { onTournamentClicked(tournaments[index].id) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun TournamentItemCard(
    tournament: Tournament,
    onClick: () -> Unit
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
                // Ikon baseret på om den er færdig eller ej
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
                    Text(
                        text = "${tournament.players.size} spillere • ${tournament.matches.maxOfOrNull { it.roundNumber } ?: 0} runder",
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

// Hjælpefunktion til dato
fun formatDate(timestamp: Long): String {
    // Dette er en simpel placeholder.
    // timestamp er millisekunder siden 1970.
    // For en rigtig dato-visning anbefales et bibliotek som kotlinx-datetime.
    return "ID: ${timestamp.toString().takeLast(4)}"
}