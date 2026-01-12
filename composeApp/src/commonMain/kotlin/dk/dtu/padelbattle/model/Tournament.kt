package dk.dtu.padelbattle.model

import dk.dtu.padelbattle.model.utils.generateId
import kotlin.random.Random

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

    // --- TRACKING DATA ---
    private val playedAgainst = mutableMapOf<String, MutableList<String>>()
    private val partnerCount = mutableMapOf<Set<String>, Int>()
    private val matchCount = mutableMapOf<String, Int>()
    // Tracker hvornår spilleren sidst spillede (lavere = længere siden)
    private val lastPlayedRound = mutableMapOf<String, Int>()

    // --- PUBLIC API ---

    fun getMaxCourts(): Int = (players.size / 4).coerceAtLeast(1)

    /**
     * Tjekker om der er nogle kampe der allerede er blevet spillet.
     * Returnerer true hvis mindst én kamp har isPlayed = true
     */
    fun hasPlayedMatches(): Boolean = matches.any { it.isPlayed }

    /**
     * Returnerer det faktiske antal baner der vil blive brugt.
     * Dette er minimum af numberOfCourts og getMaxCourts()
     */
    fun getEffectiveCourts(): Int = numberOfCourts.coerceIn(1, getMaxCourts())

    fun startTournament(): Boolean {
        if (!validatePlayerCount()) return false

        matches.clear()
        resetTrackingData()

        return when (type) {
            TournamentType.AMERICANO -> {
                generateMinimalAmericanoTournament()
                true
            }
            TournamentType.MEXICANO -> false
        }
    }

    fun extendTournament(): Boolean {
        if (!validatePlayerCount()) return false
        if (matches.isEmpty()) return startTournament()

        return when (type) {
            TournamentType.AMERICANO -> {
                extendAmericanoTournament()
                true
            }
            TournamentType.MEXICANO -> false
        }
    }

    // --- PRIVATE HELPERS ---

    private fun validatePlayerCount(): Boolean {
        if (players.size < 4) throw IllegalStateException("Fejl: Der skal være mindst 4 spillere.")
        if (players.size > 16) throw IllegalStateException("Fejl: Maksimalt 16 spillere understøttes.")
        return true
    }

    private fun resetTrackingData() {
        playedAgainst.clear()
        partnerCount.clear()
        matchCount.clear()
        lastPlayedRound.clear()

        players.forEach { player ->
            playedAgainst[player.id] = mutableListOf()
            matchCount[player.id] = 0
            lastPlayedRound[player.id] = 0
        }
    }

    private fun rebuildTrackingFromMatches() {
        resetTrackingData()

        matches.forEach { match ->
            val t1p1 = match.team1Player1
            val t1p2 = match.team1Player2
            val t2p1 = match.team2Player1
            val t2p2 = match.team2Player2

            recordOpponents(t1p1, t1p2, t2p1, t2p2)
            recordPartners(t1p1, t1p2)
            recordPartners(t2p1, t2p2)

            listOf(t1p1, t1p2, t2p1, t2p2).forEach { player ->
                matchCount[player.id] = (matchCount[player.id] ?: 0) + 1
                lastPlayedRound[player.id] = match.roundNumber
            }
        }
    }

    private fun recordOpponents(t1p1: Player, t1p2: Player, t2p1: Player, t2p2: Player) {
        for (p1 in listOf(t1p1, t1p2)) {
            for (p2 in listOf(t2p1, t2p2)) {
                playedAgainst[p1.id]?.add(p2.id)
                playedAgainst[p2.id]?.add(p1.id)
            }
        }
    }

    private fun recordPartners(p1: Player, p2: Player) {
        val pair = setOf(p1.id, p2.id)
        partnerCount[pair] = (partnerCount[pair] ?: 0) + 1
    }

    private fun allPlayersHaveEqualMatches(): Boolean {
        if (players.isEmpty()) return true
        val firstCount = matchCount[players.first().id] ?: 0
        return players.all { (matchCount[it.id] ?: 0) == firstCount }
    }

    // --- MINIMAL AMERICANO ALGORITHM ---

    /**
     * Genererer den mindst mulige turnering hvor:
     * 1. Alle spillere har spillet sammen med alle andre mindst én gang
     * 2. Alle spillere ender med samme antal kampe
     * 3. Spillere der har siddet over længst tid prioriteres
     */
    private fun generateMinimalAmericanoTournament() {
        val courts = getEffectiveCourts()
        val playersPerRound = courts * 4

        // Generer alle nødvendige partnerpar
        val allRequiredPairs = generateAllPairs()
        val remainingPairs = allRequiredPairs.toMutableSet()

        var roundNumber = 0
        val maxIterations = 500

        // Fase 1: Dæk alle partnerpar med mindst mulige kampe
        while (remainingPairs.isNotEmpty() && roundNumber < maxIterations) {
            roundNumber++

            // Vælg aktive spillere baseret på hvem der har siddet over længst
            val activePlayers = selectActivePlayersForRound(playersPerRound, roundNumber)

            if (activePlayers.size < 4) break

            // Find optimale kampe der dækker flest mulige nye par
            val roundMatches = findMatchesCoveringMostPairs(
                roundNumber = roundNumber,
                activePlayers = activePlayers,
                remainingPairs = remainingPairs
            )

            if (roundMatches.isEmpty()) {
                // Ingen gode kampe fundet, prøv med andre spillere
                continue
            }

            matches.addAll(roundMatches)
            roundMatches.forEach { match ->
                updateTracking(match, roundNumber)
                remainingPairs.remove(setOf(match.team1Player1.id, match.team1Player2.id))
                remainingPairs.remove(setOf(match.team2Player1.id, match.team2Player2.id))
            }
        }

        // Fase 2: Balancer så alle har samme antal kampe
        balanceMatchCounts(roundNumber)
    }

    /**
     * Udvider turneringen med flere runder, balanceret
     */
    private fun extendAmericanoTournament() {
        rebuildTrackingFromMatches()

        val courts = getEffectiveCourts()
        val playersPerRound = courts * 4
        var roundNumber = matches.maxOfOrNull { it.roundNumber } ?: 0

        // Generer én ekstra runde for hver spiller (balanceret)
        val targetMatches = (matchCount.values.maxOrNull() ?: 0) + 1
        val maxIterations = 100
        var iterations = 0

        while (iterations < maxIterations) {
            val minMatches = matchCount.values.minOrNull() ?: 0
            if (minMatches >= targetMatches) break

            roundNumber++
            iterations++

            val activePlayers = selectActivePlayersForRound(playersPerRound, roundNumber)
            if (activePlayers.size < 4) break

            val roundMatches = findOptimalRoundConfiguration(
                roundNumber = roundNumber,
                activePlayers = activePlayers
            )

            if (roundMatches.isEmpty()) continue

            matches.addAll(roundMatches)
            roundMatches.forEach { match ->
                updateTracking(match, roundNumber)
            }
        }

        // Sørg for at alle har samme antal kampe
        balanceMatchCounts(roundNumber)
    }

    /**
     * Genererer alle unikke par af spillere
     */
    private fun generateAllPairs(): Set<Set<String>> {
        val pairs = mutableSetOf<Set<String>>()
        for (i in players.indices) {
            for (j in i + 1 until players.size) {
                pairs.add(setOf(players[i].id, players[j].id))
            }
        }
        return pairs
    }

    /**
     * Vælger spillere til en runde baseret på:
     * 1. Hvem der har færrest kampe
     * 2. Hvem der har siddet over længst tid
     */
    @Suppress("UNUSED_PARAMETER")
    private fun selectActivePlayersForRound(playersNeeded: Int, currentRound: Int): List<Player> {
        return players
            .sortedWith(
                compareBy<Player> { matchCount[it.id] ?: 0 }
                    .thenBy { lastPlayedRound[it.id] ?: 0 }
                    .thenBy { Random.nextInt() }
            )
            .take(playersNeeded.coerceAtMost(players.size))
    }

    /**
     * Finder kampe der dækker flest mulige nye partnerpar
     */
    private fun findMatchesCoveringMostPairs(
        roundNumber: Int,
        activePlayers: List<Player>,
        remainingPairs: Set<Set<String>>
    ): List<Match> {
        var bestMatches: List<Match> = emptyList()
        var bestScore = Int.MIN_VALUE

        fun backtrack(
            remainingPlayers: MutableList<Player>,
            currentMatches: List<Match>,
            coveredNewPairs: Int
        ) {
            if (remainingPlayers.size < 4) {
                if (coveredNewPairs > bestScore) {
                    bestScore = coveredNewPairs
                    bestMatches = currentMatches
                }
                return
            }

            val p1 = remainingPlayers[0]

            for (i in 1 until remainingPlayers.size) {
                val p2 = remainingPlayers[i]
                val pair1 = setOf(p1.id, p2.id)
                val pair1IsNew = pair1 in remainingPairs

                for (j in 1 until remainingPlayers.size) {
                    if (j == i) continue
                    val p3 = remainingPlayers[j]

                    for (k in j + 1 until remainingPlayers.size) {
                        if (k == i) continue
                        val p4 = remainingPlayers[k]

                        val pair2 = setOf(p3.id, p4.id)
                        val pair2IsNew = pair2 in remainingPairs

                        val newPairsInMatch = (if (pair1IsNew) 1 else 0) + (if (pair2IsNew) 1 else 0)

                        // Prioriter kampe med nye par
                        if (currentMatches.isEmpty() && newPairsInMatch == 0) continue

                        val nextRemaining = remainingPlayers.toMutableList().apply {
                            removeAll(listOf(p1, p2, p3, p4))
                        }

                        val newMatch = Match(
                            roundNumber = roundNumber,
                            courtNumber = currentMatches.size + 1,
                            team1Player1 = p1, team1Player2 = p2,
                            team2Player1 = p3, team2Player2 = p4
                        )

                        backtrack(
                            nextRemaining,
                            currentMatches + newMatch,
                            coveredNewPairs + newPairsInMatch
                        )
                    }
                }
            }
        }

        backtrack(activePlayers.shuffled().toMutableList(), emptyList(), 0)
        return bestMatches
    }

    /**
     * Balancerer så alle spillere har samme antal kampe
     */
    private fun balanceMatchCounts(startRoundNumber: Int) {
        var roundNumber = startRoundNumber
        val maxIterations = 100
        var iterations = 0

        while (!allPlayersHaveEqualMatches() && iterations < maxIterations) {
            roundNumber++
            iterations++

            val minMatches = matchCount.values.minOrNull() ?: 0

            // Find spillere der har færrest kampe
            val playersNeedingMatches = players
                .filter { (matchCount[it.id] ?: 0) == minMatches }
                .sortedBy { lastPlayedRound[it.id] ?: 0 }

            if (playersNeedingMatches.size < 4) {
                // Tilføj spillere med næstfærrest kampe
                val additionalPlayers = players
                    .filter { (matchCount[it.id] ?: 0) == minMatches + 1 }
                    .sortedBy { lastPlayedRound[it.id] ?: 0 }
                    .take(4 - playersNeedingMatches.size)

                val activePlayers = playersNeedingMatches + additionalPlayers
                if (activePlayers.size < 4) break

                val roundMatches = findOptimalRoundConfiguration(
                    roundNumber = roundNumber,
                    activePlayers = activePlayers.take(4)
                )

                if (roundMatches.isNotEmpty()) {
                    matches.addAll(roundMatches)
                    roundMatches.forEach { match ->
                        updateTracking(match, roundNumber)
                    }
                }
            } else {
                // Lav kampe med kun de spillere der har færrest
                val courts = getEffectiveCourts()
                val playersToUse = playersNeedingMatches.take(courts * 4)

                val roundMatches = findOptimalRoundConfiguration(
                    roundNumber = roundNumber,
                    activePlayers = playersToUse
                )

                if (roundMatches.isNotEmpty()) {
                    matches.addAll(roundMatches)
                    roundMatches.forEach { match ->
                        updateTracking(match, roundNumber)
                    }
                }
            }
        }
    }

    private fun findOptimalRoundConfiguration(
        roundNumber: Int,
        activePlayers: List<Player>
    ): List<Match> {
        if (activePlayers.size < 4) return emptyList()

        var bestMatches: List<Match> = emptyList()
        var minCost = Int.MAX_VALUE

        fun backtrack(remainingPlayers: MutableList<Player>, currentMatches: List<Match>, currentCost: Int) {
            if (currentCost >= minCost) return

            if (remainingPlayers.size < 4) {
                if (currentCost < minCost && currentMatches.isNotEmpty()) {
                    minCost = currentCost
                    bestMatches = currentMatches
                }
                return
            }

            val p1 = remainingPlayers[0]

            for (i in 1 until remainingPlayers.size) {
                val p2 = remainingPlayers[i]

                val pairKey = setOf(p1.id, p2.id)
                val partnersCount = partnerCount[pairKey] ?: 0
                val partnerCost = partnersCount * partnersCount * 1000

                for (j in 1 until remainingPlayers.size) {
                    if (j == i) continue
                    val p3 = remainingPlayers[j]

                    for (k in j + 1 until remainingPlayers.size) {
                        if (k == i) continue
                        val p4 = remainingPlayers[k]

                        val matchCost = partnerCost +
                                calculatePairCost(p3, p4) +
                                calculateOpponentCost(p1, p2, p3, p4)

                        val nextRemaining = remainingPlayers.toMutableList().apply {
                            removeAll(listOf(p1, p2, p3, p4))
                        }

                        val newMatch = Match(
                            roundNumber = roundNumber,
                            courtNumber = currentMatches.size + 1,
                            team1Player1 = p1, team1Player2 = p2,
                            team2Player1 = p3, team2Player2 = p4
                        )

                        backtrack(nextRemaining, currentMatches + newMatch, currentCost + matchCost)
                        if (minCost == 0) return
                    }
                }
            }
        }

        backtrack(activePlayers.shuffled().toMutableList(), emptyList(), 0)
        return bestMatches
    }

    private fun calculatePairCost(p1: Player, p2: Player): Int {
        val count = partnerCount[setOf(p1.id, p2.id)] ?: 0
        return count * count * 1000
    }

    private fun calculateOpponentCost(t1p1: Player, t1p2: Player, t2p1: Player, t2p2: Player): Int {
        var score = 0
        val team1 = listOf(t1p1, t1p2)
        val team2 = listOf(t2p1, t2p2)

        for (p1 in team1) {
            for (p2 in team2) {
                val meetings = playedAgainst[p1.id]?.count { it == p2.id } ?: 0
                score += meetings
            }
        }
        return score
    }

    private fun updateTracking(match: Match, roundNumber: Int) {
        val t1p1 = match.team1Player1
        val t1p2 = match.team1Player2
        val t2p1 = match.team2Player1
        val t2p2 = match.team2Player2
        val matchPlayers = listOf(t1p1, t1p2, t2p1, t2p2)

        matchPlayers.forEach { p ->
            matchCount[p.id] = (matchCount[p.id] ?: 0) + 1
            lastPlayedRound[p.id] = roundNumber
        }

        incrementPartner(t1p1, t1p2)
        incrementPartner(t2p1, t2p2)

        listOf(t1p1, t1p2).forEach { p1 ->
            listOf(t2p1, t2p2).forEach { p2 ->
                playedAgainst.getOrPut(p1.id) { mutableListOf() }.add(p2.id)
                playedAgainst.getOrPut(p2.id) { mutableListOf() }.add(p1.id)
            }
        }
    }

    private fun incrementPartner(p1: Player, p2: Player) {
        val key = setOf(p1.id, p2.id)
        partnerCount[key] = (partnerCount[key] ?: 0) + 1
    }

    private fun allPlayersHavePartneredWithAll(): Boolean {
        val requiredPartners = players.size - 1
        return players.all { player ->
            val uniquePartners = partnerCount.keys
                .filter { it.contains(player.id) }
                .flatMap { it }
                .filter { it != player.id }
                .toSet()
            uniquePartners.size >= requiredPartners
        }
    }
}