package dk.dtu.padelbattle.model.generator

import dk.dtu.padelbattle.model.Match
import dk.dtu.padelbattle.model.Player

/**
 * Generator for Americano-turneringer.
 * 
 * Americano-regler:
 * 1. Alle spillere får så mange forskellige makkere som muligt
 * 2. Stopper når ENTEN alle har spillet med alle ELLER alle har spillet MAX_MATCHES_PER_PLAYER kampe
 * 3. Modstandere varieres så meget som muligt
 * 4. Alle kampe genereres på forhånd (ikke runde-for-runde som Mexicano)
 */
class AmericanoGenerator : TournamentGenerator {

    companion object {
        const val MAX_MATCHES_PER_PLAYER = 8
    }

    // --- TRACKING STATE (genopbygges ved hver kørsel) ---
    private val partnerCount = mutableMapOf<Set<String>, Int>()
    private val matchCount = mutableMapOf<String, Int>()

    // Hjælpefunktion til konsistent par-nøgle
    private fun pairKey(id1: String, id2: String): Set<String> = setOf(id1, id2)

    override fun generateInitialMatches(
        players: List<Player>,
        numberOfCourts: Int
    ): List<Match> {
        // Reset tracking state
        resetTrackingData(players)

        val generatedMatches = mutableListOf<Match>()

        // Track hvilke par der allerede har spillet sammen
        val usedPartnerPairs = mutableSetOf<Set<String>>()

        // Generer alle mulige partnerpar for at tracke hvornår alle har spillet med alle
        val allPossiblePairs = generateAllPairs(players)
        val remainingPairs = allPossiblePairs.toMutableSet()

        // Track modstandere for at variere dem
        val opponentCount = mutableMapOf<Set<String>, Int>()

        val courts = numberOfCourts

        // Beregn hvor mange kampe hver spiller skal have
        // Det er minimum af (spillere - 1) og MAX_MATCHES_PER_PLAYER
        val targetMatchesPerPlayer = (players.size - 1).coerceAtMost(MAX_MATCHES_PER_PLAYER)

        val maxRounds = targetMatchesPerPlayer + 5 // Sikkerhedsmargin for balancering

        for (roundNumber in 1..maxRounds) {
            // Stop-betingelse 1: Alle har spillet med alle
            if (remainingPairs.isEmpty()) break

            // Stop-betingelse 2: Alle har nået target
            val minMatches = matchCount.values.minOrNull() ?: 0
            if (minMatches >= targetMatchesPerPlayer) break

            // Find spillere der kan spille flere kampe
            val eligiblePlayers = players.filter { (matchCount[it.id] ?: 0) < targetMatchesPerPlayer }
            if (eligiblePlayers.size < 4) break

            // Generer kampe for denne runde
            val roundMatches = generateRoundMatches(
                roundNumber = roundNumber,
                eligiblePlayers = eligiblePlayers,
                usedPartnerPairs = usedPartnerPairs,
                opponentCount = opponentCount,
                maxMatches = courts
            )

            if (roundMatches.isEmpty()) break

            generatedMatches.addAll(roundMatches)
            roundMatches.forEach { match ->
                updateTrackingForMatch(match, roundNumber)
                val pair1 = pairKey(match.team1Player1.id, match.team1Player2.id)
                val pair2 = pairKey(match.team2Player1.id, match.team2Player2.id)
                usedPartnerPairs.add(pair1)
                usedPartnerPairs.add(pair2)
                remainingPairs.remove(pair1)
                remainingPairs.remove(pair2)
                updateOpponentCount(match, opponentCount)
            }
        }

        // Fase 2: Balancer så alle har præcis samme antal kampe
        val balancingMatches = balanceMatchCounts(
            players = players,
            startRoundNumber = generatedMatches.maxOfOrNull { it.roundNumber } ?: 0,
            opponentCount = opponentCount,
            usedPartnerPairs = usedPartnerPairs,
            targetMatches = targetMatchesPerPlayer,
            numberOfCourts = courts
        )
        generatedMatches.addAll(balancingMatches)

        return generatedMatches
    }

    override fun generateExtensionMatches(
        players: List<Player>,
        existingMatches: List<Match>,
        numberOfCourts: Int
    ): List<Match> {
        // Genopbyg tracking fra eksisterende kampe
        rebuildTrackingFromMatches(players, existingMatches)

        val newMatches = mutableListOf<Match>()

        // Rebuild opponent count from existing matches
        val opponentCount = mutableMapOf<Set<String>, Int>()
        existingMatches.forEach { match ->
            updateOpponentCount(match, opponentCount)
        }

        val startRoundNumber = existingMatches.maxOfOrNull { it.roundNumber } ?: 0
        var roundNumber = startRoundNumber

        // Minimum 2 nye runder skal genereres
        val minNewRounds = 2

        val maxIterations = 100
        var iterations = 0

        // Fortsæt indtil vi har mindst 2 nye runder OG alle har lige mange kampe
        while (iterations < maxIterations) {
            iterations++

            val newRoundsAdded = roundNumber - startRoundNumber
            val currentMin = matchCount.values.minOrNull() ?: 0
            val currentMaxNow = matchCount.values.maxOrNull() ?: 0
            val allEqual = currentMin == currentMaxNow

            // Stop når vi har mindst 2 nye runder OG alle har lige mange kampe
            if (newRoundsAdded >= minNewRounds && allEqual) break

            // Find spillere sorteret efter færrest kampe først
            val sortedPlayers = players.sortedBy { matchCount[it.id] ?: 0 }
            val minMatches = matchCount[sortedPlayers.first().id] ?: 0

            // Find alle spillere der har færrest kampe
            val playersWithMin = sortedPlayers.filter { (matchCount[it.id] ?: 0) == minMatches }

            if (playersWithMin.size >= 4) {
                // Vi kan lave en kamp kun med spillere der har færrest kampe
                roundNumber++
                val fourPlayers = playersWithMin.take(4)
                val match = createBalancingMatch(roundNumber, 1, fourPlayers, opponentCount)
                newMatches.add(match)
                updateTrackingForMatch(match, roundNumber)
                updateOpponentCount(match, opponentCount)
            } else {
                // Vi har brug for filler-spillere fra dem med næst-færrest kampe
                val fillersNeeded = 4 - playersWithMin.size
                val fillerCandidates = sortedPlayers
                    .filter { (matchCount[it.id] ?: 0) > minMatches }
                    .take(fillersNeeded)

                if (playersWithMin.size + fillerCandidates.size >= 4) {
                    roundNumber++
                    val fourPlayers = (playersWithMin + fillerCandidates).take(4)
                    val match = createBalancingMatch(roundNumber, 1, fourPlayers, opponentCount)
                    newMatches.add(match)
                    updateTrackingForMatch(match, roundNumber)
                    updateOpponentCount(match, opponentCount)
                } else {
                    // Ikke nok spillere - stop
                    break
                }
            }
        }

        return newMatches
    }

    // --- PRIVATE HELPERS ---

    private fun resetTrackingData(players: List<Player>) {
        partnerCount.clear()
        matchCount.clear()

        players.forEach { player ->
            matchCount[player.id] = 0
        }
    }

    private fun rebuildTrackingFromMatches(players: List<Player>, matches: List<Match>) {
        resetTrackingData(players)

        matches.forEach { match ->
            val t1p1 = match.team1Player1
            val t1p2 = match.team1Player2
            val t2p1 = match.team2Player1
            val t2p2 = match.team2Player2

            // Registrer partners
            incrementPartner(t1p1, t1p2)
            incrementPartner(t2p1, t2p2)

            // Kun tæl spillede kampe for matchCount
            if (match.isPlayed) {
                listOf(t1p1, t1p2, t2p1, t2p2).forEach { player ->
                    matchCount[player.id] = (matchCount[player.id] ?: 0) + 1
                }
            }
        }
    }

    private fun generateAllPairs(players: List<Player>): Set<Set<String>> {
        val pairs = mutableSetOf<Set<String>>()
        for (i in players.indices) {
            for (j in i + 1 until players.size) {
                pairs.add(setOf(players[i].id, players[j].id))
            }
        }
        return pairs
    }

    private fun generateRoundMatches(
        roundNumber: Int,
        eligiblePlayers: List<Player>,
        usedPartnerPairs: Set<Set<String>>,
        opponentCount: Map<Set<String>, Int>,
        maxMatches: Int
    ): List<Match> {
        val roundMatches = mutableListOf<Match>()
        val usedInRound = mutableSetOf<String>()

        // Sorter spillere efter færrest kampe først
        val sortedPlayers = eligiblePlayers
            .filter { (matchCount[it.id] ?: 0) < MAX_MATCHES_PER_PLAYER }
            .sortedBy { matchCount[it.id] ?: 0 }

        while (roundMatches.size < maxMatches) {
            val available = sortedPlayers.filter { it.id !in usedInRound }
            if (available.size < 4) break

            // Find det bedste sæt af 4 spillere for en kamp
            val matchPlayers = findBestFourPlayers(
                available = available,
                usedPartnerPairs = usedPartnerPairs,
                opponentCount = opponentCount
            )

            if (matchPlayers == null) break

            val (p1, p2, p3, p4) = matchPlayers

            val match = Match(
                roundNumber = roundNumber,
                courtNumber = roundMatches.size + 1,
                team1Player1 = p1,
                team1Player2 = p2,
                team2Player1 = p3,
                team2Player2 = p4
            )

            roundMatches.add(match)
            usedInRound.addAll(listOf(p1.id, p2.id, p3.id, p4.id))
        }

        return roundMatches
    }

    private fun findBestFourPlayers(
        available: List<Player>,
        usedPartnerPairs: Set<Set<String>>,
        opponentCount: Map<Set<String>, Int>
    ): List<Player>? {
        if (available.size < 4) return null

        var bestConfig: List<Player>? = null
        var bestScore = Int.MAX_VALUE

        // Tag de første 8 spillere (for at begrænse søgning)
        val candidates = available.take(8.coerceAtMost(available.size))

        // Prøv alle kombinationer af 4 spillere
        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                for (k in j + 1 until candidates.size) {
                    for (l in k + 1 until candidates.size) {
                        val four = listOf(candidates[i], candidates[j], candidates[k], candidates[l])

                        // Find den bedste holdfordeling for disse 4
                        val bestTeamConfig = findBestTeamConfiguration(four, usedPartnerPairs, opponentCount)

                        if (bestTeamConfig.second < bestScore) {
                            bestScore = bestTeamConfig.second
                            bestConfig = bestTeamConfig.first
                        }
                    }
                }
            }
        }

        return bestConfig
    }

    private fun findBestTeamConfiguration(
        fourPlayers: List<Player>,
        usedPartnerPairs: Set<Set<String>>,
        opponentCount: Map<Set<String>, Int>
    ): Pair<List<Player>, Int> {
        var bestConfig = fourPlayers
        var bestScore = Int.MAX_VALUE

        // Prøv alle 3 mulige holdfordelinger
        val configs = listOf(
            listOf(0, 1, 2, 3), // (0,1) vs (2,3)
            listOf(0, 2, 1, 3), // (0,2) vs (1,3)
            listOf(0, 3, 1, 2)  // (0,3) vs (1,2)
        )

        for (config in configs) {
            val p1 = fourPlayers[config[0]]
            val p2 = fourPlayers[config[1]]
            val p3 = fourPlayers[config[2]]
            val p4 = fourPlayers[config[3]]

            var score = 0

            // Stor straf for gentagne partnerskaber
            val pair12 = pairKey(p1.id, p2.id)
            val pair34 = pairKey(p3.id, p4.id)
            if (pair12 in usedPartnerPairs) score += 10000
            if (pair34 in usedPartnerPairs) score += 10000

            // Mindre straf for gentagne modstandere
            score += (opponentCount[pairKey(p1.id, p3.id)] ?: 0) * 100
            score += (opponentCount[pairKey(p1.id, p4.id)] ?: 0) * 100
            score += (opponentCount[pairKey(p2.id, p3.id)] ?: 0) * 100
            score += (opponentCount[pairKey(p2.id, p4.id)] ?: 0) * 100

            // Lille bonus for at balancere kampantal
            score += (matchCount[p1.id] ?: 0) + (matchCount[p2.id] ?: 0) +
                    (matchCount[p3.id] ?: 0) + (matchCount[p4.id] ?: 0)

            if (score < bestScore) {
                bestScore = score
                bestConfig = listOf(p1, p2, p3, p4)
            }
        }

        return Pair(bestConfig, bestScore)
    }

    private fun balanceMatchCounts(
        players: List<Player>,
        startRoundNumber: Int,
        opponentCount: MutableMap<Set<String>, Int>,
        usedPartnerPairs: MutableSet<Set<String>>,
        targetMatches: Int,
        numberOfCourts: Int
    ): List<Match> {
        val balancingMatches = mutableListOf<Match>()
        var roundNumber = startRoundNumber
        val maxIterations = 10
        var iterations = 0

        while (!allPlayersHaveEqualMatches(players) && iterations < maxIterations) {
            iterations++

            val maxMatches = (matchCount.values.maxOrNull() ?: 0).coerceAtMost(targetMatches)

            // Find spillere der har færre kampe end max
            val playersNeedingMatches = players
                .filter {
                    val count = matchCount[it.id] ?: 0
                    count < maxMatches && count < targetMatches
                }
                .sortedBy { matchCount[it.id] ?: 0 }

            if (playersNeedingMatches.size < 4) break

            roundNumber++
            val matchesToCreate = (playersNeedingMatches.size / 4).coerceIn(1, numberOfCourts)

            val usedInRound = mutableSetOf<String>()
            for (m in 0 until matchesToCreate) {
                val available = playersNeedingMatches.filter { it.id !in usedInRound }
                if (available.size < 4) break

                val fourPlayers = available.take(4)
                val (config, _) = findBestTeamConfiguration(fourPlayers, usedPartnerPairs, opponentCount)

                val match = Match(
                    roundNumber = roundNumber,
                    courtNumber = m + 1,
                    team1Player1 = config[0],
                    team1Player2 = config[1],
                    team2Player1 = config[2],
                    team2Player2 = config[3]
                )

                balancingMatches.add(match)
                updateTrackingForMatch(match, roundNumber)
                usedPartnerPairs.add(pairKey(config[0].id, config[1].id))
                usedPartnerPairs.add(pairKey(config[2].id, config[3].id))
                updateOpponentCount(match, opponentCount)
                fourPlayers.forEach { usedInRound.add(it.id) }
            }
        }

        return balancingMatches
    }

    private fun allPlayersHaveEqualMatches(players: List<Player>): Boolean {
        if (players.isEmpty()) return true
        val firstCount = matchCount[players.first().id] ?: 0
        return players.all { (matchCount[it.id] ?: 0) == firstCount }
    }

    private fun updateOpponentCount(match: Match, opponentCount: MutableMap<Set<String>, Int>) {
        val team1 = listOf(match.team1Player1, match.team1Player2)
        val team2 = listOf(match.team2Player1, match.team2Player2)

        for (p1 in team1) {
            for (p2 in team2) {
                val key = pairKey(p1.id, p2.id)
                opponentCount[key] = (opponentCount[key] ?: 0) + 1
            }
        }
    }

    private fun createBalancingMatch(
        roundNumber: Int,
        courtNumber: Int,
        fourPlayers: List<Player>,
        opponentCount: Map<Set<String>, Int>
    ): Match {
        var bestConfig: List<Player>? = null
        var bestScore = Int.MAX_VALUE

        val configs = listOf(
            listOf(0, 1, 2, 3),
            listOf(0, 2, 1, 3),
            listOf(0, 3, 1, 2)
        )

        for (config in configs) {
            val p1 = fourPlayers[config[0]]
            val p2 = fourPlayers[config[1]]
            val p3 = fourPlayers[config[2]]
            val p4 = fourPlayers[config[3]]

            var score = 0

            // Straf for gentagne partnerskaber
            score += (partnerCount[pairKey(p1.id, p2.id)] ?: 0) * 1000
            score += (partnerCount[pairKey(p3.id, p4.id)] ?: 0) * 1000

            // Straf for gentagne modstandere
            score += (opponentCount[pairKey(p1.id, p3.id)] ?: 0) * 10
            score += (opponentCount[pairKey(p1.id, p4.id)] ?: 0) * 10
            score += (opponentCount[pairKey(p2.id, p3.id)] ?: 0) * 10
            score += (opponentCount[pairKey(p2.id, p4.id)] ?: 0) * 10

            if (score < bestScore) {
                bestScore = score
                bestConfig = listOf(p1, p2, p3, p4)
            }
        }

        val selectedPlayers = bestConfig ?: fourPlayers
        return Match(
            roundNumber = roundNumber,
            courtNumber = courtNumber,
            team1Player1 = selectedPlayers[0],
            team1Player2 = selectedPlayers[1],
            team2Player1 = selectedPlayers[2],
            team2Player2 = selectedPlayers[3]
        )
    }

    private fun updateTrackingForMatch(match: Match, roundNumber: Int) {
        val matchPlayers = listOf(
            match.team1Player1,
            match.team1Player2,
            match.team2Player1,
            match.team2Player2
        )

        matchPlayers.forEach { p ->
            matchCount[p.id] = (matchCount[p.id] ?: 0) + 1
        }

        incrementPartner(match.team1Player1, match.team1Player2)
        incrementPartner(match.team2Player1, match.team2Player2)
    }

    private fun incrementPartner(p1: Player, p2: Player) {
        val key = pairKey(p1.id, p2.id)
        partnerCount[key] = (partnerCount[key] ?: 0) + 1
    }
}
