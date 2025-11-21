package dk.dtu.padelbattle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ChooseTournamentScreen(
    onGoToAmericano : () -> Unit,
    onGoToMexicano : () -> Unit,
    onGoBack: () -> Unit

) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Vælg turneringstype")

        Button(onClick = onGoToAmericano) {
            Text(text = "Americano")
        }
        Button(onClick = onGoToMexicano) {
            Text(text = "Mexicano")
        }

    }

}