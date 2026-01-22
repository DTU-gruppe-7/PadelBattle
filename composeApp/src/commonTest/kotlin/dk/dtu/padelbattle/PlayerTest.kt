package dk.dtu.padelbattle

import dk.dtu.padelbattle.model.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PlayerTest {

    // =====================================================
    // PLAYER CREATION TESTS
    // =====================================================

    @Test
    fun testPlayerCreationWithName() {
        val player = Player(name = "Anders")
        
        assertEquals("Anders", player.name)
        assertTrue(player.id.isNotBlank())
    }

    @Test
    fun testPlayerDefaultValues() {
        val player = Player(name = "Test Player")
        
        assertEquals(0, player.totalPoints)
        assertEquals(0, player.gamesPlayed)
        assertEquals(0, player.wins)
        assertEquals(0, player.losses)
        assertEquals(0, player.draws)
    }

    @Test
    fun testPlayerWithCustomValues() {
        val player = Player(
            name = "Pro Player",
            totalPoints = 100,
            gamesPlayed = 5,
            wins = 3,
            losses = 1,
            draws = 1
        )
        
        assertEquals("Pro Player", player.name)
        assertEquals(100, player.totalPoints)
        assertEquals(5, player.gamesPlayed)
        assertEquals(3, player.wins)
        assertEquals(1, player.losses)
        assertEquals(1, player.draws)
    }

    // =====================================================
    // UNIQUE ID TESTS
    // =====================================================

    @Test
    fun testPlayersHaveUniqueIds() {
        val player1 = Player(name = "Player 1")
        val player2 = Player(name = "Player 2")
        
        assertNotEquals(player1.id, player2.id)
    }

    @Test
    fun testMultiplePlayersHaveUniqueIds() {
        val players = (1..10).map { Player(name = "Player $it") }
        val uniqueIds = players.map { it.id }.toSet()
        
        assertEquals(10, uniqueIds.size)
    }

    // =====================================================
    // IMMUTABLE PROPERTY TESTS (using copy() to update)
    // =====================================================

    @Test
    fun testPlayerStatisticsCanBeUpdated() {
        val player = Player(name = "Test")
        
        // Simulate winning a match using copy()
        val updatedPlayer = player.copy(
            wins = player.wins + 1,
            gamesPlayed = player.gamesPlayed + 1,
            totalPoints = player.totalPoints + 21
        )
        
        assertEquals(1, updatedPlayer.wins)
        assertEquals(1, updatedPlayer.gamesPlayed)
        assertEquals(21, updatedPlayer.totalPoints)
        
        // Original player should be unchanged (immutability)
        assertEquals(0, player.wins)
    }

    @Test
    fun testPlayerLossTracking() {
        val player = Player(name = "Test")
        
        // Simulate losing a match using copy()
        val updatedPlayer = player.copy(
            losses = player.losses + 1,
            gamesPlayed = player.gamesPlayed + 1,
            totalPoints = player.totalPoints + 10
        )
        
        assertEquals(1, updatedPlayer.losses)
        assertEquals(1, updatedPlayer.gamesPlayed)
        assertEquals(10, updatedPlayer.totalPoints)
    }

    @Test
    fun testPlayerDrawTracking() {
        val player = Player(name = "Test")
        
        // Simulate a draw using copy()
        val updatedPlayer = player.copy(
            draws = player.draws + 1,
            gamesPlayed = player.gamesPlayed + 1,
            totalPoints = player.totalPoints + 15
        )
        
        assertEquals(1, updatedPlayer.draws)
        assertEquals(1, updatedPlayer.gamesPlayed)
        assertEquals(15, updatedPlayer.totalPoints)
    }

    @Test
    fun testPlayerNameCanBeChanged() {
        val player = Player(name = "Original Name")
        
        val updatedPlayer = player.copy(name = "New Name")
        
        assertEquals("New Name", updatedPlayer.name)
        // Original unchanged
        assertEquals("Original Name", player.name)
    }

    // =====================================================
    // DATA CLASS FUNCTIONALITY TESTS
    // =====================================================

    @Test
    fun testPlayerCopy() {
        val original = Player(name = "Original", totalPoints = 50, wins = 3)
        val copied = original.copy()
        val modified = original.copy(name = "Modified")
        
        assertEquals(original.id, copied.id)
        assertEquals(original.totalPoints, copied.totalPoints)
        assertEquals("Modified", modified.name)
        assertEquals(original.totalPoints, modified.totalPoints)
    }

    @Test
    fun testPlayerToString() {
        val player = Player(name = "Test Player")
        val string = player.toString()
        
        assertTrue(string.contains("Test Player"))
        assertTrue(string.contains("Player"))
    }

    // =====================================================
    // REALISTIC GAME SCENARIO TESTS
    // =====================================================

    @Test
    fun testPlayerAfterMultipleMatches() {
        var player = Player(name = "Tournament Player")
        
        // Match 1: Win with 21 points
        player = player.copy(
            wins = player.wins + 1,
            gamesPlayed = player.gamesPlayed + 1,
            totalPoints = player.totalPoints + 21
        )
        
        // Match 2: Loss with 15 points
        player = player.copy(
            losses = player.losses + 1,
            gamesPlayed = player.gamesPlayed + 1,
            totalPoints = player.totalPoints + 15
        )
        
        // Match 3: Draw with 18 points
        player = player.copy(
            draws = player.draws + 1,
            gamesPlayed = player.gamesPlayed + 1,
            totalPoints = player.totalPoints + 18
        )
        
        assertEquals(3, player.gamesPlayed)
        assertEquals(1, player.wins)
        assertEquals(1, player.losses)
        assertEquals(1, player.draws)
        assertEquals(54, player.totalPoints) // 21 + 15 + 18
    }

    @Test
    fun testWinRateCalculation() {
        val player = Player(
            name = "Test",
            gamesPlayed = 10,
            wins = 7,
            losses = 2,
            draws = 1
        )
        
        // Win rate can be calculated from stats
        val winRate = if (player.gamesPlayed > 0) {
            player.wins.toDouble() / player.gamesPlayed
        } else 0.0
        
        assertEquals(0.7, winRate, 0.01)
    }
}
