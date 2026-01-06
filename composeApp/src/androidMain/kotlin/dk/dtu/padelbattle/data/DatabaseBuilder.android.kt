// composeApp/src/androidMain/kotlin/dk/dtu/padelbattle/data/DatabaseBuilder.android.kt
package dk.dtu.padelbattle.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver



fun getPadelBattleDatabase(context: Context): PadelBattleDatabase {
    val dbFile = context.getDatabasePath("padelbattle.db")
    return Room.databaseBuilder<PadelBattleDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}