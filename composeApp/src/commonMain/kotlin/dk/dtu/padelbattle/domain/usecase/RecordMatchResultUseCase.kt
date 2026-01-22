package dk.dtu.padelbattle.domain.usecase

import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.MatchResult
import dk.dtu.padelbattle.domain.model.TournamentType
import dk.dtu.padelbattle.domain.service.MatchResultService

/**
 * UseCase til at registrere et kampresultat.
 *
 * Håndterer:
 * - Gemning af kampresultat
 * - Opdatering af spillerstatistik
 * - Automatisk generering af nye runder (Mexicano)
 * - Afslutning af turnering når alle kampe er spillet
 *
 * Denne UseCase isolerer forretningslogikken fra ViewModel'en,
 * hvilket gør koden mere testbar og vedligeholdelsesvenlig.
 */
class RecordMatchResultUseCase(
    private val repository: TournamentRepository,
    private val matchResultService: MatchResultService
) {
    /**
     * Resultat fra at registrere et kampresultat.
     */
    sealed class Result {
        /**
         * Kampen blev gemt succesfuldt, turneringen fortsætter.
         */
        data class MatchSaved(val match: Match) : Result()

        /**
         * Kampen blev gemt, turneringen er nu afsluttet.
         */
        data class TournamentCompleted(val match: Match) : Result()

        /**
         * Kampen blev gemt, en ny runde blev genereret (Mexicano).
         */
        data class NewRoundGenerated(val match: Match, val newMatchCount: Int) : Result()

        /**
         * Der opstod en fejl.
         */
        data class Error(val message: String, val exception: Throwable? = null) : Result()
    }

    /**
     * Registrerer et kampresultat.
     *
     * @param match Kampen der skal opdateres
     * @param result Resultatet (scores)
     * @param tournamentId ID på turneringen
     * @return Result der indikerer hvad der skete
     */
    suspend operator fun invoke(
        match: Match,
        result: MatchResult,
        tournamentId: String
    ): Result {
        if (tournamentId.isBlank()) {
            return Result.Error("Tournament ID mangler")
        }

        return try {
            // 1. Gem resultatet via MatchResultService
            matchResultService.recordMatchResult(match, result, tournamentId)

            // 2. Tjek hvor mange kampe der mangler
            val unplayedCount = repository.countUnplayedMatches(tournamentId)

            // Hvis der stadig er kampe tilbage, fortsæt
            if (unplayedCount > 0) {
                return Result.MatchSaved(match)
            }

            // 3. Alle kampe i runden er spillet - vurder næste skridt
            val tournament = repository.getTournamentById(tournamentId)
                ?: return Result.Error("Turnering ikke fundet: $tournamentId")

            when (tournament.type) {
                TournamentType.MEXICANO -> handleMexicanoRoundComplete(match, tournament.id, tournament)
                TournamentType.AMERICANO -> handleAmericanoComplete(match, tournamentId)
            }
        } catch (e: Exception) {
            Result.Error("Kunne ikke gemme kampresultat: ${e.message}", e)
        }
    }

    /**
     * Håndterer afslutning af en runde i Mexicano-turnering.
     * Genererer automatisk nye kampe eller afslutter turneringen.
     */
    private suspend fun handleMexicanoRoundComplete(
        match: Match,
        tournamentId: String,
        tournament: dk.dtu.padelbattle.domain.model.Tournament
    ): Result {
        val playedMatches = tournament.matches.filter { it.isPlayed }

        // Beregn hvor mange kampe hver spiller har spillet
        val matchesPerPlayer = tournament.players.associate { player ->
            player.name to playedMatches.count { m ->
                m.team1Player1.id == player.id ||
                m.team1Player2.id == player.id ||
                m.team2Player1.id == player.id ||
                m.team2Player2.id == player.id
            }
        }

        val minMatchesPlayed = matchesPerPlayer.values.minOrNull() ?: 0
        val maxMatchesPlayed = matchesPerPlayer.values.maxOrNull() ?: 0
        val allHaveEqualMatches = minMatchesPlayed == maxMatchesPlayed

        // Tjek extension tracker
        val canCompleteFromTracker = repository.decrementExtensionRounds(tournamentId)

        // Afslut hvis: mindst 3 kampe per spiller, alle har lige mange, og extension tillader det
        if (minMatchesPlayed >= 3 && allHaveEqualMatches && canCompleteFromTracker) {
            repository.clearExtensionTracking(tournamentId)
            repository.updateTournamentCompleted(tournamentId, true)
            return Result.TournamentCompleted(match)
        }

        // Ellers generer ny runde
        return generateNewMexicanoRound(match, tournament)
    }

    /**
     * Genererer en ny runde af kampe for Mexicano-turnering.
     */
    private suspend fun generateNewMexicanoRound(
        match: Match,
        tournament: dk.dtu.padelbattle.domain.model.Tournament
    ): Result {
        val extendedTournament = tournament.generateExtensionMatches()
            ?: return Result.MatchSaved(match)

        // Find de nye kampe (forskellen mellem gammel og ny)
        val existingMatchIds = tournament.matches.map { it.id }.toSet()
        val newMatches = extendedTournament.matches.filter { it.id !in existingMatchIds }

        if (newMatches.isEmpty()) {
            return Result.MatchSaved(match)
        }

        repository.insertMatches(newMatches, tournament.id)

        return Result.NewRoundGenerated(match, newMatches.size)
    }

    /**
     * Håndterer afslutning af Americano-turnering.
     * I Americano er alle kampe genereret fra start, så når alle er spillet er turneringen færdig.
     */
    private suspend fun handleAmericanoComplete(match: Match, tournamentId: String): Result {
        repository.updateTournamentCompleted(tournamentId, true)
        return Result.TournamentCompleted(match)
    }
}
