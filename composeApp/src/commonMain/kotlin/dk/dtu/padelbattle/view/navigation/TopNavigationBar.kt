package dk.dtu.padelbattle.view.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dk.dtu.padelbattle.view.SettingsMenu
import dk.dtu.padelbattle.view.SettingsMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentScreen: Screen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    settingsMenuItems: List<SettingsMenuItem>? = null,
    titleOverride: String? = null
) {
    TopAppBar(
        title = { Text(titleOverride ?: currentScreen.title) },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            if (settingsMenuItems != null && settingsMenuItems.isNotEmpty()) {
                SettingsMenu(menuItems = settingsMenuItems)
            }
        }
    )
}