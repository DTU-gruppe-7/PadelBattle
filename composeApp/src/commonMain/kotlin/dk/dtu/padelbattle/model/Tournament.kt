package dk.dtu.padelbattle.model

import dk.dtu.padelbattle.model.generator.AmericanoGenerator
import dk.dtu.padelbattle.model.generator.MexicanoGenerator
import dk.dtu.padelbattle.model.generator.TournamentGenerator
import dk.dtu.padelbattle.model.utils.generateId

/**
 * Repræsenterer en padel-turnering.
 * 
 * Turneringen indeholder spillere og kampe, og delegerer
 * generering af kampe til den passende [TournamentGenerator].
 */
class Tournament(
    val id: String = generateId(),
    var name: String,
    val type: TournamentType,
    val dateCreated: Long,
    var numberOfCourts: Int = 1,
    var pointsPerMatch: Int = 16,
    val players: MutableList<Player> = mutableListOf(),
    val matches: MutableList<Match> = mutableListOf(),
    var isCompleted: Boolean = false
) {

    companion object {
        const val MAX_COURTS = 8
        const val MAX_PLAYERS = 32
        const val MIN_PLAYERS = 4
    }

    /**
     * Generator til at oprette kampe baseret på turneringstypen.
     * Lazy-initialiseret for at undgå unødvendig instansiering.
     */
    private val generator: TournamentGenerator by lazy {
        when (type) {
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
     * Starter turneringen ved at generere initielle kampe.
     * Rydder eksisterende kampe og genererer nye baseret på turneringstypen.
     * 
     * @return true hvis turneringen blev startet succesfuldt
     * @throws IllegalStateException hvis antal spillere er ugyldigt
     */
    fun startTournament(): Boolean {
        validatePlayerCount()

        matches.clear()

        val generatedMatches = generator.generateInitialMatches(
            players = players.toList(),
            numberOfCourts = getEffectiveCourts()
        )

        matches.addAll(generatedMatches)
        return true
    }

    /**
     * Udvider turneringen med flere kampe.
     * Bruges til at fortsætte en afsluttet turnering.
     * 
     * @return true hvis turneringen blev udvidet succesfuldt
     * @throws IllegalStateException hvis antal spillere er ugyldigt
     */
    fun extendTournament(): Boolean {
        validatePlayerCount()

        if (matches.isEmpty()) {
            return startTournament()
        }

        val newMatches = generator.generateExtensionMatches(
            players = players.toList(),
            existingMatches = matches.toList(),
            numberOfCourts = getEffectiveCourts()
        )

        matches.addAll(newMatches)
        return newMatches.isNotEmpty()
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
