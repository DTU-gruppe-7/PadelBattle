package dk.dtu.padelbattle

import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.Player
import dk.dtu.padelbattle.model.Tournament
import dk.dtu.padelbattle.model.TournamentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TournamentTest {

    // Helper function to create players
    private fun createPlayers(count: Int): MutableList<Player> {
        return (1..count).map { Player(name = "Player $it") }.toMutableList()
    }

    // Helper function to create a default Americano tournament
    private fun createAmericanoTournament(
        playerCount: Int,
        numberOfCourts: Int = 1
    ): Tournament {
        return Tournament(
            name = "Test Tournament",
            type = TournamentType.AMERICANO,
            dateCreated = 0L,
            numberOfCourts = numberOfCourts,
            players = createPlayers(playerCount)
        )
    }

    // =====================================================
    // TOURNAMENT CREATION TESTS
    // =====================================================

    @Test
    fun testTournamentCreation() {
        val players = createPlayers(8)
        val tournament = Tournament(
            name = "Test Tournament",
            type = TournamentType.AMERICANO,
            dateCreated = 1234567890L,
            numberOfCourts = 2,
            players = players
        )

        assertEquals("Test Tournament", tournament.name)
        assertEquals(TournamentType.AMERICANO, tournament.type)
        assertEquals(1234567890L, tournament.dateCreated)
        assertEquals(2, tournament.numberOfCourts)
        assertEquals(8, tournament.players.size)
        assertTrue(tournament.matches.isEmpty())
    }

    @Test
    fun testTournamentDefaultValues() {
        val tournament = Tournament(
            name = "Default Test",
            type = TournamentType.AMERICANO,
            dateCreated = 0L
        )

        assertEquals(1, tournament.numberOfCourts)
        assertTrue(tournament.players.isEmpty())
        assertTrue(tournament.matches.isEmpty())
    }

    @Test
    fun testTournamentHasUniqueId() {
        val tournament1 = Tournament(
            name = "Tournament 1",
            type = TournamentType.AMERICANO,
            dateCreated = 0L
        )
        val tournament2 = Tournament(
            name = "Tournament 2",
            type = TournamentType.AMERICANO,
            dateCreated = 0L
        )

        assertTrue(tournament1.id.isNotEmpty())
        assertTrue(tournament2.id.isNotEmpty())
        assertTrue(tournament1.id != tournament2.id)
    }

    // =====================================================
    // COURT CALCULATION TESTS
    // =====================================================

    @Test
    fun testGetMaxCourts() {
        assertEquals(1, createAmericanoTournament(4).getMaxCourts())
        assertEquals(1, createAmericanoTournament(5).getMaxCourts())
        assertEquals(1, createAmericanoTournament(6).getMaxCourts())
        assertEquals(1, createAmericanoTournament(7).getMaxCourts())
        assertEquals(2, createAmericanoTournament(8).getMaxCourts())
        assertEquals(2, createAmericanoTournament(9).getMaxCourts())
        assertEquals(3, createAmericanoTournament(12).getMaxCourts())
        assertEquals(4, createAmericanoTournament(16).getMaxCourts())
    }

    @Test
    fun testGetMaxCourtsMinimumOne() {
        // Even with less than 4 players, max courts should be at least 1
        val tournament = Tournament(
            name = "Small Tournament",
            type = TournamentType.AMERICANO,
            dateCreated = 0L,
            players = createPlayers(2)
        )
        assertEquals(1, tournament.getMaxCourts())
    }

    @Test
    fun testGetEffectiveCourts() {
        // When numberOfCourts is less than max available
        val tournament1 = createAmericanoTournament(8, numberOfCourts = 1)
        assertEquals(1, tournament1.getEffectiveCourts())

        // When numberOfCourts equals max available
        val tournament2 = createAmericanoTournament(8, numberOfCourts = 2)
        assertEquals(2, tournament2.getEffectiveCourts())

        // When numberOfCourts exceeds max available (should be clamped)
        val tournament3 = createAmericanoTournament(8, numberOfCourts = 4)
        assertEquals(2, tournament3.getEffectiveCourts())
    }

    @Test
    fun testGetEffectiveCourtsWithFewPlayers() {
        // 4 players can only have 1 court, regardless of numberOfCourts setting
        val tournament = createAmericanoTournament(4, numberOfCourts = 4)
        assertEquals(1, tournament.getEffectiveCourts())
    }

    // =====================================================
    // START TOURNAMENT TESTS
    // =====================================================

    @Test
    fun testStartTournamentWithValidPlayerCount() {
        val tournament = createAmericanoTournament(8)

        val result = tournament.startTournament()

        assertTrue(result)
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testStartTournamentWithMinimumPlayers() {
        val tournament = createAmericanoTournament(4)

        val result = tournament.startTournament()

        assertTrue(result)
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testStartTournamentWithMaximumPlayers() {
        val tournament = createAmericanoTournament(16)

        val result = tournament.startTournament()

        assertTrue(result)
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testStartTournamentFailsWithTooFewPlayers() {
        val tournament = Tournament(
            name = "Too Few Players",
            type = TournamentType.AMERICANO,
            dateCreated = 0L,
            players = createPlayers(3)
        )

        assertFailsWith<IllegalStateException> {
            tournament.startTournament()
        }
        assertTrue(tournament.matches.isEmpty())
    }

    @Test
    fun testStartTournamentFailsWithTooManyPlayers() {
        val tournament = Tournament(
            name = "Too Many Players",
            type = TournamentType.AMERICANO,
            dateCreated = 0L,
            players = createPlayers(17)
        )

        assertFailsWith<IllegalStateException> {
            tournament.startTournament()
        }
        assertTrue(tournament.matches.isEmpty())
    }

    @Test
    fun testStartTournamentFailsWithNoPlayers() {
        val tournament = Tournament(
            name = "No Players",
            type = TournamentType.AMERICANO,
            dateCreated = 0L,
            players = mutableListOf()
        )

        assertFailsWith<IllegalStateException> {
            tournament.startTournament()
        }
        assertTrue(tournament.matches.isEmpty())
    }

    @Test
    fun testMexicanoTournamentNotImplemented() {
        val tournament = Tournament(
            name = "Mexicano Tournament",
            type = TournamentType.MEXICANO,
            dateCreated = 0L,
            players = createPlayers(8)
        )

        val result = tournament.startTournament()

        assertFalse(result)
    }

    @Test
    fun testStartTournamentClearsExistingMatches() {
        val tournament = createAmericanoTournament(8)

        // Start once
        tournament.startTournament()
        val initialMatchCount = tournament.matches.size

        // Start again - should clear and regenerate
        tournament.startTournament()

        assertTrue(tournament.matches.isNotEmpty())
        // Matches should be regenerated (same algorithm, same result expected)
        assertEquals(initialMatchCount, tournament.matches.size)
    }

    // =====================================================
    // MATCH GENERATION TESTS
    // =====================================================

    @Test
    fun testMatchesHaveCorrectPlayerAssignments() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        for (match in tournament.matches) {
            // Each match should have 4 different players
            val players = setOf(
                match.team1Player1.id,
                match.team1Player2.id,
                match.team2Player1.id,
                match.team2Player2.id
            )
            assertEquals(4, players.size, "Each match should have 4 unique players")
        }
    }

    @Test
    fun testMatchesHaveSequentialRoundNumbers() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        val roundNumbers = tournament.matches.map { it.roundNumber }.distinct().sorted()

        assertTrue(roundNumbers.isNotEmpty())
        assertEquals(1, roundNumbers.first())

        // Round numbers should be sequential
        for (i in 1 until roundNumbers.size) {
            assertEquals(roundNumbers[i - 1] + 1, roundNumbers[i])
        }
    }

    @Test
    fun testMatchesHaveCorrectCourtNumbers() {
        val tournament = createAmericanoTournament(8, numberOfCourts = 2)
        tournament.startTournament()

        val rounds = tournament.matches.groupBy { it.roundNumber }

        for ((roundNum, matchesInRound) in rounds) {
            val courtNumbers = matchesInRound.map { it.courtNumber }.sorted()

            // Court numbers should be sequential starting from 1
            for (i in courtNumbers.indices) {
                assertEquals(i + 1, courtNumbers[i], "Court numbers should be sequential in round $roundNum")
            }
        }
    }

    @Test
    fun testAllPlayersHaveEqualMatchesAfterStart() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        val matchCounts = tournament.players.associate { player ->
            player.id to tournament.matches.count { match ->
                match.team1Player1.id == player.id ||
                match.team1Player2.id == player.id ||
                match.team2Player1.id == player.id ||
                match.team2Player2.id == player.id
            }
        }

        val uniqueCounts = matchCounts.values.toSet()
        assertEquals(1, uniqueCounts.size, "All players should have the same number of matches")
    }

    @Test
    fun testAllOpponentPairsCoveredFor4Players() {
        val tournament = createAmericanoTournament(4)
        tournament.startTournament()

        val opponentPairs = mutableSetOf<Set<String>>()

        for (match in tournament.matches) {
            val team1 = listOf(match.team1Player1, match.team1Player2)
            val team2 = listOf(match.team2Player1, match.team2Player2)

            for (p1 in team1) {
                for (p2 in team2) {
                    opponentPairs.add(setOf(p1.id, p2.id))
                }
            }
        }

        // For 4 players, all 6 pairs should eventually be opponents
        val expectedPairs = 4 * 3 / 2 // C(4,2) = 6
        assertEquals(expectedPairs, opponentPairs.size, "All player pairs should have played as opponents")
    }

    @Test
    fun testMultipleCourtsUsedInEachRound() {
        val tournament = createAmericanoTournament(8, numberOfCourts = 2)
        tournament.startTournament()

        val rounds = tournament.matches.groupBy { it.roundNumber }

        for ((roundNum, matchesInRound) in rounds) {
            assertEquals(2, matchesInRound.size, "Round $roundNum should have 2 matches (one per court)")
        }
    }

    @Test
    fun testNoDuplicatePlayersInSameRound() {
        val tournament = createAmericanoTournament(8, numberOfCourts = 2)
        tournament.startTournament()

        val rounds = tournament.matches.groupBy { it.roundNumber }

        for ((roundNum, matchesInRound) in rounds) {
            val allPlayersInRound = matchesInRound.flatMap { match ->
                listOf(
                    match.team1Player1.id,
                    match.team1Player2.id,
                    match.team2Player1.id,
                    match.team2Player2.id
                )
            }

            assertEquals(
                allPlayersInRound.size,
                allPlayersInRound.toSet().size,
                "No player should appear in multiple matches in round $roundNum"
            )
        }
    }

    // =====================================================
    // EXTEND TOURNAMENT TESTS
    // =====================================================

    @Test
    fun testExtendTournamentAddsMoreMatches() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        val initialMatchCount = tournament.matches.size

        tournament.extendTournament()

        assertTrue(tournament.matches.size > initialMatchCount, "Extend should add more matches")
    }

    @Test
    fun testExtendTournamentStartsIfNoMatches() {
        val tournament = createAmericanoTournament(8)

        // Extend without starting first
        val result = tournament.extendTournament()

        assertTrue(result)
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testExtendTournamentMaintainsEqualMatchCounts() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()
        tournament.extendTournament()

        val matchCounts = tournament.players.associate { player ->
            player.id to tournament.matches.count { match ->
                match.team1Player1.id == player.id ||
                match.team1Player2.id == player.id ||
                match.team2Player1.id == player.id ||
                match.team2Player2.id == player.id
            }
        }

        val uniqueCounts = matchCounts.values.toSet()
        assertEquals(1, uniqueCounts.size, "All players should have equal matches after extend")
    }

    @Test
    fun testExtendTournamentContinuesRoundNumbers() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        val maxRoundBefore = tournament.matches.maxOf { it.roundNumber }

        tournament.extendTournament()

        val roundsAfter = tournament.matches.map { it.roundNumber }.distinct().sorted()

        assertTrue(roundsAfter.max() > maxRoundBefore, "New rounds should be added")

        // Round numbers should still be sequential
        for (i in 1 until roundsAfter.size) {
            assertEquals(roundsAfter[i - 1] + 1, roundsAfter[i])
        }
    }

    @Test
    fun testExtendTournamentFailsWithInvalidPlayerCount() {
        val tournament = Tournament(
            name = "Invalid",
            type = TournamentType.AMERICANO,
            dateCreated = 0L,
            players = createPlayers(3)
        )

        assertFailsWith<IllegalStateException> {
            tournament.extendTournament()
        }
    }

    // =====================================================
    // EDGE CASE TESTS
    // =====================================================

    @Test
    fun testTournamentWithOddNumberOfPlayers() {
        // 5 players - one player sits out each round
        val tournament = createAmericanoTournament(5)
        val result = tournament.startTournament()

        assertTrue(result)
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testTournamentWith6Players() {
        val tournament = createAmericanoTournament(6)
        val result = tournament.startTournament()

        assertTrue(result)
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testMatchInitialState() {
        val tournament = createAmericanoTournament(4)
        tournament.startTournament()

        for (match in tournament.matches) {
            assertEquals(0, match.scoreTeam1, "Initial score should be 0")
            assertEquals(0, match.scoreTeam2, "Initial score should be 0")
            assertFalse(match.isPlayed, "Match should not be marked as played initially")
        }
    }

    @Test
    fun testMultipleExtensions() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        val count1 = tournament.matches.size

        tournament.extendTournament()
        val count2 = tournament.matches.size
        assertTrue(count2 > count1, "First extension should add matches")

        tournament.extendTournament()
        val count3 = tournament.matches.size
        assertTrue(count3 > count2, "Second extension should add more matches")
    }

    // =====================================================
    // VARIOUS PLAYER COUNT TESTS
    // =====================================================

    @Test
    fun testTournamentWith4Players1Court() {
        val tournament = createAmericanoTournament(4, 1)
        assertTrue(tournament.startTournament())
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testTournamentWith8Players2Courts() {
        val tournament = createAmericanoTournament(8, 2)
        assertTrue(tournament.startTournament())
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testTournamentWith12Players3Courts() {
        val tournament = createAmericanoTournament(12, 3)
        assertTrue(tournament.startTournament())
        assertTrue(tournament.matches.isNotEmpty())
    }

    @Test
    fun testTournamentWith16Players4Courts() {
        val tournament = createAmericanoTournament(16, 4)
        assertTrue(tournament.startTournament())
        assertTrue(tournament.matches.isNotEmpty())
    }

    // =====================================================
    // MATCH AND PLAYER DATA INTEGRITY TESTS
    // =====================================================

    @Test
    fun testMatchReferencesActualPlayers() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        val playerIds = tournament.players.map { it.id }.toSet()

        for (match in tournament.matches) {
            assertTrue(match.team1Player1.id in playerIds, "Match player should be in tournament players")
            assertTrue(match.team1Player2.id in playerIds, "Match player should be in tournament players")
            assertTrue(match.team2Player1.id in playerIds, "Match player should be in tournament players")
            assertTrue(match.team2Player2.id in playerIds, "Match player should be in tournament players")
        }
    }

    @Test
    fun testPlayerObjectsAreShared() {
        // Ensure that the same Player objects are used throughout
        val tournament = createAmericanoTournament(4)
        tournament.startTournament()

        val match = tournament.matches.first()
        match.scoreTeam1 = 21
        match.scoreTeam2 = 15
        match.isPlayed = true

        // The player in the match and in the players list should be the same object
        val playerFromList = tournament.players.find { it.id == match.team1Player1.id }
        assertEquals(match.team1Player1.totalPoints, playerFromList?.totalPoints)
    }

    @Test
    fun testPlayersHaveUniqueIds() {
        val tournament = createAmericanoTournament(8)

        val ids = tournament.players.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "All players should have unique IDs")
    }

    @Test
    fun testMatchesHaveUniqueIds() {
        val tournament = createAmericanoTournament(8)
        tournament.startTournament()

        val ids = tournament.matches.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "All matches should have unique IDs")
    }
}

