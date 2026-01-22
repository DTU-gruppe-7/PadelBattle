package dk.dtu.padelbattle

import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.MatchResult
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.utils.MatchResultService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Unit tests for MatchResultService.
 * Uses a FakeRepository to isolate the service logic from database operations.
 */
class MatchResultServiceTest {

    // =====================================================
    // FAKE REPOSITORY FOR TESTING
    // =====================================================

    private class FakeRepository : TournamentRepository {
        val updatedMatches = mutableListOf<Pair<Match, String>>()
        val updatedPlayers = mutableListOf<Pair<Player, String>>()

        override fun getAllTournaments(): Flow<List<Tournament>> = flowOf(emptyList())
        override suspend fun getTournamentById(tournamentId: String): Tournament? = null
        override suspend fun saveTournament(tournament: Tournament) {}
        override suspend fun updateTournamentName(tournamentId: String, newName: String) {}
        override suspend fun updateNumberOfCourts(tournamentId: String, numberOfCourts: Int) {}
        override suspend fun updatePointsPerMatch(tournamentId: String, pointsPerMatch: Int) {}
        override suspend fun updateTournamentCompleted(tournamentId: String, isCompleted: Boolean) {}
        override suspend fun deleteTournament(tournamentId: String) {}
        override suspend fun registerExtension(tournamentId: String) {}
        override suspend fun decrementExtensionRounds(tournamentId: String): Boolean = true
        override suspend fun clearExtensionTracking(tournamentId: String) {}
        
        override suspend fun updateMatch(match: Match, tournamentId: String) {
            updatedMatches.add(match to tournamentId)
        }
        
        override suspend fun insertMatches(matches: List<Match>, tournamentId: String) {}
        override suspend fun deleteMatchesByTournament(tournamentId: String) {}
        override suspend fun countUnplayedMatches(tournamentId: String): Int = 0
        override suspend fun countPlayedMatches(tournamentId: String): Int = 0
        
        override suspend fun updatePlayer(player: Player, tournamentId: String) {
            updatedPlayers.add(player to tournamentId)
        }
        
        override suspend fun getPlayersForTournament(tournamentId: String): List<Player> = emptyList()
    }

    // =====================================================
    // TEST HELPERS
    // =====================================================

    private fun createTestPlayers(): List<Player> {
        return listOf(
            Player(id = "p1", name = "Player 1"),
            Player(id = "p2", name = "Player 2"),
            Player(id = "p3", name = "Player 3"),
            Player(id = "p4", name = "Player 4")
        )
    }

    private fun createTestMatch(players: List<Player> = createTestPlayers()): Match {
        return Match(
            id = "match1",
            roundNumber = 1,
            courtNumber = 1,
            team1Player1 = players[0],
            team1Player2 = players[1],
            team2Player1 = players[2],
            team2Player2 = players[3]
        )
    }

    // =====================================================
    // BASIC FUNCTIONALITY TESTS
    // =====================================================

    @Test
    fun testRecordMatchResult_Team1Wins() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)

        service.recordMatchResult(match, result, "tournament1")

        // Verify match was updated
        assertEquals(1, repository.updatedMatches.size)
        val updatedMatch = repository.updatedMatches[0].first
        assertEquals(21, updatedMatch.scoreTeam1)
        assertEquals(15, updatedMatch.scoreTeam2)
        assertTrue(updatedMatch.isPlayed)

        // Verify all 4 players were updated
        assertEquals(4, repository.updatedPlayers.size)
    }

    @Test
    fun testRecordMatchResult_Team2Wins() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 10, scoreTeam2 = 21)

        service.recordMatchResult(match, result, "tournament1")

        val updatedMatch = repository.updatedMatches[0].first
        assertEquals(10, updatedMatch.scoreTeam1)
        assertEquals(21, updatedMatch.scoreTeam2)
    }

    @Test
    fun testRecordMatchResult_Draw() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 15, scoreTeam2 = 15)

        service.recordMatchResult(match, result, "tournament1")

        val updatedMatch = repository.updatedMatches[0].first
        assertEquals(15, updatedMatch.scoreTeam1)
        assertEquals(15, updatedMatch.scoreTeam2)
    }

    // =====================================================
    // PLAYER STATISTICS TESTS
    // =====================================================

    @Test
    fun testPlayerStatistics_Team1Wins() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)

        service.recordMatchResult(match, result, "tournament1")

        // Find updated players by ID
        val player1 = repository.updatedPlayers.find { it.first.id == "p1" }!!.first
        val player2 = repository.updatedPlayers.find { it.first.id == "p2" }!!.first
        val player3 = repository.updatedPlayers.find { it.first.id == "p3" }!!.first
        val player4 = repository.updatedPlayers.find { it.first.id == "p4" }!!.first

        // Team 1 (winners) should have: 21 points, 1 win, 1 game played
        assertEquals(21, player1.totalPoints)
        assertEquals(1, player1.wins)
        assertEquals(0, player1.losses)
        assertEquals(1, player1.gamesPlayed)

        assertEquals(21, player2.totalPoints)
        assertEquals(1, player2.wins)
        assertEquals(0, player2.losses)
        assertEquals(1, player2.gamesPlayed)

        // Team 2 (losers) should have: 15 points, 1 loss, 1 game played
        assertEquals(15, player3.totalPoints)
        assertEquals(0, player3.wins)
        assertEquals(1, player3.losses)
        assertEquals(1, player3.gamesPlayed)

        assertEquals(15, player4.totalPoints)
        assertEquals(0, player4.wins)
        assertEquals(1, player4.losses)
        assertEquals(1, player4.gamesPlayed)
    }

    @Test
    fun testPlayerStatistics_Draw() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 15, scoreTeam2 = 15)

        service.recordMatchResult(match, result, "tournament1")

        // All players should have 1 draw
        repository.updatedPlayers.forEach { (player, _) ->
            assertEquals(1, player.draws)
            assertEquals(0, player.wins)
            assertEquals(0, player.losses)
            assertEquals(1, player.gamesPlayed)
        }
    }

    // =====================================================
    // RE-RECORDING (REVERT + APPLY) TESTS
    // =====================================================

    @Test
    fun testReRecordMatchResult_RevertsOldStatistics() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        
        // Create a match that was already played
        val players = listOf(
            Player(id = "p1", name = "Player 1", totalPoints = 21, wins = 1, gamesPlayed = 1),
            Player(id = "p2", name = "Player 2", totalPoints = 21, wins = 1, gamesPlayed = 1),
            Player(id = "p3", name = "Player 3", totalPoints = 15, losses = 1, gamesPlayed = 1),
            Player(id = "p4", name = "Player 4", totalPoints = 15, losses = 1, gamesPlayed = 1)
        )
        val playedMatch = Match(
            id = "match1",
            roundNumber = 1,
            courtNumber = 1,
            team1Player1 = players[0],
            team1Player2 = players[1],
            team2Player1 = players[2],
            team2Player2 = players[3],
            scoreTeam1 = 21,
            scoreTeam2 = 15,
            isPlayed = true
        )

        // Re-record with new result (Team 2 wins now)
        val newResult = MatchResult(scoreTeam1 = 10, scoreTeam2 = 25)
        service.recordMatchResult(playedMatch, newResult, "tournament1")

        // Find updated players
        val player1 = repository.updatedPlayers.find { it.first.id == "p1" }!!.first
        val player3 = repository.updatedPlayers.find { it.first.id == "p3" }!!.first

        // Player 1: Reverted (21-21=0 points, 1-1=0 wins) then applied (0+10=10 points, 0+1=1 loss)
        assertEquals(10, player1.totalPoints)
        assertEquals(0, player1.wins)
        assertEquals(1, player1.losses)

        // Player 3: Reverted (15-15=0 points, 1-1=0 losses) then applied (0+25=25 points, 0+1=1 win)
        assertEquals(25, player3.totalPoints)
        assertEquals(1, player3.wins)
        assertEquals(0, player3.losses)
    }

    // =====================================================
    // VALIDATION TESTS
    // =====================================================

    @Test
    fun testRecordMatchResult_ThrowsOnBlankTournamentId() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)

        assertFailsWith<IllegalArgumentException> {
            service.recordMatchResult(match, result, "")
        }

        assertFailsWith<IllegalArgumentException> {
            service.recordMatchResult(match, result, "   ")
        }
    }

    @Test
    fun testRecordMatchResult_UsesCorrectTournamentId() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)
        val tournamentId = "my-tournament-123"

        service.recordMatchResult(match, result, tournamentId)

        // Verify all updates used the correct tournament ID
        repository.updatedMatches.forEach { (_, id) ->
            assertEquals(tournamentId, id)
        }
        repository.updatedPlayers.forEach { (_, id) ->
            assertEquals(tournamentId, id)
        }
    }

    // =====================================================
    // EDGE CASE TESTS
    // =====================================================

    @Test
    fun testRecordMatchResult_ZeroZeroScore() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 0, scoreTeam2 = 0)

        service.recordMatchResult(match, result, "tournament1")

        val updatedMatch = repository.updatedMatches[0].first
        assertEquals(0, updatedMatch.scoreTeam1)
        assertEquals(0, updatedMatch.scoreTeam2)

        // All players should have 0 points but 1 draw
        repository.updatedPlayers.forEach { (player, _) ->
            assertEquals(0, player.totalPoints)
            assertEquals(1, player.draws)
        }
    }

    @Test
    fun testRecordMatchResult_HighScores() = runTest {
        val repository = FakeRepository()
        val service = MatchResultService(repository)
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 100, scoreTeam2 = 50)

        service.recordMatchResult(match, result, "tournament1")

        val player1 = repository.updatedPlayers.find { it.first.id == "p1" }!!.first
        assertEquals(100, player1.totalPoints)
    }
}
