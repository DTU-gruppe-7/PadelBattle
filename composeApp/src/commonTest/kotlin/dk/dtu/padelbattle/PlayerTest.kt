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
    // MUTABLE PROPERTY TESTS (simulates playing matches)
    // =====================================================

    @Test
    fun testPlayerStatisticsCanBeUpdated() {
        val player = Player(name = "Test")
        
        // Simulate winning a match
        player.wins++
        player.gamesPlayed++
        player.totalPoints += 21
        
        assertEquals(1, player.wins)
        assertEquals(1, player.gamesPlayed)
        assertEquals(21, player.totalPoints)
    }

    @Test
    fun testPlayerLossTracking() {
        val player = Player(name = "Test")
        
        // Simulate losing a match
        player.losses++
        player.gamesPlayed++
        player.totalPoints += 10
        
        assertEquals(1, player.losses)
        assertEquals(1, player.gamesPlayed)
        assertEquals(10, player.totalPoints)
    }

    @Test
    fun testPlayerDrawTracking() {
        val player = Player(name = "Test")
        
        // Simulate a draw
        player.draws++
        player.gamesPlayed++
        player.totalPoints += 15
        
        assertEquals(1, player.draws)
        assertEquals(1, player.gamesPlayed)
        assertEquals(15, player.totalPoints)
    }

    @Test
    fun testPlayerNameCanBeChanged() {
        val player = Player(name = "Original Name")
        
        player.name = "New Name"
        
        assertEquals("New Name", player.name)
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
        val player = Player(name = "Tournament Player")
        
        // Match 1: Win with 21 points
        player.wins++
        player.gamesPlayed++
        player.totalPoints += 21
        
        // Match 2: Loss with 15 points
        player.losses++
        player.gamesPlayed++
        player.totalPoints += 15
        
        // Match 3: Draw with 18 points
        player.draws++
        player.gamesPlayed++
        player.totalPoints += 18
        
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
