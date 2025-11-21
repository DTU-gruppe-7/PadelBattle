package dk.dtu.padelbattle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChoosePlayerScreen(
    onGoToStart: (playerNames: String) -> Unit,
    onGoBack: () -> Unit
) {
    var playerNames by remember { mutableStateOf(listOf<String>()) }
    var currentPlayerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Indtast spillernavne")

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentPlayerName,
                onValueChange = { currentPlayerName = it },
                label = { Text("Spillernavn") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (currentPlayerName.isNotBlank()) {
                        playerNames = playerNames + currentPlayerName
                        currentPlayerName = ""
                    }
                },
                enabled = currentPlayerName.isNotBlank() && playerNames.size < 8
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tilføj spiller")
            }
        }

        if (playerNames.size < 4) {
            Text("Tilføj venligst mindst 4 spillere. (${playerNames.size} tilføjet)")
        } else {
            Text("${playerNames.size} spillere tilføjet.")
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(playerNames) { index, playerName ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. $playerName",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    IconButton(onClick = {
                        val newPlayerNames = playerNames.toMutableList()
                        newPlayerNames.removeAt(index)
                        playerNames = newPlayerNames
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Fjern spiller")
                    }
                }
            }
        }

        val playerCountIsValid = playerNames.size in 4..8

        Button(
            onClick = { onGoToStart(playerNames.joinToString(",")) },
            enabled = playerCountIsValid
        ) {
            Text("Start spillet")
        }
    }
}