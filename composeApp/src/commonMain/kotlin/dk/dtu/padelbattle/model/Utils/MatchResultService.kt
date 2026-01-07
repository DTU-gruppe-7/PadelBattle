package dk.dtu.padelbattle.model.Utils

import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.MatchOutcome
import dk.dtu.padelbattle.model.MatchResult

/**
 * Service responsible for managing match results and updating player statistics.
 * This follows the Single Responsibility Principle by separating the match result logic
 * from the ViewModel and domain models.
 */
class MatchResultService {

    /**
     * Records a match result, updating the match and player statistics.
     * If the match was already played, it first reverts the old statistics before applying new ones.
     *
     * @param match The match to update
     * @param newResult The new result to apply
     */
    fun recordMatchResult(match: Match, newResult: MatchResult) {
        // If the match was already played, revert old statistics first
        if (match.isPlayed) {
            val oldResult = MatchResult(match.scoreTeam1, match.scoreTeam2)
            revertPlayerStatistics(match, oldResult)
        }

        // Update match scores
        match.scoreTeam1 = newResult.scoreTeam1
        match.scoreTeam2 = newResult.scoreTeam2
        match.isPlayed = true

        // Apply new statistics
        applyPlayerStatistics(match, newResult)
    }

    /**
     * Applies player statistics based on the match result.
     * Updates points, wins, losses, draws, and games played for all players.
     *
     * @param match The match containing the players
     * @param result The result to apply
     */
    private fun applyPlayerStatistics(match: Match, result: MatchResult) {
        val team1Players = listOf(match.team1Player1, match.team1Player2)
        val team2Players = listOf(match.team2Player1, match.team2Player2)

        // Update points scored
        team1Players.forEach { player ->
            player.totalPoints += result.scoreTeam1
        }
        team2Players.forEach { player ->
            player.totalPoints += result.scoreTeam2
        }

        // Update wins, losses, draws, and games played based on outcome
        when (result.getOutcome()) {
            MatchOutcome.TEAM1_WIN -> {
                team1Players.forEach { player ->
                    player.wins++
                    player.gamesPlayed++
                }
                team2Players.forEach { player ->
                    player.losses++
                    player.gamesPlayed++
                }
            }
            MatchOutcome.TEAM2_WIN -> {
                team2Players.forEach { player ->
                    player.wins++
                    player.gamesPlayed++
                }
                team1Players.forEach { player ->
                    player.losses++
                    player.gamesPlayed++
                }
            }
            MatchOutcome.DRAW -> {
                (team1Players + team2Players).forEach { player ->
                    player.draws++
                    player.gamesPlayed++
                }
            }
        }
    }

    /**
     * Reverts player statistics that were previously applied from a match result.
     * Used when editing a match that has already been played.
     *
     * @param match The match containing the players
     * @param result The result to revert
     */
    private fun revertPlayerStatistics(match: Match, result: MatchResult) {
        val team1Players = listOf(match.team1Player1, match.team1Player2)
        val team2Players = listOf(match.team2Player1, match.team2Player2)

        // Revert points
        team1Players.forEach { player ->
            player.totalPoints -= result.scoreTeam1
        }
        team2Players.forEach { player ->
            player.totalPoints -= result.scoreTeam2
        }

        // Revert wins, losses, draws, and games played based on outcome
        when (result.getOutcome()) {
            MatchOutcome.TEAM1_WIN -> {
                team1Players.forEach { player ->
                    player.wins--
                    player.gamesPlayed--
                }
                team2Players.forEach { player ->
                    player.losses--
                    player.gamesPlayed--
                }
            }
            MatchOutcome.TEAM2_WIN -> {
                team2Players.forEach { player ->
                    player.wins--
                    player.gamesPlayed--
                }
                team1Players.forEach { player ->
                    player.losses--
                    player.gamesPlayed--
                }
            }
            MatchOutcome.DRAW -> {
                (team1Players + team2Players).forEach { player ->
                    player.draws--
                    player.gamesPlayed--
                }
            }
        }
    }
}