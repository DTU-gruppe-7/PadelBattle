package dk.dtu.padelbattle

import dk.dtu.padelbattle.model.MatchOutcome
import dk.dtu.padelbattle.model.MatchResult
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Match
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MatchResultTest {

    // =====================================================
    // MATCHRESULT DATA CLASS TESTS
    // =====================================================

    @Test
    fun testMatchResultCreation() {
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)
        
        assertEquals(21, result.scoreTeam1)
        assertEquals(15, result.scoreTeam2)
    }

    @Test
    fun testMatchResultWithZeroScores() {
        val result = MatchResult(scoreTeam1 = 0, scoreTeam2 = 0)
        
        assertEquals(0, result.scoreTeam1)
        assertEquals(0, result.scoreTeam2)
    }

    @Test
    fun testMatchResultEquality() {
        val result1 = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)
        val result2 = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)
        val result3 = MatchResult(scoreTeam1 = 15, scoreTeam2 = 21)
        
        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
        assertNotEquals(result1, result3)
    }

    // =====================================================
    // MATCH OUTCOME TESTS - TEAM 1 WINS
    // =====================================================

    @Test
    fun testGetOutcomeTeam1WinsByClearMargin() {
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)
        
        assertEquals(MatchOutcome.TEAM1_WIN, result.getOutcome())
    }

    @Test
    fun testGetOutcomeTeam1WinsByOnePoint() {
        val result = MatchResult(scoreTeam1 = 16, scoreTeam2 = 15)
        
        assertEquals(MatchOutcome.TEAM1_WIN, result.getOutcome())
    }

    @Test
    fun testGetOutcomeTeam1WinsWithHighScores() {
        val result = MatchResult(scoreTeam1 = 100, scoreTeam2 = 99)
        
        assertEquals(MatchOutcome.TEAM1_WIN, result.getOutcome())
    }

    @Test
    fun testGetOutcomeTeam1WinsWithZeroAgainst() {
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 0)
        
        assertEquals(MatchOutcome.TEAM1_WIN, result.getOutcome())
    }

    // =====================================================
    // MATCH OUTCOME TESTS - TEAM 2 WINS
    // =====================================================

    @Test
    fun testGetOutcomeTeam2WinsByClearMargin() {
        val result = MatchResult(scoreTeam1 = 10, scoreTeam2 = 21)
        
        assertEquals(MatchOutcome.TEAM2_WIN, result.getOutcome())
    }

    @Test
    fun testGetOutcomeTeam2WinsByOnePoint() {
        val result = MatchResult(scoreTeam1 = 14, scoreTeam2 = 15)
        
        assertEquals(MatchOutcome.TEAM2_WIN, result.getOutcome())
    }

    @Test
    fun testGetOutcomeTeam2WinsWithHighScores() {
        val result = MatchResult(scoreTeam1 = 50, scoreTeam2 = 51)
        
        assertEquals(MatchOutcome.TEAM2_WIN, result.getOutcome())
    }

    @Test
    fun testGetOutcomeTeam2WinsWithZeroAgainst() {
        val result = MatchResult(scoreTeam1 = 0, scoreTeam2 = 21)
        
        assertEquals(MatchOutcome.TEAM2_WIN, result.getOutcome())
    }

    // =====================================================
    // MATCH OUTCOME TESTS - DRAW
    // =====================================================

    @Test
    fun testGetOutcomeDrawWithZeroZero() {
        val result = MatchResult(scoreTeam1 = 0, scoreTeam2 = 0)
        
        assertEquals(MatchOutcome.DRAW, result.getOutcome())
    }

    @Test
    fun testGetOutcomeDrawWithEqualScores() {
        val result = MatchResult(scoreTeam1 = 15, scoreTeam2 = 15)
        
        assertEquals(MatchOutcome.DRAW, result.getOutcome())
    }

    @Test
    fun testGetOutcomeDrawWithHighEqualScores() {
        val result = MatchResult(scoreTeam1 = 100, scoreTeam2 = 100)
        
        assertEquals(MatchOutcome.DRAW, result.getOutcome())
    }

    // =====================================================
    // MATCH OUTCOME ENUM TESTS
    // =====================================================

    @Test
    fun testMatchOutcomeEnumValues() {
        val outcomes = MatchOutcome.entries
        
        assertEquals(3, outcomes.size)
        assertTrue(MatchOutcome.TEAM1_WIN in outcomes)
        assertTrue(MatchOutcome.TEAM2_WIN in outcomes)
        assertTrue(MatchOutcome.DRAW in outcomes)
    }

    // =====================================================
    // MATCH INTEGRATION TESTS (using MatchResult with Match)
    // =====================================================

    private fun createTestPlayers(): List<Player> {
        return (1..4).map { Player(name = "Player $it") }
    }

    private fun createTestMatch(): Match {
        val players = createTestPlayers()
        return Match(
            roundNumber = 1,
            courtNumber = 1,
            team1Player1 = players[0],
            team1Player2 = players[1],
            team2Player1 = players[2],
            team2Player2 = players[3]
        )
    }

    @Test
    fun testMatchResultFromMatchScores() {
        val players = createTestPlayers()
        val match = Match(
            roundNumber = 1,
            courtNumber = 1,
            team1Player1 = players[0],
            team1Player2 = players[1],
            team2Player1 = players[2],
            team2Player2 = players[3],
            scoreTeam1 = 21,
            scoreTeam2 = 18
        )
        
        val result = MatchResult(match.scoreTeam1, match.scoreTeam2)
        
        assertEquals(21, result.scoreTeam1)
        assertEquals(18, result.scoreTeam2)
        assertEquals(MatchOutcome.TEAM1_WIN, result.getOutcome())
    }

    @Test
    fun testMatchResultAppliedToMatch() {
        val match = createTestMatch()
        val result = MatchResult(scoreTeam1 = 15, scoreTeam2 = 21)
        
        // Use copy() for immutable Match
        val updatedMatch = match.copy(
            scoreTeam1 = result.scoreTeam1,
            scoreTeam2 = result.scoreTeam2,
            isPlayed = true
        )
        
        assertEquals(15, updatedMatch.scoreTeam1)
        assertEquals(21, updatedMatch.scoreTeam2)
        assertEquals(true, updatedMatch.isPlayed)
    }

    // =====================================================
    // EDGE CASE TESTS
    // =====================================================

    @Test
    fun testMatchResultWithNegativeScores() {
        // While negative scores are not realistic, the data class doesn't prevent them
        val result = MatchResult(scoreTeam1 = -5, scoreTeam2 = -3)
        
        // Higher (less negative) score wins
        assertEquals(MatchOutcome.TEAM2_WIN, result.getOutcome())
    }

    @Test
    fun testMatchResultCopy() {
        val original = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)
        val copied = original.copy()
        val modified = original.copy(scoreTeam1 = 25)
        
        assertEquals(original, copied)
        assertEquals(25, modified.scoreTeam1)
        assertEquals(15, modified.scoreTeam2)
    }

    @Test
    fun testMatchResultToString() {
        val result = MatchResult(scoreTeam1 = 21, scoreTeam2 = 15)
        val string = result.toString()
        
        assertTrue(string.contains("21"))
        assertTrue(string.contains("15"))
    }
}
