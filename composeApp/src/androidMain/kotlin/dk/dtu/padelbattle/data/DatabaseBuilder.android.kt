// composeApp/src/androidMain/kotlin/dk/dtu/padelbattle/data/DatabaseBuilder.android.kt
package dk.dtu.padelbattle.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Singleton database holder for Android.
 * Undgår at oprette nye database-instanser ved hver Activity recreation.
 */
private object DatabaseHolder {
    @Volatile
    private var instance: PadelBattleDatabase? = null

    fun getDatabase(context: Context): PadelBattleDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): PadelBattleDatabase {
        val dbFile = context.getDatabasePath("padelbattle.db")
        return Room.databaseBuilder<PadelBattleDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}

/**
 * Returnerer singleton database-instansen.
 * Thread-safe via double-checked locking.
 */
fun getPadelBattleDatabase(context: Context): PadelBattleDatabase = 
    DatabaseHolder.getDatabase(context)