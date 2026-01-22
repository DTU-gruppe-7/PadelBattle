package dk.dtu.padelbattle.di

import dk.dtu.padelbattle.data.PadelBattleDatabase
import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.data.repository.TournamentRepositoryImpl
import dk.dtu.padelbattle.domain.usecase.RecordMatchResultUseCase
import dk.dtu.padelbattle.domain.service.MatchResultService
import dk.dtu.padelbattle.presentation.home.HomeViewModel
import dk.dtu.padelbattle.presentation.tournament.view.MatchEditViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentContentViewModel
import dk.dtu.padelbattle.presentation.tournament.settings.SettingsViewModel
import dk.dtu.padelbattle.presentation.tournament.config.TournamentConfigViewModel
import dk.dtu.padelbattle.presentation.tournament.view.TournamentViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin DI module for the application.
 * Defines all dependencies and their scopes.
 * 
 * Konsoliderede ViewModels:
 * - TournamentConfigViewModel: Inkluderer turneringstype valg (tidligere ChooseTournamentViewModel)
 * - TournamentContentViewModel: Inkluderer kampe og standings (tidligere MatchListViewModel + StandingsViewModel)
 */
val appModule = module {
    // Repository (singleton - én instans for hele app-sessionen)
    singleOf(::TournamentRepositoryImpl) bind TournamentRepository::class

    // Services (singleton)
    single { MatchResultService(get()) }

    // UseCases (singleton - stateless, så én instans er tilstrækkelig)
    single { RecordMatchResultUseCase(get(), get()) }

    // ViewModels (singleton scope - én instans per app-session)
    // 6 ViewModels i stedet for 8 efter konsolidering
    single { HomeViewModel(get()) }
    single { TournamentConfigViewModel(get()) }  // Inkluderer turneringstype valg
    single { TournamentViewModel(get()) }
    single { TournamentContentViewModel() }      // Erstatter StandingsViewModel + MatchListViewModel
    single { MatchEditViewModel(get()) }         // Modtager RecordMatchResultUseCase
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
