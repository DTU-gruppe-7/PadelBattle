package dk.dtu.padelbattle.model

/**
 * Represents the result of a match with team scores.
 * This is a value object that encapsulates the outcome of a match.
 */
data class MatchResult(
    val scoreTeam1: Int,
    val scoreTeam2: Int
) {
    /**
     * Determines the winner of the match.
     * @return MatchOutcome indicating which team won or if it's a draw
     */
    fun getOutcome(): MatchOutcome {
        return when {
            scoreTeam1 > scoreTeam2 -> MatchOutcome.TEAM1_WIN
            scoreTeam2 > scoreTeam1 -> MatchOutcome.TEAM2_WIN
            else -> MatchOutcome.DRAW
        }
    }
}

enum class MatchOutcome {
    TEAM1_WIN,
    TEAM2_WIN,
    DRAW
}

