package dk.dtu.padelbattle.domain.service

import dk.dtu.padelbattle.data.repository.TournamentRepository
import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.MatchOutcome
import dk.dtu.padelbattle.domain.model.MatchResult
import dk.dtu.padelbattle.domain.model.Player

/**
 * Service responsible for managing match results and updating player statistics.
 * This follows the Single Responsibility Principle by separating the match result logic
 * from the ViewModel and domain models.
 */
class MatchResultService(
    private val repository: TournamentRepository
) {

    /**
     * Records a match result, updating the match and player statistics.
     * If the match was already played, it first reverts the old statistics before applying new ones.
     *
     * @param match The match to update
     * @param newResult The new result to apply
     * @param tournamentId The tournament ID for database operations
     * @throws IllegalArgumentException if tournamentId is blank
     */
    suspend fun recordMatchResult(match: Match, newResult: MatchResult, tournamentId: String) {
        try {
            require(tournamentId.isNotBlank()) { "tournamentId cannot be blank" }

            // Get current players from the match
            var player1Team1 = match.team1Player1
            var player1Team2 = match.team1Player2
            var player2Team1 = match.team2Player1
            var player2Team2 = match.team2Player2

            // If the match was already played, revert old statistics first
            if (match.isPlayed) {
                val oldResult = MatchResult(match.scoreTeam1, match.scoreTeam2)
                val revertedPlayers = revertPlayerStatistics(
                    listOf(player1Team1, player1Team2),
                    listOf(player2Team1, player2Team2),
                    oldResult
                )
                player1Team1 = revertedPlayers[0]
                player1Team2 = revertedPlayers[1]
                player2Team1 = revertedPlayers[2]
                player2Team2 = revertedPlayers[3]
            }

            // Apply new statistics
            val updatedPlayers = applyPlayerStatistics(
                listOf(player1Team1, player1Team2),
                listOf(player2Team1, player2Team2),
                newResult
            )

            // Create updated match with new scores and updated player references
            val updatedMatch = match.copy(
                scoreTeam1 = newResult.scoreTeam1,
                scoreTeam2 = newResult.scoreTeam2,
                isPlayed = true,
                team1Player1 = updatedPlayers[0],
                team1Player2 = updatedPlayers[1],
                team2Player1 = updatedPlayers[2],
                team2Player2 = updatedPlayers[3]
            )

            repository.updateMatch(updatedMatch, tournamentId)

            updatedPlayers.forEach { player ->
                repository.updatePlayer(player, tournamentId)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Applies player statistics based on the match result.
     * Returns new Player instances with updated statistics.
     */
    private fun applyPlayerStatistics(
        team1Players: List<Player>,
        team2Players: List<Player>,
        result: MatchResult
    ): List<Player> {
        // Update points scored
        val team1WithPoints = team1Players.map { player ->
            player.copy(totalPoints = player.totalPoints + result.scoreTeam1)
        }
        val team2WithPoints = team2Players.map { player ->
            player.copy(totalPoints = player.totalPoints + result.scoreTeam2)
        }

        // Update wins, losses, draws, and games played based on outcome
        return when (result.getOutcome()) {
            MatchOutcome.TEAM1_WIN -> {
                val team1Updated = team1WithPoints.map { player ->
                    player.copy(wins = player.wins + 1, gamesPlayed = player.gamesPlayed + 1)
                }
                val team2Updated = team2WithPoints.map { player ->
                    player.copy(losses = player.losses + 1, gamesPlayed = player.gamesPlayed + 1)
                }
                team1Updated + team2Updated
            }
            MatchOutcome.TEAM2_WIN -> {
                val team1Updated = team1WithPoints.map { player ->
                    player.copy(losses = player.losses + 1, gamesPlayed = player.gamesPlayed + 1)
                }
                val team2Updated = team2WithPoints.map { player ->
                    player.copy(wins = player.wins + 1, gamesPlayed = player.gamesPlayed + 1)
                }
                team1Updated + team2Updated
            }
            MatchOutcome.DRAW -> {
                (team1WithPoints + team2WithPoints).map { player ->
                    player.copy(draws = player.draws + 1, gamesPlayed = player.gamesPlayed + 1)
                }
            }
        }
    }

    /**
     * Reverts player statistics that were previously applied from a match result.
     * Returns new Player instances with reverted statistics.
     */
    private fun revertPlayerStatistics(
        team1Players: List<Player>,
        team2Players: List<Player>,
        result: MatchResult
    ): List<Player> {
        // Revert points
        val team1WithPoints = team1Players.map { player ->
            player.copy(totalPoints = player.totalPoints - result.scoreTeam1)
        }
        val team2WithPoints = team2Players.map { player ->
            player.copy(totalPoints = player.totalPoints - result.scoreTeam2)
        }

        // Revert wins, losses, draws, and games played based on outcome
        return when (result.getOutcome()) {
            MatchOutcome.TEAM1_WIN -> {
                val team1Updated = team1WithPoints.map { player ->
                    player.copy(wins = player.wins - 1, gamesPlayed = player.gamesPlayed - 1)
                }
                val team2Updated = team2WithPoints.map { player ->
                    player.copy(losses = player.losses - 1, gamesPlayed = player.gamesPlayed - 1)
                }
                team1Updated + team2Updated
            }
            MatchOutcome.TEAM2_WIN -> {
                val team1Updated = team1WithPoints.map { player ->
                    player.copy(losses = player.losses - 1, gamesPlayed = player.gamesPlayed - 1)
                }
                val team2Updated = team2WithPoints.map { player ->
                    player.copy(wins = player.wins - 1, gamesPlayed = player.gamesPlayed - 1)
                }
                team1Updated + team2Updated
            }
            MatchOutcome.DRAW -> {
                (team1WithPoints + team2WithPoints).map { player ->
                    player.copy(draws = player.draws - 1, gamesPlayed = player.gamesPlayed - 1)
                }
            }
        }
    }
}
