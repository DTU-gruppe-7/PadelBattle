package dk.dtu.padelbattle.domain.model

import dk.dtu.padelbattle.domain.generator.AmericanoGenerator
import dk.dtu.padelbattle.domain.generator.MexicanoGenerator
import dk.dtu.padelbattle.domain.generator.TournamentGenerator
import dk.dtu.padelbattle.domain.util.generateId

/**
 * Repræsenterer en padel-turnering (immutable).
 * 
 * Turneringen indeholder spillere og kampe. Brug [copy] til at oprette
 * opdaterede versioner af turneringen.
 */
data class Tournament(
    val id: String = generateId(),
    val name: String,
    val type: TournamentType,
    val dateCreated: Long,
    val numberOfCourts: Int = 1,
    val pointsPerMatch: Int = 16,
    val players: List<Player> = emptyList(),
    val matches: List<Match> = emptyList(),
    val isCompleted: Boolean = false
) {

    companion object {
        const val MAX_COURTS = 8
        const val MAX_PLAYERS = 32
        const val MIN_PLAYERS = 4

        /**
         * Henter den korrekte generator baseret på turneringstype.
         */
        fun getGenerator(type: TournamentType): TournamentGenerator = when (type) {
            TournamentType.AMERICANO -> AmericanoGenerator()
            TournamentType.MEXICANO -> MexicanoGenerator()
        }
    }

    // --- PUBLIC API ---

    /**
     * Returnerer maksimalt antal baner baseret på antal spillere.
     * Der kræves 4 spillere per bane.
     */
    fun getMaxCourts(): Int = (players.size / 4).coerceIn(1, MAX_COURTS)

    /**
     * Returnerer det faktiske antal baner der vil blive brugt.
     * Dette er minimum af numberOfCourts og getMaxCourts()
     */
    fun getEffectiveCourts(): Int = numberOfCourts.coerceIn(1, getMaxCourts())

    /**
     * Tjekker om der er nogle kampe der allerede er blevet spillet.
     * Returnerer true hvis mindst én kamp har isPlayed = true
     */
    fun hasPlayedMatches(): Boolean = matches.any { it.isPlayed }

    /**
     * Genererer initielle kampe for turneringen.
     * Returnerer en ny Tournament med genererede kampe.
     * 
     * @return Ny Tournament med genererede kampe
     * @throws IllegalStateException hvis antal spillere er ugyldigt
     */
    fun generateInitialMatches(): Tournament {
        validatePlayerCount()

        val generator = getGenerator(type)
        val generatedMatches = generator.generateInitialMatches(
            players = players,
            numberOfCourts = getEffectiveCourts()
        )

        return copy(matches = generatedMatches)
    }

    /**
     * Udvider turneringen med flere kampe.
     * Returnerer en ny Tournament med eksisterende + nye kampe.
     * 
     * @return Ny Tournament med udvidede kampe, eller null hvis ingen nye kampe kunne genereres
     * @throws IllegalStateException hvis antal spillere er ugyldigt
     */
    fun generateExtensionMatches(): Tournament? {
        validatePlayerCount()

        if (matches.isEmpty()) {
            return generateInitialMatches()
        }

        val generator = getGenerator(type)
        val newMatches = generator.generateExtensionMatches(
            players = players,
            existingMatches = matches,
            numberOfCourts = getEffectiveCourts()
        )

        return if (newMatches.isNotEmpty()) {
            copy(matches = matches + newMatches)
        } else {
            null
        }
    }

    // --- PRIVATE HELPERS ---

    /**
     * Validerer at antal spillere er inden for tilladte grænser.
     * @throws IllegalStateException hvis antal spillere er ugyldigt
     */
    private fun validatePlayerCount() {
        if (players.size < MIN_PLAYERS) {
            throw IllegalStateException("Fejl: Der skal være mindst $MIN_PLAYERS spillere.")
        }
        if (players.size > MAX_PLAYERS) {
            throw IllegalStateException("Fejl: Maksimalt $MAX_PLAYERS spillere understøttes.")
        }
    }
}
