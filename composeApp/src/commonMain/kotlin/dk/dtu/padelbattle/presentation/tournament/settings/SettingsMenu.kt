package dk.dtu.padelbattle.presentation.tournament.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsDialogType
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsViewModel

/**
 * Data class to represent a settings menu item
 * @param label The text to display for this menu option
 * @param onClick The action to perform when this option is clicked
 */
data class SettingsMenuItem(
    val label: String,
    val onClick: () -> Unit
)

/**
 * A reusable settings menu component with a gear icon button
 * @param menuItems List of menu items to display when the settings button is clicked
 * @param modifier Optional modifier for positioning the settings button
 */
@Composable
fun SettingsMenu(
    menuItems: List<SettingsMenuItem>,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val showDeleteConfirmation by viewModel.deleteConfirmation.showDeleteConfirmation.collectAsState()
    val currentDialogType by viewModel.currentDialogType.collectAsState()

    Box(modifier = modifier) {
        IconButton(
                onClick = { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Indstillinger",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            menuItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        expanded = false
                        item.onClick()
                    }
                )
            }
        }

        // Show delete confirmation dialog separately (not as a parameter to IconButton)
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.deleteConfirmation.dismiss() },
                title = { Text("Slet turnering") },
                text = { Text("Er du sikker på, at du vil slette denne turnering?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteConfirmation.confirm() }) {
                        Text("Slet", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.deleteConfirmation.dismiss() }) {
                        Text("Annuller")
                    }
                }
            )
        }

        // Håndter settings dialoge
        val isUpdatingCourts by viewModel.isUpdatingCourts.collectAsState()

        when (val dialogType = currentDialogType) {
            is SettingsDialogType.EditTournamentName -> {
                TextInputDialog(
                    title = "Ændr turneringsnavn",
                    label = "Turneringsnavn",
                    currentValue = dialogType.currentName,
                    onConfirm = { newName ->
                        viewModel.updateTournamentName(dialogType.tournamentId, newName)
                    },
                    onDismiss = { viewModel.dismissDialog() },
                    validateInput = { name ->
                        if (name.isBlank()) "Navn må ikke være tomt" else null
                    }
                )
            }
            is SettingsDialogType.EditNumberOfCourts -> {
                NumberOfCourtsDialog(
                    currentCourts = dialogType.currentCourts,
                    maxCourts = dialogType.maxCourts,
                    hasPlayedMatches = dialogType.hasPlayedMatches,
                    isLoading = isUpdatingCourts,
                    onConfirm = { newCourts ->
                        viewModel.updateNumberOfCourts(dialogType.tournamentId, newCourts)
                    },
                    onCancel = { viewModel.dismissDialog() }
                )
            }
            null -> { /* Ingen dialog */ }
        }
    }
}
