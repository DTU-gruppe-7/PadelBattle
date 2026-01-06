package dk.dtu.padelbattle

import androidx.compose.ui.window.ComposeUIViewController
import dk.dtu.padelbattle.data.getPadelBattleDatabase

fun MainViewController() = ComposeUIViewController {
    // 1. Vi henter den database, der allerede er bygget korrekt i DatabaseBuilder.ios.kt
    val database = getPadelBattleDatabase()

    // 2. Vi sender databasen med ind i App'en
    App(database = database)
}