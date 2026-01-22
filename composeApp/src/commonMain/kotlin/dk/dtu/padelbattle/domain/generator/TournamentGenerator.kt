package dk.dtu.padelbattle.domain.generator

import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.Player

/**
 * Interface for turneringsgenerering.
 * Implementeringer håndterer specifik logik for forskellige turneringstyper
 * (Americano, Mexicano, etc.)
 */
interface TournamentGenerator {

    /**
     * Genererer alle kampe for en ny turnering.
     * Kaldes når turneringen startes første gang.
     *
     * @param players Liste af spillere i turneringen
     * @param numberOfCourts Antal baner der bruges
     * @return Liste af genererede kampe
     */
    fun generateInitialMatches(
        players: List<Player>,
        numberOfCourts: Int
    ): List<Match>

    /**
     * Udvider turneringen med flere kampe.
     * Kaldes når brugeren vil fortsætte en afsluttet turnering.
     *
     * @param players Liste af spillere i turneringen
     * @param existingMatches Eksisterende kampe (bruges til at undgå gentagelser)
     * @param numberOfCourts Antal baner der bruges
     * @return Liste af NYE kampe der skal tilføjes
     */
    fun generateExtensionMatches(
        players: List<Player>,
        existingMatches: List<Match>,
        numberOfCourts: Int
    ): List<Match>
}
