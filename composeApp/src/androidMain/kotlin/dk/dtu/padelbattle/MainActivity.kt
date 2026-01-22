package dk.dtu.padelbattle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import dk.dtu.padelbattle.data.getPadelBattleDatabase
import dk.dtu.padelbattle.di.appModule
import dk.dtu.padelbattle.di.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display and make system bars transparent
        enableEdgeToEdge()

        // Make system bars respect the dark theme
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Opret databasen
        val database = getPadelBattleDatabase(applicationContext)

        // Initialiser Koin DI (kun én gang)
        try {
            org.koin.mp.KoinPlatform.getKoin()
            // Koin er allerede startet
        } catch (e: IllegalStateException) {
            // Koin er ikke startet endnu
            startKoin {
                androidLogger()
                androidContext(this@MainActivity)
                modules(databaseModule(database), appModule)
            }
        }

        setContent {
            App()
        }
    }
}