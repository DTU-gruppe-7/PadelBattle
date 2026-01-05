package dk.dtu.padelbattle.model

import dk.dtu.padelbattle.model.Utils.generateId

class Tournament(
    val id: String = generateId(),
    val name: String,
    val type: TournamentType,
    val dateCreated: Long,
    val players: MutableList<Player> = mutableListOf(),
    val matches: MutableList<Match> = mutableListOf()
) {

    // --- LOGIK START ---

    /**
     * Hovedfunktion til at generere kampprogrammet.
     * Denne skal kaldes, når man trykker "Start Turnering" i UI.
     */
    fun generateSchedule() {
        matches.clear() // Ryd gamle kampe hvis man regenererer

        // Krav: 4-16 spillere
        if (players.size < 4) {
            println("Fejl: Der skal være mindst 4 spillere.")
            return
        }

        when (type) {
            TournamentType.AMERICANO -> generateSmartAmericano()
            TournamentType.MEXICANO -> {
                // Mexicano implementeres senere (kræver runde-for-runde logik)
            }
        }
    }

    /**
     * Beregner stillingen baseret på færdigspillede kampe.
     * Opfylder kravet om "Visning af resultat"[cite: 18].
     */
    fun calculateStandings() {
        // 1. Nulstil point for alle
        players.forEach {
            it.totalPoints = 0
            it.gamesPlayed = 0
        }

        // 2. Løb igennem alle færdige kampe
        matches.filter { it.isPlayed }.forEach { match ->
            // I Americano får hver spiller point svarende til deres holds score
            addPoints(match.team1Player1, match.scoreTeam1)
            addPoints(match.team1Player2, match.scoreTeam1)
            addPoints(match.team2Player1, match.scoreTeam2)
            addPoints(match.team2Player2, match.scoreTeam2)
        }

        // 3. Sorter listen: Flest point øverst (Leaderboard logik [cite: 21])
        players.sortByDescending { it.totalPoints }
    }

    private fun addPoints(player: Player, points: Int) {
        // Find spilleren og opdater (hvis det ikke er en Ghost-spiller)
            player.totalPoints += points
            player.gamesPlayed += 1
    }

    /**
     * En "Smart" algoritme der håndterer både lige og ulige antal spillere (4-16).
     * Bruger en rotations-metode for at sikre variation.
     */
    private fun generateSmartAmericano() {
        // Lav en kopi af listen, så vi kan rode rundt i den uden at ødelægge originalen
        val activePlayers = players.toMutableList()

        // HÅNDTERING AF ULIGE ANTAL (Fairness )
        // Hvis antallet er ulige, tilføjer vi en "Ghost" (dummy spiller).
        // Den der spiller sammen med Ghosten, sidder over i den runde.
        val hasGhost = activePlayers.size % 2 != 0
        if (hasGhost) {
            activePlayers.add(Player(id = "GHOST", name = "Pause"))
        }

        val totalCount = activePlayers.size
        // Antal runder: Normalt spiller alle mod alle ca. en gang.
        // Her sætter vi antal runder til antal spillere - 1 for at sikre rotation.
        val numberOfRounds = totalCount - 1
        val matchesPerRound = totalCount / 4 // Vi skal bruge 4 spillere pr kamp

        for (round in 0 until numberOfRounds) {
            for (i in 0 until matchesPerRound) {
                // Vi tager 4 spillere ad gangen fra listen baseret på indeksering
                // Denne logik sikrer, at vi trækker nye kombinationer, når vi roterer listen senere
                val p1 = activePlayers[i * 4]
                val p2 = activePlayers[i * 4 + 1]
                val p3 = activePlayers[i * 4 + 2]
                val p4 = activePlayers[i * 4 + 3]

                // Tjek om nogen er "GHOST". Hvis ja, oprettes kampen ikke (spillerne sidder over)
                if (p1.id == "GHOST" || p2.id == "GHOST" || p3.id == "GHOST" || p4.id == "GHOST") {
                    continue
                }

                // Opret kampen
                matches.add(
                    Match(
                        roundNumber = round + 1,
                        courtNumber = i + 1, // Banenummer
                        team1Player1 = p1,
                        team1Player2 = p2,
                        team2Player1 = p3,
                        team2Player2 = p4
                    )
                )
            }

            // ROTATION: Dette er nøglen til Americano.
            // Vi fastholder spiller 0, og roterer resten af listen.
            // Dette er en klassisk turneringsteknik (Circle Method).
            val lastPlayer = activePlayers.removeAt(activePlayers.lastIndex)
            activePlayers.add(1, lastPlayer)
        }
    }
}