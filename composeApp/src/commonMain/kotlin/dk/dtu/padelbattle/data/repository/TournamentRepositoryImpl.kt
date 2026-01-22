package dk.dtu.padelbattle.data.repository

import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.entity.MatchEntity
import dk.dtu.padelbattle.data.entity.PlayerEntity
import dk.dtu.padelbattle.data.entity.TournamentEntity
import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.Player
import dk.dtu.padelbattle.domain.model.Tournament
import dk.dtu.padelbattle.domain.model.TournamentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of TournamentRepository.
 * Coordinates access to Tournament, Player, and Match DAOs.
 */
class TournamentRepositoryImpl(
    private val tournamentDao: TournamentDao,
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao
) : TournamentRepository {

    // =====================================================
    // TOURNAMENT OPERATIONS
    // =====================================================

    override fun getAllTournaments(): Flow<List<Tournament>> {
        return tournamentDao.getAllTournaments().map { tournamentEntities ->
            tournamentEntities.map { entity ->
                loadTournamentWithDetails(entity.id)
                    ?: throw IllegalStateException("Tournament not found: ${entity.id}")
            }
        }
    }

    override suspend fun getTournamentById(tournamentId: String): Tournament? {
        return loadTournamentWithDetails(tournamentId)
    }

    override suspend fun saveTournament(tournament: Tournament) {
        // Gem turnering
        tournamentDao.insertTournament(tournament.toEntity())
        
        // Gem spillere
        val playerEntities = tournament.players.map { it.toEntity(tournament.id) }
        playerDao.insertPlayers(playerEntities)
        
        // Gem kampe
        val matchEntities = tournament.matches.map { it.toEntity(tournament.id) }
        matchDao.insertMatches(matchEntities)
    }

    override suspend fun updateTournamentName(tournamentId: String, newName: String) {
        tournamentDao.updateTournamentName(tournamentId, newName)
    }

    override suspend fun updateNumberOfCourts(tournamentId: String, numberOfCourts: Int) {
        tournamentDao.updateNumberOfCourts(tournamentId, numberOfCourts)
    }

    override suspend fun updatePointsPerMatch(tournamentId: String, pointsPerMatch: Int) {
        tournamentDao.updatePointsPerMatch(tournamentId, pointsPerMatch)
    }

    override suspend fun updateTournamentCompleted(tournamentId: String, isCompleted: Boolean) {
        tournamentDao.updateTournamentCompleted(tournamentId, isCompleted)
    }

    override suspend fun deleteTournament(tournamentId: String) {
        tournamentDao.deleteTournamentById(tournamentId)
    }

    // =====================================================
    // MEXICANO EXTENSION TRACKING
    // =====================================================

    override suspend fun registerExtension(tournamentId: String) {
        tournamentDao.updateExtensionRoundsRemaining(tournamentId, 2)
    }

    override suspend fun decrementExtensionRounds(tournamentId: String): Boolean {
        val current = tournamentDao.getExtensionRoundsRemaining(tournamentId) ?: 0
        if (current > 0) {
            val newValue = current - 1
            tournamentDao.updateExtensionRoundsRemaining(tournamentId, newValue)
            return newValue <= 0
        }
        return true // Ingen udvidelse registreret, kan afsluttes
    }

    override suspend fun clearExtensionTracking(tournamentId: String) {
        tournamentDao.updateExtensionRoundsRemaining(tournamentId, 0)
    }

    // =====================================================
    // MATCH OPERATIONS
    // =====================================================

    override suspend fun updateMatch(match: Match, tournamentId: String) {
        matchDao.updateMatch(match.toEntity(tournamentId))
    }

    override suspend fun insertMatches(matches: List<Match>, tournamentId: String) {
        val matchEntities = matches.map { it.toEntity(tournamentId) }
        matchDao.insertMatches(matchEntities)
    }

    override suspend fun deleteMatchesByTournament(tournamentId: String) {
        matchDao.deleteMatchesByTournament(tournamentId)
    }

    override suspend fun countUnplayedMatches(tournamentId: String): Int {
        return matchDao.countUnplayedMatches(tournamentId)
    }

    override suspend fun countPlayedMatches(tournamentId: String): Int {
        return matchDao.countPlayedMatches(tournamentId)
    }

    // =====================================================
    // PLAYER OPERATIONS
    // =====================================================

    override suspend fun updatePlayer(player: Player, tournamentId: String) {
        playerDao.updatePlayer(player.toEntity(tournamentId))
    }

    override suspend fun getPlayersForTournament(tournamentId: String): List<Player> {
        return playerDao.getPlayersForTournament(tournamentId).map { it.toPlayer() }
    }

    // =====================================================
    // PRIVATE HELPERS - Entity Mapping
    // =====================================================

    /**
     * Loader en komplet Tournament fra databasen med alle spillere og kampe.
     */
    private suspend fun loadTournamentWithDetails(tournamentId: String): Tournament? {
        val tournamentEntity = tournamentDao.getTournamentById(tournamentId) ?: return null
        val playerEntities = playerDao.getPlayersByTournamentOnce(tournamentId)
        val matchEntities = matchDao.getMatchesByTournamentOnce(tournamentId)

        return tournamentEntity.toTournament(playerEntities, matchEntities)
    }

    // --- Entity to Domain Model Mappers ---

    private fun TournamentEntity.toTournament(
        playerEntities: List<PlayerEntity>,
        matchEntities: List<MatchEntity>
    ): Tournament {
        val players = playerEntities.map { it.toPlayer() }
        val playerMap = players.associateBy { it.id }

        val matches = matchEntities.map { matchEntity ->
            matchEntity.toMatch(playerMap)
        }

        return Tournament(
            id = this.id,
            name = this.name,
            type = TournamentType.valueOf(this.type),
            dateCreated = this.dateCreated,
            numberOfCourts = this.numberOfCourts,
            pointsPerMatch = this.pointsPerMatch,
            players = players,
            matches = matches,
            isCompleted = this.isCompleted
        )
    }

    private fun PlayerEntity.toPlayer(): Player {
        return Player(
            id = this.id,
            name = this.name,
            totalPoints = this.totalPoints,
            gamesPlayed = this.gamesPlayed,
            wins = this.wins,
            losses = this.losses,
            draws = this.draws
        )
    }

    private fun MatchEntity.toMatch(playerMap: Map<String, Player>): Match {
        return Match(
            id = this.id,
            roundNumber = this.roundNumber,
            courtNumber = this.courtNumber,
            team1Player1 = playerMap[this.team1Player1Id]
                ?: throw IllegalStateException("Player not found: ${this.team1Player1Id}"),
            team1Player2 = playerMap[this.team1Player2Id]
                ?: throw IllegalStateException("Player not found: ${this.team1Player2Id}"),
            team2Player1 = playerMap[this.team2Player1Id]
                ?: throw IllegalStateException("Player not found: ${this.team2Player1Id}"),
            team2Player2 = playerMap[this.team2Player2Id]
                ?: throw IllegalStateException("Player not found: ${this.team2Player2Id}"),
            scoreTeam1 = this.scoreTeam1,
            scoreTeam2 = this.scoreTeam2,
            isPlayed = this.isPlayed
        )
    }

    // --- Domain Model to Entity Mappers ---

    private fun Tournament.toEntity(): TournamentEntity {
        return TournamentEntity(
            id = this.id,
            name = this.name,
            type = this.type.name,
            dateCreated = this.dateCreated,
            numberOfCourts = this.numberOfCourts,
            pointsPerMatch = this.pointsPerMatch,
            isCompleted = this.isCompleted
        )
    }

    private fun Player.toEntity(tournamentId: String): PlayerEntity {
        return PlayerEntity(
            id = this.id,
            tournamentId = tournamentId,
            name = this.name,
            totalPoints = this.totalPoints,
            gamesPlayed = this.gamesPlayed,
            wins = this.wins,
            losses = this.losses,
            draws = this.draws
        )
    }

    private fun Match.toEntity(tournamentId: String): MatchEntity {
        return MatchEntity(
            id = this.id,
            tournamentId = tournamentId,
            roundNumber = this.roundNumber,
            courtNumber = this.courtNumber,
            team1Player1Id = this.team1Player1.id,
            team1Player2Id = this.team1Player2.id,
            team2Player1Id = this.team2Player1.id,
            team2Player2Id = this.team2Player2.id,
            scoreTeam1 = this.scoreTeam1,
            scoreTeam2 = this.scoreTeam2,
            isPlayed = this.isPlayed
        )
    }
}
