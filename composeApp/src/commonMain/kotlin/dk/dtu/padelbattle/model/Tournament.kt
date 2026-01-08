package dk.dtu.padelbattle.model

import dk.dtu.padelbattle.model.utils.generateId

class Tournament(
    val id: String = generateId(),
    var name: String,
    val type: TournamentType,
    val dateCreated: Long,
    val numberOfCourts: Int = 1, // Default to 1, valid range: 1-4
    val winScore: Int = 32,
    val players: MutableList<Player> = mutableListOf(),
    val matches: MutableList<Match> = mutableListOf(),
    var isCompleted: Boolean = false
) {

    // --- TRACKING DATA ---
    // Disse maps holder styr på spillernes historik gennem turneringen
    private val playedAgainst = mutableMapOf<String, MutableSet<String>>()
    private val partnerCount = mutableMapOf<Set<String>, Int>()
    private val matchCount = mutableMapOf<String, Int>()

    // --- PUBLIC API ---

    /**
     * Beregner det maksimale antal baner der kan bruges baseret på antal spillere.
     * Hver bane kræver 4 spillere, så max baner = antal spillere / 4
     */
    fun getMaxCourts(): Int = (players.size / 4).coerceAtLeast(1)

    /**
     * Returnerer det faktiske antal baner der vil blive brugt.
     * Dette er minimum af numberOfCourts og getMaxCourts()
     */
    fun getEffectiveCourts(): Int = numberOfCourts.coerceIn(1, getMaxCourts())

    /**
     * Starter turneringen og genererer det initielle kampprogram.
     * Kaldes fra UI når man trykker "Start Turnering".
     */
    fun startTournament(): Boolean {
        if (!validatePlayerCount()) return false

        // Ryd alt og start forfra
        matches.clear()
        resetTrackingData()

        return when (type) {
            TournamentType.AMERICANO -> {
                generateAmericanoRounds(untilAllHavePlayed = true)
                true
            }
            TournamentType.MEXICANO -> {
                // Mexicano implementeres senere (kræver runde-for-runde logik)
                false
            }
        }
    }

    /**
     * Forlænger turneringen med flere runder.
     * Fortsætter indtil alle spillere igen har et lige antal kampe.
     * Overholder stadig alle regler (undgår genbrugte makkerpar osv.)
     * Kaldes fra UI når man trykker "Forlæng Turnering".
     */
    fun extendTournament(): Boolean {
        if (!validatePlayerCount()) return false
        if (matches.isEmpty()) return startTournament() // Hvis ingen kampe, start i stedet

        return when (type) {
            TournamentType.AMERICANO -> {
                generateAmericanoRounds(untilAllHavePlayed = false)
                true
            }
            TournamentType.MEXICANO -> false
        }
    }

    // --- PRIVATE HELPERS ---

    private fun validatePlayerCount(): Boolean {
        if (players.size < 4) {
            throw IllegalStateException("Fejl: Der skal være mindst 4 spillere.")
        }
        if (players.size > 16) {
            throw IllegalStateException("Fejl: Maksimalt 16 spillere understøttes.")
        }
        return true
    }

    private fun resetTrackingData() {
        playedAgainst.clear()
        partnerCount.clear()
        matchCount.clear()

        players.forEach { player ->
            playedAgainst[player.id] = mutableSetOf()
            matchCount[player.id] = 0
        }
    }

    private fun rebuildTrackingFromMatches() {
        resetTrackingData()

        matches.forEach { match ->
            val t1p1 = match.team1Player1
            val t1p2 = match.team1Player2
            val t2p1 = match.team2Player1
            val t2p2 = match.team2Player2

            // Opdater modstander tracking
            recordOpponents(t1p1, t1p2, t2p1, t2p2)

            // Opdater partner tracking
            recordPartners(t1p1, t1p2)
            recordPartners(t2p1, t2p2)

            // Opdater match count
            listOf(t1p1, t1p2, t2p1, t2p2).forEach { player ->
                matchCount[player.id] = (matchCount[player.id] ?: 0) + 1
            }
        }
    }

    private fun recordOpponents(t1p1: Player, t1p2: Player, t2p1: Player, t2p2: Player) {
        for (teamPlayer in listOf(t1p1, t1p2)) {
            for (opponent in listOf(t2p1, t2p2)) {
                playedAgainst[teamPlayer.id]?.add(opponent.id)
                playedAgainst[opponent.id]?.add(teamPlayer.id)
            }
        }
    }

    private fun recordPartners(p1: Player, p2: Player) {
        val pair = setOf(p1.id, p2.id)
        partnerCount[pair] = (partnerCount[pair] ?: 0) + 1
    }

    // --- AMERICANO ALGORITHM ---

    /**
     * Americano-algoritme der sikrer:
     * 1. Alle spillere spiller MOD alle andre som modstander mindst én gang
     * 2. Minimerer antal kampe
     * 3. Undgår gentagelse af makkerpar medmindre nødvendigt
     * 4. Fungerer for 4-16 spillere
     * 5. Alle baner bruges i hver runde (ingen tomme baner)
     * 6. Alle spillere ender med samme antal kampe
     * 7. Mulighed for at forlænge turneringen til næste gang alle spillere har et lige antal kampe
     */
    private fun generateAmericanoRounds(untilAllHavePlayed: Boolean) {
        val courts = getEffectiveCourts()

        // Genopbyg tracking fra eksisterende kampe (relevant ved forlængelse)
        if (matches.isNotEmpty()) {
            rebuildTrackingFromMatches()
        }

        val startMatchCount = matches.size
        var currentRound = (matches.maxOfOrNull { it.roundNumber } ?: 0) + 1

        // Bestem stopbetingelse
        val targetCondition: () -> Boolean = if (untilAllHavePlayed) {
            { allOpponentPairsCovered() && allPlayersHaveEqualMatches() }
        } else {
            // Ved forlængelse: generer mindst én runde, derefter fortsæt til alle har lige antal kampe
            var minRoundsGenerated = false
            {
                if (!minRoundsGenerated && matches.size > startMatchCount) {
                    minRoundsGenerated = true
                }
                minRoundsGenerated && allPlayersHaveEqualMatches()
            }
        }

        var safetyCounter = 0
        val maxRounds = 100

        while (!targetCondition() && safetyCounter < maxRounds) {
            val nextMatchNumber = matches.size + 1
            val roundMatches = generateSingleRound(currentRound, courts, nextMatchNumber)

            if (roundMatches.isEmpty()) {
                // Ingen flere kampe kan genereres
                break
            }

            matches.addAll(roundMatches)
            currentRound++
            safetyCounter++
        }

        if (safetyCounter >= maxRounds) {
            println("Advarsel: Stoppede efter $maxRounds runder for at undgå uendelig loop")
        }

        val newMatches = matches.size - startMatchCount
        println("Genereret $newMatches nye kampe. Total: ${matches.size} kampe i ${currentRound - 1} runder med $courts bane(r) for ${players.size} spillere")
    }

    private fun generateSingleRound(roundNumber: Int, courts: Int, startingMatchNumber: Int): List<Match> {
        val roundMatches = mutableListOf<Match>()
        val usedPlayers = mutableSetOf<String>()

        // Få prioriterede kandidat-kampe
        val candidates = generateMatchCandidates(usedPlayers)

        while (roundMatches.size < courts && candidates.isNotEmpty()) {
            // Find bedste kamp der ikke bruger allerede brugte spillere
            val bestMatch = candidates
                .filter { candidate ->
                    candidate.players.none { it in usedPlayers }
                }
                .maxByOrNull { it.score }

            if (bestMatch == null) break

            candidates.remove(bestMatch)

            val match = Match(
                roundNumber = roundNumber,
                courtNumber = roundMatches.size + 1,
                team1Player1 = bestMatch.t1p1,
                team1Player2 = bestMatch.t1p2,
                team2Player1 = bestMatch.t2p1,
                team2Player2 = bestMatch.t2p2
            )

            roundMatches.add(match)
            usedPlayers.addAll(bestMatch.players)

            // Opdater tracking
            updateTracking(bestMatch)
        }

        return roundMatches
    }

    private data class MatchCandidate(
        val t1p1: Player,
        val t1p2: Player,
        val t2p1: Player,
        val t2p2: Player,
        val score: Int,
        val players: Set<String>
    )

    private fun generateMatchCandidates(excludePlayers: Set<String>): MutableList<MatchCandidate> {
        val candidates = mutableListOf<MatchCandidate>()
        val availablePlayers = players.filter { it.id !in excludePlayers }

        if (availablePlayers.size < 4) return candidates

        // Generer alle mulige 4-spiller kombinationer
        for (i in availablePlayers.indices) {
            for (j in i + 1 until availablePlayers.size) {
                for (k in j + 1 until availablePlayers.size) {
                    for (l in k + 1 until availablePlayers.size) {
                        val fourPlayers = listOf(
                            availablePlayers[i],
                            availablePlayers[j],
                            availablePlayers[k],
                            availablePlayers[l]
                        )

                        // Find den bedste måde at opdele disse 4 spillere på 2 hold
                        val bestConfig = findBestTeamConfiguration(fourPlayers)
                        if (bestConfig != null) {
                            candidates.add(bestConfig)
                        }
                    }
                }
            }
        }

        return candidates
    }

    private fun findBestTeamConfiguration(fourPlayers: List<Player>): MatchCandidate? {
        val (a, b, c, d) = fourPlayers

        // Der er 3 måder at dele 4 spillere i 2 hold:
        // (a,b) vs (c,d), (a,c) vs (b,d), (a,d) vs (b,c)
        val configurations = listOf(
            listOf(a, b, c, d), // (a,b) vs (c,d)
            listOf(a, c, b, d), // (a,c) vs (b,d)
            listOf(a, d, b, c)  // (a,d) vs (b,c)
        )

        return configurations.map { (t1p1, t1p2, t2p1, t2p2) ->
            val score = calculateMatchScore(t1p1, t1p2, t2p1, t2p2)
            MatchCandidate(
                t1p1, t1p2, t2p1, t2p2, score,
                setOf(t1p1.id, t1p2.id, t2p1.id, t2p2.id)
            )
        }.maxByOrNull { it.score }
    }

    private fun calculateMatchScore(t1p1: Player, t1p2: Player, t2p1: Player, t2p2: Player): Int {
        var score = 0

        // Prioritet 1: Nye modstander-kombinationer (højest prioritet)
        val team1 = listOf(t1p1, t1p2)
        val team2 = listOf(t2p1, t2p2)

        for (p1 in team1) {
            for (p2 in team2) {
                if (playedAgainst[p1.id]?.contains(p2.id) != true) {
                    score += 1000 // Høj bonus for ny modstander
                }
            }
        }

        // Prioritet 2: Undgå genbrugte makkerpar
        val pair1 = setOf(t1p1.id, t1p2.id)
        val pair2 = setOf(t2p1.id, t2p2.id)
        val pair1Count = partnerCount[pair1] ?: 0
        val pair2Count = partnerCount[pair2] ?: 0

        // Straf for genbrugte par (jo flere gange brugt, jo større straf)
        score -= (pair1Count + pair2Count) * 100

        // Bonus for helt nye par
        if (pair1Count == 0) score += 50
        if (pair2Count == 0) score += 50

        // Prioritet 3: Balancer antal kampe per spiller
        val allPlayers = listOf(t1p1, t1p2, t2p1, t2p2)
        val minMatches = matchCount.values.minOrNull() ?: 0

        // Bonus for spillere med færrest kampe
        for (player in allPlayers) {
            val playerMatches = matchCount[player.id] ?: 0
            if (playerMatches == minMatches) {
                score += 25
            }
        }

        return score
    }

    private fun updateTracking(candidate: MatchCandidate) {
        val (t1p1, t1p2, t2p1, t2p2) = listOf(
            candidate.t1p1, candidate.t1p2, candidate.t2p1, candidate.t2p2
        )

        // Opdater modstander tracking
        recordOpponents(t1p1, t1p2, t2p1, t2p2)

        // Opdater partner tracking
        recordPartners(t1p1, t1p2)
        recordPartners(t2p1, t2p2)

        // Opdater match count
        listOf(t1p1, t1p2, t2p1, t2p2).forEach { player ->
            matchCount[player.id] = (matchCount[player.id] ?: 0) + 1
        }
    }

    private fun allOpponentPairsCovered(): Boolean {
        for (i in players.indices) {
            for (j in i + 1 until players.size) {
                val p1 = players[i]
                val p2 = players[j]
                if (playedAgainst[p1.id]?.contains(p2.id) != true) {
                    return false
                }
            }
        }
        return true
    }

    private fun allPlayersHaveEqualMatches(): Boolean {
        val counts = matchCount.values
        return counts.isNotEmpty() && counts.all { it == counts.first() && it > 0 }
    }
}