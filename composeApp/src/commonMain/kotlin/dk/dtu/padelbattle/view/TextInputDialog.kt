package dk.dtu.padelbattle.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * En genanvendelig dialog til at indtaste tekst.
 * Kan bruges til at ændre turneringsnavn og andre tekstfelter.
 *
 * @param title Dialogens titel
 * @param label Label for tekstfeltet
 * @param currentValue Den nuværende værdi (vises som default)
 * @param onConfirm Callback når brugeren bekræfter med den nye værdi
 * @param onDismiss Callback når dialogen lukkes uden at gemme
 * @param confirmButtonText Tekst på bekræft-knappen (default: "Gem")
 * @param dismissButtonText Tekst på annuller-knappen (default: "Annuller")
 * @param validateInput Valideringsfunktion der returnerer en fejlbesked eller null hvis valid
 */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Gem",
    dismissButtonText: String = "Annuller",
    validateInput: ((String) -> String?)? = null
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
                        errorMessage = validateInput?.invoke(newValue)
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
                    val validationError = validateInput?.invoke(textValue)
                    if (validationError == null) {
                        onConfirm(textValue)
                    } else {
                        errorMessage = validationError
                    }
                },
                enabled = textValue.isNotBlank() && errorMessage == null
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}

