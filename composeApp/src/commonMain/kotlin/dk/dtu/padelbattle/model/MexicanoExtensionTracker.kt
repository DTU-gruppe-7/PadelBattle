package dk.dtu.padelbattle.model

/**
 * Singleton der tracker Mexicano-turneringer der er blevet udvidet.
 * Bruges til at sikre at mindst 2 runder spilles efter udvidelse.
 * 
 * Note: Dette er in-memory og nulstilles når appen lukkes.
 */
object MexicanoExtensionTracker {
    
    // Map af tournamentId til antal runder der mangler efter udvidelse
    private val roundsRemaining = mutableMapOf<String, Int>()
    
    /**
     * Registrerer at en turnering er blevet udvidet.
     * Sætter at der skal spilles mindst 2 runder mere.
     */
    fun registerExtension(tournamentId: String) {
        roundsRemaining[tournamentId] = 2
    }
    
    /**
     * Kaldes når en runde er færdigspillet.
     * Returnerer true hvis turneringen kan afsluttes (0 runder tilbage).
     */
    fun roundCompleted(tournamentId: String): Boolean {
        val remaining = roundsRemaining[tournamentId] ?: 0
        if (remaining > 0) {
            roundsRemaining[tournamentId] = remaining - 1
            return remaining - 1 <= 0
        }
        return true // Ingen udvidelse registreret, kan afsluttes
    }
    
    /**
     * Tjekker om turneringen kan afsluttes.
     */
    fun canComplete(tournamentId: String): Boolean {
        return (roundsRemaining[tournamentId] ?: 0) <= 0
    }
    
    /**
     * Rydder tracking for en turnering.
     */
    fun clear(tournamentId: String) {
        roundsRemaining.remove(tournamentId)
    }
}
