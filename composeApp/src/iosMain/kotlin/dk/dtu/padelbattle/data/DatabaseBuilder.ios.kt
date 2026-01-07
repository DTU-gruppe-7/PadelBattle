// composeApp/src/iosMain/kotlin/dk/dtu/padelbattle/data/DatabaseBuilder.ios.kt
package dk.dtu.padelbattle.data

import androidx.room.Room
import androidx.room.RoomDatabaseConstructor
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO


fun getPadelBattleDatabase(): PadelBattleDatabase {
    // Brug din hjælpefunktion her for at få den korrekte sti
    val dbFilePath = documentDirectory() + "/padelbattle.db"

    return Room.databaseBuilder<PadelBattleDatabase>(
        name = dbFilePath,
        factory = { PadelBattleDatabase::class.instantiateImpl()}
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}