package dk.dtu.padelbattle.view.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dk.dtu.padelbattle.view.SettingsMenu
import dk.dtu.padelbattle.view.SettingsMenuItem
import dk.dtu.padelbattle.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentScreen: Screen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    settingsMenuItems: List<SettingsMenuItem>? = null,
    settingsViewModel: SettingsViewModel? = null
) {
    TopAppBar(
        title = { 
            Text(
                text = currentScreen.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ) 
        },
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            if (settingsMenuItems != null && settingsMenuItems.isNotEmpty() && settingsViewModel != null) {
                SettingsMenu(menuItems = settingsMenuItems, viewModel = settingsViewModel)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}