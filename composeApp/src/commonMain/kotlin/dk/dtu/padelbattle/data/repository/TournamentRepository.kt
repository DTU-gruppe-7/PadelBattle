package dk.dtu.padelbattle.data.repository

import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Tournament data operations.
 * Provides a clean API for accessing tournament data without exposing
 * the underlying database implementation details.
 */
interface TournamentRepository {

    // =====================================================
    // TOURNAMENT OPERATIONS
    // =====================================================

    /**
     * Henter alle turneringer som et reaktivt Flow.
     * Inkluderer spillere og kampe for hver turnering.
     */
    fun getAllTournaments(): Flow<List<Tournament>>

    /**
     * Henter en specifik turnering med alle spillere og kampe.
     * @param tournamentId ID på turneringen
     * @return Tournament eller null hvis ikke fundet
     */
    suspend fun getTournamentById(tournamentId: String): Tournament?

    /**
     * Gemmer en ny turnering med alle spillere og kampe.
     * @param tournament Turneringen der skal gemmes
     */
    suspend fun saveTournament(tournament: Tournament)

    /**
     * Opdaterer turneringens navn.
     * @param tournamentId ID på turneringen
     * @param newName Det nye navn
     */
    suspend fun updateTournamentName(tournamentId: String, newName: String)

    /**
     * Opdaterer antal baner for turneringen.
     * @param tournamentId ID på turneringen
     * @param numberOfCourts Det nye antal baner
     */
    suspend fun updateNumberOfCourts(tournamentId: String, numberOfCourts: Int)

    /**
     * Opdaterer point per kamp for turneringen.
     * @param tournamentId ID på turneringen
     * @param pointsPerMatch Det nye antal point
     */
    suspend fun updatePointsPerMatch(tournamentId: String, pointsPerMatch: Int)

    /**
     * Markerer turneringen som afsluttet eller aktiv.
     * @param tournamentId ID på turneringen
     * @param isCompleted Om turneringen er afsluttet
     */
    suspend fun updateTournamentCompleted(tournamentId: String, isCompleted: Boolean)

    /**
     * Sletter en turnering og alle tilhørende spillere og kampe.
     * @param tournamentId ID på turneringen der skal slettes
     */
    suspend fun deleteTournament(tournamentId: String)

    // =====================================================
    // MATCH OPERATIONS
    // =====================================================

    /**
     * Opdaterer en kamp i databasen.
     * @param match Kampen der skal opdateres
     * @param tournamentId ID på turneringen
     */
    suspend fun updateMatch(match: Match, tournamentId: String)

    /**
     * Tilføjer nye kampe til en turnering.
     * @param matches Liste af kampe der skal tilføjes
     * @param tournamentId ID på turneringen
     */
    suspend fun insertMatches(matches: List<Match>, tournamentId: String)

    /**
     * Sletter alle kampe for en turnering.
     * @param tournamentId ID på turneringen
     */
    suspend fun deleteMatchesByTournament(tournamentId: String)

    /**
     * Tæller antal uafspillede kampe i en turnering.
     * @param tournamentId ID på turneringen
     * @return Antal uafspillede kampe
     */
    suspend fun countUnplayedMatches(tournamentId: String): Int

    /**
     * Tæller antal afspillede kampe i en turnering.
     * @param tournamentId ID på turneringen
     * @return Antal afspillede kampe
     */
    suspend fun countPlayedMatches(tournamentId: String): Int

    // =====================================================
    // PLAYER OPERATIONS
    // =====================================================

    /**
     * Opdaterer en spiller i databasen.
     * @param player Spilleren der skal opdateres
     * @param tournamentId ID på turneringen
     */
    suspend fun updatePlayer(player: Player, tournamentId: String)

    /**
     * Henter spillere for en turnering (til duplikering).
     * @param tournamentId ID på turneringen
     * @return Liste af spillere
     */
    suspend fun getPlayersForTournament(tournamentId: String): List<Player>
}
