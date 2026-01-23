package dk.dtu.padelbattle.presentation.tournament.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsDialogType
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsViewModel

/**
 * Composable der håndterer alle settings-relaterede dialoger.
 * Observerer SettingsViewModel og renderer den relevante dialog baseret på state.
 */
@Composable
fun SettingsDialogs(settingsViewModel: SettingsViewModel) {
    val currentDialogType by settingsViewModel.currentDialogType.collectAsState()
    val isUpdatingCourts by settingsViewModel.isUpdatingCourts.collectAsState()

    when (val dialogType = currentDialogType) {
        is SettingsDialogType.EditTournamentName -> {
            EditTournamentNameDialog(
                currentName = dialogType.currentName,
                onConfirm = { newName ->
                    settingsViewModel.updateTournamentName(dialogType.tournamentId, newName)
                },
                onDismiss = { settingsViewModel.dismissDialog() }
            )
        }
        is SettingsDialogType.EditNumberOfCourts -> {
            NumberOfCourtsDialog(
                currentCourts = dialogType.currentCourts,
                maxCourts = dialogType.maxCourts,
                hasPlayedMatches = dialogType.hasPlayedMatches,
                isLoading = isUpdatingCourts,
                onConfirm = { newCourts ->
                    settingsViewModel.updateNumberOfCourts(dialogType.tournamentId, newCourts)
                },
                onCancel = { settingsViewModel.dismissDialog() }
            )
        }
        null -> { /* Ingen dialog */ }
    }
}

/**
 * Dialog til at ændre turneringsnavn
 */
@Composable
private fun EditTournamentNameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ændre Turneringsnavn",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        textValue = newValue
                        errorMessage = if (newValue.isBlank()) "Navn må ikke være tomt" else null
                    },
                    label = { Text("Turneringsnavn") },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (textValue.isNotBlank()) {
                        onConfirm(textValue)
                    } else {
                        errorMessage = "Navn må ikke være tomt"
                    }
                },
                enabled = textValue.isNotBlank() && errorMessage == null
            ) {
                Text("Gem")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuller")
            }
        }
    )
}

/**
 * Generisk dialog til tekstinput med validering
 */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    validateInput: (String) -> String? = { null }
) {
    var textValue by remember { mutableStateOf(currentValue) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        textValue = newValue
                        errorMessage = validateInput(newValue)
                    },
                    label = { Text(label) },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validationError = validateInput(textValue)
                    if (validationError == null) {
                        onConfirm(textValue)
                    } else {
                        errorMessage = validationError
                    }
                },
                enabled = textValue.isNotBlank() && errorMessage == null
            ) {
                Text("Gem")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuller")
            }
        }
    )
}

/**
 * Advarselsdialog der vises når brugeren forsøger at ændre points efter at kampe er spillet
 */
@Composable
fun PointsChangeWarningDialog(
    newPoints: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
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
                    text = "⚠️ Advarsel",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Du er ved at ændre antallet af points pr. kamp til $newPoints.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Da der allerede er spillet kampe, kan dette resultere i at ikke alle spillere ender med at spille det samme antal points i alt.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuller")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Fortsæt")
                    }
                }
            }
        }
    }
}

/**
 * Dialog til at ændre antallet af baner i en turnering.
 * Viser kun valgmulighederne hvis der ikke er nogen spillede kampe.
 * Viser loading state mens opdateringen sker.
 */
@Composable
fun NumberOfCourtsDialog(
    currentCourts: Int,
    maxCourts: Int,
    hasPlayedMatches: Boolean,
    isLoading: Boolean = false,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var selectedCourts by remember { mutableStateOf(currentCourts) }

    Dialog(onDismissRequest = { if (!isLoading) onCancel() }) {
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
                    text = "Ændre antal baner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    // Vis loading state
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.padding(32.dp)
                    )

                    Text(
                        text = "Genererer nye kampe...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else if (hasPlayedMatches) {
                    // Vis besked om at ændring ikke er tilladt
                    Text(
                        text = "⚠️ Kan ikke ændres",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Du kan ikke ændre antallet af baner, når der allerede er blevet spillet kampe.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK")
                    }
                } else {
                    Text(
                        text = "Vælg det nye antal baner. Alle eksisterende kampe vil blive slettet og nye kampe vil blive genereret.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Knapper til at vælge antal baner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        for (i in 1..maxCourts) {
                            val isSelected = selectedCourts == i
                            if (isSelected) {
                                Button(
                                    onClick = { selectedCourts = i },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("$i")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedCourts = i },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("$i")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuller")
                        }
                        Button(
                            onClick = { onConfirm(selectedCourts) },
                            modifier = Modifier.weight(1f),
                            enabled = selectedCourts != currentCourts
                        ) {
                            Text("Gem")
                        }
                    }
                }
            }
        }
    }
}
