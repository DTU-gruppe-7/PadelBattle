package dk.dtu.padelbattle

import dk.dtu.padelbattle.domain.model.Player
import dk.dtu.padelbattle.presentation.tournament.view.PlayerStanding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerStandingTest {

    // =====================================================
    // PLAYER STANDING CREATION TESTS
    // =====================================================

    @Test
    fun testPlayerStandingCreation() {
        val player = Player(name = "Test Player", totalPoints = 50)
        val standing = PlayerStanding(
            player = player,
            bonusPoints = 8,
            displayTotal = 58
        )
        
        assertEquals(player, standing.player)
        assertEquals(8, standing.bonusPoints)
        assertEquals(58, standing.displayTotal)
    }

    @Test
    fun testPlayerStandingWithNoBonus() {
        val player = Player(name = "Leader", totalPoints = 100, gamesPlayed = 5)
        val standing = PlayerStanding(
            player = player,
            bonusPoints = 0,
            displayTotal = 100
        )
        
        assertEquals(0, standing.bonusPoints)
        assertEquals(player.totalPoints, standing.displayTotal)
    }

    // =====================================================
    // BONUS BEREGNING TESTS - simulerer StandingsViewModel logik
    // =====================================================

    /**
     * Hjælpefunktion der simulerer bonus-beregningen fra StandingsViewModel.
     * Bonus = (maxKampe - spillerKampe) × (pointsPerMatch / 2)
     */
    private fun calculateBonus(
        playerGamesPlayed: Int,
        maxGamesPlayed: Int,
        pointsPerMatch: Int = 16
    ): Int {
        val drawPoints = pointsPerMatch / 2  // Half of pointsPerMatch
        val gamesBehind = maxGamesPlayed - playerGamesPlayed
        return gamesBehind * drawPoints
    }

    @Test
    fun testBonusCalculationPlayerOneGameBehind() {
        val maxGames = 5
        val playerGames = 4
        val pointsPerMatch = 16
        
        val bonus = calculateBonus(playerGames, maxGames, pointsPerMatch)
        
        // 1 game behind × 8 points = 8 bonus
        assertEquals(8, bonus)
    }

    @Test
    fun testBonusCalculationPlayerTwoGamesBehind() {
        val maxGames = 5
        val playerGames = 3
        val pointsPerMatch = 16
        
        val bonus = calculateBonus(playerGames, maxGames, pointsPerMatch)
        
        // 2 games behind × 8 points = 16 bonus
        assertEquals(16, bonus)
    }

    @Test
    fun testBonusCalculationPlayerNotBehind() {
        val maxGames = 5
        val playerGames = 5
        val pointsPerMatch = 16
        
        val bonus = calculateBonus(playerGames, maxGames, pointsPerMatch)
        
        // 0 games behind = 0 bonus
        assertEquals(0, bonus)
    }

    @Test
    fun testBonusCalculationWithDifferentPointsPerMatch() {
        val maxGames = 4
        val playerGames = 2
        val pointsPerMatch = 24  // Different scoring system
        
        val bonus = calculateBonus(playerGames, maxGames, pointsPerMatch)
        
        // 2 games behind × 12 points = 24 bonus
        assertEquals(24, bonus)
    }

    // =====================================================
    // SORTERING TESTS - simulerer standings sortering
    // =====================================================

    @Test
    fun testSortingByDisplayTotal() {
        val player1 = Player(name = "Player 1", totalPoints = 50, gamesPlayed = 5)
        val player2 = Player(name = "Player 2", totalPoints = 60, gamesPlayed = 5)
        val player3 = Player(name = "Player 3", totalPoints = 40, gamesPlayed = 4)
        
        val standings = listOf(
            PlayerStanding(player1, bonusPoints = 0, displayTotal = 50),
            PlayerStanding(player2, bonusPoints = 0, displayTotal = 60),
            PlayerStanding(player3, bonusPoints = 8, displayTotal = 48)  // 40 + 8 bonus
        ).sortedByDescending { it.displayTotal }
        
        assertEquals("Player 2", standings[0].player.name)  // 60 points
        assertEquals("Player 1", standings[1].player.name)  // 50 points
        assertEquals("Player 3", standings[2].player.name)  // 48 points (40+8)
    }

    @Test
    fun testSortingWithBonusAffectingOrder() {
        // Scenario: Player 3 har færre points men får bonus der løfter dem op
        val player1 = Player(name = "Leader", totalPoints = 80, gamesPlayed = 5)
        val player2 = Player(name = "Behind", totalPoints = 70, gamesPlayed = 4)
        
        // Player 2 er 1 game behind, får 8 bonus points (standard pointsPerMatch=16)
        val standings = listOf(
            PlayerStanding(player1, bonusPoints = 0, displayTotal = 80),
            PlayerStanding(player2, bonusPoints = 8, displayTotal = 78)  // 70 + 8
        ).sortedByDescending { it.displayTotal }
        
        // Leader is still first (80 > 78)
        assertEquals("Leader", standings[0].player.name)
        assertEquals("Behind", standings[1].player.name)
    }

    @Test
    fun testSortingWithBonusOvertakingLeader() {
        // Scenario: Player 2 har så god gennemsnit at bonus gør dem til #1
        val player1 = Player(name = "Current Leader", totalPoints = 75, gamesPlayed = 5)
        val player2 = Player(name = "Strong Average", totalPoints = 72, gamesPlayed = 4)
        
        // Player 2 får 8 bonus → 72 + 8 = 80 > 75
        val standings = listOf(
            PlayerStanding(player1, bonusPoints = 0, displayTotal = 75),
            PlayerStanding(player2, bonusPoints = 8, displayTotal = 80)
        ).sortedByDescending { it.displayTotal }
        
        // Strong Average overtager med bonus!
        assertEquals("Strong Average", standings[0].player.name)
        assertEquals("Current Leader", standings[1].player.name)
    }

    @Test
    fun testSortingTiebreakerByWins() {
        // Når displayTotal er ens, sorteres efter wins
        val player1 = Player(name = "More Wins", totalPoints = 50, gamesPlayed = 5, wins = 4)
        val player2 = Player(name = "Fewer Wins", totalPoints = 50, gamesPlayed = 5, wins = 2)
        
        val standings = listOf(
            PlayerStanding(player1, bonusPoints = 0, displayTotal = 50),
            PlayerStanding(player2, bonusPoints = 0, displayTotal = 50)
        ).sortedWith(
            compareByDescending<PlayerStanding> { it.displayTotal }
                .thenByDescending { it.player.wins }
        )
        
        assertEquals("More Wins", standings[0].player.name)
        assertEquals("Fewer Wins", standings[1].player.name)
    }

    // =====================================================
    // FULL SCENARIO TESTS
    // =====================================================

    @Test
    fun testFullStandingsScenario() {
        // Simulerer en runde hvor nogle spillere har spillet flere kampe
        val players = listOf(
            Player(name = "Alice", totalPoints = 42, gamesPlayed = 3, wins = 2),
            Player(name = "Bob", totalPoints = 35, gamesPlayed = 2, wins = 2),
            Player(name = "Charlie", totalPoints = 50, gamesPlayed = 3, wins = 3),
            Player(name = "Diana", totalPoints = 28, gamesPlayed = 2, wins = 1)
        )
        
        val maxGamesPlayed = players.maxOf { it.gamesPlayed }  // 3
        val pointsPerMatch = 16
        val drawPoints = pointsPerMatch / 2  // 8
        
        val standings = players.map { player ->
            val gamesBehind = maxGamesPlayed - player.gamesPlayed
            val bonus = gamesBehind * drawPoints
            PlayerStanding(
                player = player,
                bonusPoints = bonus,
                displayTotal = player.totalPoints + bonus
            )
        }.sortedWith(
            compareByDescending<PlayerStanding> { it.displayTotal }
                .thenByDescending { it.player.wins }
        )
        
        // Forventede resultater:
        // Charlie: 50 + 0 = 50 (3 games, 0 behind)
        // Bob: 35 + 8 = 43 (2 games, 1 behind)
        // Alice: 42 + 0 = 42 (3 games, 0 behind)
        // Diana: 28 + 8 = 36 (2 games, 1 behind)
        
        assertEquals("Charlie", standings[0].player.name)
        assertEquals(50, standings[0].displayTotal)
        assertEquals(0, standings[0].bonusPoints)
        
        assertEquals("Bob", standings[1].player.name)
        assertEquals(43, standings[1].displayTotal)
        assertEquals(8, standings[1].bonusPoints)
        
        assertEquals("Alice", standings[2].player.name)
        assertEquals(42, standings[2].displayTotal)
        
        assertEquals("Diana", standings[3].player.name)
        assertEquals(36, standings[3].displayTotal)
    }

    // =====================================================
    // EDGE CASE TESTS
    // =====================================================

    @Test
    fun testAllPlayersEqualGames() {
        // Når alle har spillet lige mange kampe, er der ingen bonus
        val players = listOf(
            Player(name = "P1", totalPoints = 50, gamesPlayed = 4),
            Player(name = "P2", totalPoints = 45, gamesPlayed = 4),
            Player(name = "P3", totalPoints = 40, gamesPlayed = 4)
        )
        
        val maxGames = players.maxOf { it.gamesPlayed }
        
        players.forEach { player ->
            val bonus = calculateBonus(player.gamesPlayed, maxGames, 16)
            assertEquals(0, bonus)
        }
    }

    @Test
    fun testZeroGamesPlayed() {
        val player = Player(name = "New Player", totalPoints = 0, gamesPlayed = 0)
        val maxGames = 3
        
        val bonus = calculateBonus(player.gamesPlayed, maxGames, 16)
        
        // 3 games behind × 8 = 24 bonus
        assertEquals(24, bonus)
    }
}
