// composeApp/src/commonMain/kotlin/dk/dtu/padelbattle/data/PadelBattleDatabase.kt
package dk.dtu.padelbattle.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.entity.MatchEntity
import dk.dtu.padelbattle.data.entity.PlayerEntity
import dk.dtu.padelbattle.data.entity.TournamentEntity

@Database(entities = [TournamentEntity::class, PlayerEntity::class, MatchEntity::class], version = 1)
@ConstructedBy(PadelBattleDatabaseConstructor::class)
abstract class PadelBattleDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
}

// Dette object er NØDVENDIGT for at Room kan generere koden
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PadelBattleDatabaseConstructor : RoomDatabaseConstructor<PadelBattleDatabase> {
    override fun initialize(): PadelBattleDatabase
}