package dk.dtu.padelbattle.domain.model

/**
 * Letvægts-repræsentation af en turnering til visning i lister.
 * Undgår at loade alle spillere og kampe når det ikke er nødvendigt.
 * 
 * Bruges til:
 * - HomeScreen turneringsliste
 * - Søgeresultater
 */
data class TournamentSummary(
    val id: String,
    val name: String,
    val type: TournamentType,
    val dateCreated: Long,
    val numberOfCourts: Int,
    val pointsPerMatch: Int,
    val isCompleted: Boolean,
    val playerCount: Int,
    val matchCount: Int,
    val playedMatchCount: Int,
    val roundCount: Int = 0,
    /** Navne på vindere (spillere med højest points). Kun udfyldt for afsluttede turneringer. */
    val winnerNames: List<String> = emptyList()
)
