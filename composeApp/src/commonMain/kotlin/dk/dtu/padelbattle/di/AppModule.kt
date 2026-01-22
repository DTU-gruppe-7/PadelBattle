package dk.dtu.padelbattle.di

import dk.dtu.padelbattle.data.PadelBattleDatabase
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.data.repository.TournamentRepositoryImpl
import dk.dtu.padelbattle.viewmodel.ChooseTournamentViewModel
import dk.dtu.padelbattle.viewmodel.HomeViewModel
import dk.dtu.padelbattle.viewmodel.MatchEditViewModel
import dk.dtu.padelbattle.viewmodel.MatchListViewModel
import dk.dtu.padelbattle.viewmodel.SettingsViewModel
import dk.dtu.padelbattle.viewmodel.StandingsViewModel
import dk.dtu.padelbattle.viewmodel.TournamentConfigViewModel
import dk.dtu.padelbattle.viewmodel.TournamentViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin DI module for the application.
 * Defines all dependencies and their scopes.
 */
val appModule = module {
    // Repository (singleton - én instans for hele app-sessionen)
    singleOf(::TournamentRepositoryImpl) bind TournamentRepository::class

    // ViewModels (singleton scope - én instans per app-session)
    single { HomeViewModel(get()) }
    single { ChooseTournamentViewModel() }
    single { TournamentConfigViewModel(get()) }
    single { TournamentViewModel(get()) }
    single { StandingsViewModel() }
    single { MatchEditViewModel(get()) }
    single { MatchListViewModel() }
    single { SettingsViewModel(get()) }
}

/**
 * Database module - skal konfigureres platform-specifikt.
 * Databasen skal provides via platformDatabaseModule().
 */
fun databaseModule(database: PadelBattleDatabase) = module {
    single { database }
    single { database.tournamentDao() }
    single { database.playerDao() }
    single { database.matchDao() }
}
