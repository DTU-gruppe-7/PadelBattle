package dk.dtu.padelbattle.view.navigation

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier
) {
    BottomAppBar {
        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { /* TODO: Implement navigation */ },
                icon = {},
                label = { Text("Spil") }
            )

            NavigationBarItem(
                selected = false,
                onClick = { /* TODO: Implement navigation */ },
                icon = {},
                label = { Text("Scoreboard") }
            )

            NavigationBarItem(
                selected = false,
                onClick = { /* TODO: Implement navigation */ },
                icon = {},
                label = { Text("Indstillinger") }
            )
        }
    }
}

