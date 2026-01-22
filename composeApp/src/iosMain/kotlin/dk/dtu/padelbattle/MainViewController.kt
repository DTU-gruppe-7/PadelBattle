package dk.dtu.padelbattle

import androidx.compose.ui.window.ComposeUIViewController
import dk.dtu.padelbattle.data.getPadelBattleDatabase
import dk.dtu.padelbattle.di.appModule
import dk.dtu.padelbattle.di.databaseModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

fun MainViewController() = ComposeUIViewController {
    // 1. Vi henter den database, der allerede er bygget korrekt i DatabaseBuilder.ios.kt
    val database = getPadelBattleDatabase()

    // 2. Initialiser Koin DI (kun én gang)
    try {
        KoinPlatform.getKoin()
        // Koin er allerede startet
    } catch (e: IllegalStateException) {
        // Koin er ikke startet endnu
        startKoin {
            modules(databaseModule(database), appModule)
        }
    }

    // 3. Start App'en
    App()
}