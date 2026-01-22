package dk.dtu.padelbattle.data.mapper

import dk.dtu.padelbattle.data.dao.MatchDao
import dk.dtu.padelbattle.data.dao.PlayerDao
import dk.dtu.padelbattle.data.dao.TournamentDao
import dk.dtu.padelbattle.data.entity.MatchEntity
import dk.dtu.padelbattle.data.entity.PlayerEntity
import dk.dtu.padelbattle.data.entity.TournamentEntity
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Konverterer en Tournament model til en TournamentEntity til database
 */
fun Tournament.toEntity(): TournamentEntity {
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

/**
 * Konverterer en Player model til en PlayerEntity til database
 * @param tournamentId ID på turneringen som spilleren tilhører
 */
fun Player.toEntity(tournamentId: String): PlayerEntity {
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

/**
 * Konverterer en Match model til en MatchEntity til database
 * @param tournamentId ID på turneringen som kampen tilhører
 */
fun Match.toEntity(tournamentId: String): MatchEntity {
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

/**
 * Konverterer en hel Tournament model med alle spillere og kampe til database entities
 */
data class TournamentWithRelations(
    val tournament: TournamentEntity,
    val players: List<PlayerEntity>,
    val matches: List<MatchEntity>
)

fun Tournament.toEntitiesWithRelations(): TournamentWithRelations {
    return TournamentWithRelations(
        tournament = this.toEntity(),
        players = this.players.map { it.toEntity(this.id) },
        matches = this.matches.map { it.toEntity(this.id) }
    )
}

// --- EXTENSION FUNCTIONS TIL KONVERTERING FRA ENTITIES TIL DOMAIN MODELS ---

/**
 * Konverterer PlayerEntity til Player domain model.
 */
fun PlayerEntity.toPlayer(): Player {
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

/**
 * Konverterer MatchEntity til Match domain model.
 * @param playerMap Map af player IDs til Player objekter
 */
fun MatchEntity.toMatch(playerMap: Map<String, Player>): Match {
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

/**
 * Konverterer TournamentEntity + relaterede entities til domain model Tournament.
 */
fun TournamentEntity.toTournament(
    playerEntities: List<PlayerEntity>,
    matchEntities: List<MatchEntity>
): Tournament {
    val players = playerEntities.map { it.toPlayer() }.toMutableList()
    val playerMap = players.associateBy { it.id }

    val matches = matchEntities.map { matchEntity ->
        matchEntity.toMatch(playerMap)
    }.toMutableList()

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

// --- DATABASE OPERATIONS ---

/**
 * Henter alle turneringer som fulde Tournament objekter med spillere og kampe.
 */
fun TournamentDao.getAllTournamentsWithDetails(
    playerDao: PlayerDao,
    matchDao: MatchDao
): Flow<List<Tournament>> {
    return this.getAllTournaments().map { tournamentEntities ->
        tournamentEntities.map { entity ->
            loadFullTournamentFromDao(entity.id, this, playerDao, matchDao)
        }
    }
}

/**
 * Loader en komplet Tournament fra databasen med alle spillere og kampe.
 */
suspend fun loadFullTournamentFromDao(
    tournamentId: String,
    tournamentDao: TournamentDao,
    playerDao: PlayerDao,
    matchDao: MatchDao
): Tournament {
    val tournamentEntity = tournamentDao.getTournamentById(tournamentId)
        ?: throw IllegalStateException("Tournament not found: $tournamentId")

    val playerEntities = playerDao.getPlayersByTournamentOnce(tournamentId)
    val matchEntities = matchDao.getMatchesByTournamentOnce(tournamentId)

    return tournamentEntity.toTournament(playerEntities, matchEntities)
}

/**
 * Sletter en turnering (cascade sletter automatisk spillere og kampe).
 */
suspend fun deleteTournamentFromDao(
    tournamentId: String,
    tournamentDao: TournamentDao
) {
    tournamentDao.deleteTournamentById(tournamentId)
}
