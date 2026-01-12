package dk.dtu.padelbattle.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.viewmodel.TournamentViewModel

@Composable
fun EditTournamentNameScreen(
    viewModel: TournamentViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tournamentName by viewModel.tournamentName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = tournamentName,
            onValueChange = { viewModel.updateTournamentNameInput(it) },
            label = { Text("Turneringsnavn") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.saveTournamentName(onSuccess = onSaved)
            },
            enabled = !isLoading && tournamentName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Gemmer..." else "Gem")
        }
    }
}

