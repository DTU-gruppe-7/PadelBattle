package dk.dtu.padelbattle.presentation.navigation

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.dtu.padelbattle.presentation.theme.PadelOrange

@Composable
fun BottomNavigationBar(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
            // Gør plads til home indicator / gesture bar, så indhold ikke bliver klippet
            //.navigationBarsPadding()
            // Lidt ekstra vertikal luft, især når font-scaling er slået til
            //.padding(vertical = 4.dp)
            // Undgå for lav fast højde
            .heightIn(min = 60.dp)
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Filled.SportsTennis else Icons.Outlined.SportsTennis,
                    contentDescription = "Kampe"
                )
            },
            label = {
                Text(
                    "Kampe",
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PadelOrange,
                selectedTextColor = PadelOrange,
                indicatorColor = PadelOrange.copy(alpha = 0.12f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 1) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                    contentDescription = "Stilling"
                )
            },
            label = {
                Text(
                    "Stilling",
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PadelOrange,
                selectedTextColor = PadelOrange,
                indicatorColor = PadelOrange.copy(alpha = 0.12f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
