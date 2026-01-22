// composeApp/src/iosMain/kotlin/dk/dtu/padelbattle/data/DatabaseBuilder.ios.kt
package dk.dtu.padelbattle.data

import androidx.room.Room
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Singleton database instans for iOS.
 * LazyThreadSafetyMode.PUBLICATION sikrer thread-safety i Kotlin/Native.
 */
private val databaseInstance: PadelBattleDatabase by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val dbFilePath = documentDirectory() + "/padelbattle.db"
    
    Room.databaseBuilder<PadelBattleDatabase>(
        name = dbFilePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

/**
 * Returnerer singleton database-instansen.
 * Thread-safe via lazy initialization med PUBLICATION mode.
 */
fun getPadelBattleDatabase(): PadelBattleDatabase = databaseInstance

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