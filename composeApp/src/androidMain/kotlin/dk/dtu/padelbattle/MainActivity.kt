package dk.dtu.padelbattle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.launch
import java.util.UUID // Bruges til at lave unikke String ID'er
import dk.dtu.padelbattle.data.entity.TournamentEntity
import dk.dtu.padelbattle.data.entity.PlayerEntity
import dk.dtu.padelbattle.data.getPadelBattleDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Opret databasen her
        val database = getPadelBattleDatabase(applicationContext)

        setContent {
            App(database = database) // Send den med ind
        }
    }
}