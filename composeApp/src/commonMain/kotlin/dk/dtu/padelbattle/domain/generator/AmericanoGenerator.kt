package dk.dtu.padelbattle.domain.generator

import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.Player

/**
 * Generator for Americano-turneringer (stateless).
 * 
 * Americano-regler:
 * 1. Alle spillere får så mange forskellige makkere som muligt
 * 2. Stopper når ENTEN alle har spillet med alle ELLER alle har spillet MAX_MATCHES_PER_PLAYER kampe
 * 3. Modstandere varieres så meget som muligt
 * 4. Alle kampe genereres på forhånd (ikke runde-for-runde som Mexicano)
 * 
 * Denne generator er stateless - al tracking data beregnes fra input parameters.
 */
class AmericanoGenerator : TournamentGenerator {

    companion object {
        const val MAX_MATCHES_PER_PLAYER = 8
    }

    /**
     * Intern data class til tracking af spillerstatistik under generering.
     */
    private data class GeneratorState(
        val partnerCount: MutableMap<Set<String>, Int>,
        val matchCount: MutableMap<String, Int>,
        val usedPartnerPairs: MutableSet<Set<String>>,
        val opponentCount: MutableMap<Set<String>, Int>
    )

    // Hjælpefunktion til konsistent par-nøgle
    private fun pairKey(id1: String, id2: String): Set<String> = setOf(id1, id2)

    override fun generateInitialMatches(
        players: List<Player>,
        numberOfCourts: Int
    ): List<Match> {
        val state = createInitialState(players)
        val generatedMatches = mutableListOf<Match>()

        // Generer alle mulige partnerpar for at tracke hvornår alle har spillet med alle
        val remainingPairs = generateAllPairs(players).toMutableSet()

        // Beregn hvor mange kampe hver spiller skal have
        val targetMatchesPerPlayer = (players.size - 1).coerceAtMost(MAX_MATCHES_PER_PLAYER)
        val maxRounds = targetMatchesPerPlayer + 5 // Sikkerhedsmargin for balancering

        for (roundNumber in 1..maxRounds) {
            // Stop-betingelse 1: Alle har spillet med alle
            if (remainingPairs.isEmpty()) break

            // Stop-betingelse 2: Alle har nået target
            val minMatches = state.matchCount.values.minOrNull() ?: 0
            if (minMatches >= targetMatchesPerPlayer) break

            // Find spillere der kan spille flere kampe
            val eligiblePlayers = players.filter { (state.matchCount[it.id] ?: 0) < targetMatchesPerPlayer }
            if (eligiblePlayers.size < 4) break

            // Generer kampe for denne runde
            val roundMatches = generateRoundMatches(
                roundNumber = roundNumber,
                eligiblePlayers = eligiblePlayers,
                state = state,
                maxMatches = numberOfCourts
            )

            if (roundMatches.isEmpty()) break

            generatedMatches.addAll(roundMatches)
            roundMatches.forEach { match ->
                updateStateForMatch(match, state)
                val pair1 = pairKey(match.team1Player1.id, match.team1Player2.id)
                val pair2 = pairKey(match.team2Player1.id, match.team2Player2.id)
                remainingPairs.remove(pair1)
                remainingPairs.remove(pair2)
            }
        }

        // Fase 2: Balancer så alle har præcis samme antal kampe
        val balancingMatches = balanceMatchCounts(
            players = players,
            startRoundNumber = generatedMatches.maxOfOrNull { it.roundNumber } ?: 0,
            state = state,
            targetMatches = targetMatchesPerPlayer,
            numberOfCourts = numberOfCourts
        )
        generatedMatches.addAll(balancingMatches)

        return generatedMatches
    }

    override fun generateExtensionMatches(
        players: List<Player>,
        existingMatches: List<Match>,
        numberOfCourts: Int
    ): List<Match> {
        // Byg state fra eksisterende kampe
        val state = buildStateFromMatches(players, existingMatches)
        val newMatches = mutableListOf<Match>()

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
            val currentMin = state.matchCount.values.minOrNull() ?: 0
            val currentMaxNow = state.matchCount.values.maxOrNull() ?: 0
            val allEqual = currentMin == currentMaxNow

            // Stop når vi har mindst 2 nye runder OG alle har lige mange kampe
            if (newRoundsAdded >= minNewRounds && allEqual) break

            // Find spillere sorteret efter færrest kampe først
            val sortedPlayers = players.sortedBy { state.matchCount[it.id] ?: 0 }
            val minMatches = state.matchCount[sortedPlayers.first().id] ?: 0

            // Find alle spillere der har færrest kampe
            val playersWithMin = sortedPlayers.filter { (state.matchCount[it.id] ?: 0) == minMatches }

            if (playersWithMin.size >= 4) {
                // Vi kan lave en kamp kun med spillere der har færrest kampe
                roundNumber++
                val fourPlayers = playersWithMin.take(4)
                val match = createBalancingMatch(roundNumber, 1, fourPlayers, state)
                newMatches.add(match)
                updateStateForMatch(match, state)
            } else {
                // Vi har brug for filler-spillere fra dem med næst-færrest kampe
                val fillersNeeded = 4 - playersWithMin.size
                val fillerCandidates = sortedPlayers
                    .filter { (state.matchCount[it.id] ?: 0) > minMatches }
                    .take(fillersNeeded)

                if (playersWithMin.size + fillerCandidates.size >= 4) {
                    roundNumber++
                    val fourPlayers = (playersWithMin + fillerCandidates).take(4)
                    val match = createBalancingMatch(roundNumber, 1, fourPlayers, state)
                    newMatches.add(match)
                    updateStateForMatch(match, state)
                } else {
                    break
                }
            }
        }

        return newMatches
    }

    // --- PRIVATE HELPERS ---

    private fun createInitialState(players: List<Player>): GeneratorState {
        return GeneratorState(
            partnerCount = mutableMapOf(),
            matchCount = players.associate { it.id to 0 }.toMutableMap(),
            usedPartnerPairs = mutableSetOf(),
            opponentCount = mutableMapOf()
        )
    }

    private fun buildStateFromMatches(players: List<Player>, matches: List<Match>): GeneratorState {
        val state = createInitialState(players)

        matches.forEach { match ->
            val t1p1 = match.team1Player1
            val t1p2 = match.team1Player2
            val t2p1 = match.team2Player1
            val t2p2 = match.team2Player2

            // Registrer partners
            incrementPartner(t1p1, t1p2, state)
            incrementPartner(t2p1, t2p2, state)

            // Registrer brugte partner pairs
            state.usedPartnerPairs.add(pairKey(t1p1.id, t1p2.id))
            state.usedPartnerPairs.add(pairKey(t2p1.id, t2p2.id))

            // Registrer modstandere
            updateOpponentCount(match, state.opponentCount)

            // Kun tæl spillede kampe for matchCount
            if (match.isPlayed) {
                listOf(t1p1, t1p2, t2p1, t2p2).forEach { player ->
                    state.matchCount[player.id] = (state.matchCount[player.id] ?: 0) + 1
                }
            }
        }

        return state
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
        state: GeneratorState,
        maxMatches: Int
    ): List<Match> {
        val roundMatches = mutableListOf<Match>()
        val usedInRound = mutableSetOf<String>()

        // Sorter spillere efter færrest kampe først
        val sortedPlayers = eligiblePlayers
            .filter { (state.matchCount[it.id] ?: 0) < MAX_MATCHES_PER_PLAYER }
            .sortedBy { state.matchCount[it.id] ?: 0 }

        while (roundMatches.size < maxMatches) {
            val available = sortedPlayers.filter { it.id !in usedInRound }
            if (available.size < 4) break

            // Find det bedste sæt af 4 spillere for en kamp
            val matchPlayers = findBestFourPlayers(available, state)

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
        state: GeneratorState
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
                        val bestTeamConfig = findBestTeamConfiguration(four, state)

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
        state: GeneratorState
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
            if (pair12 in state.usedPartnerPairs) score += 10000
            if (pair34 in state.usedPartnerPairs) score += 10000

            // Mindre straf for gentagne modstandere
            score += (state.opponentCount[pairKey(p1.id, p3.id)] ?: 0) * 100
            score += (state.opponentCount[pairKey(p1.id, p4.id)] ?: 0) * 100
            score += (state.opponentCount[pairKey(p2.id, p3.id)] ?: 0) * 100
            score += (state.opponentCount[pairKey(p2.id, p4.id)] ?: 0) * 100

            // Lille bonus for at balancere kampantal
            score += (state.matchCount[p1.id] ?: 0) + (state.matchCount[p2.id] ?: 0) +
                    (state.matchCount[p3.id] ?: 0) + (state.matchCount[p4.id] ?: 0)

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
        state: GeneratorState,
        targetMatches: Int,
        numberOfCourts: Int
    ): List<Match> {
        val balancingMatches = mutableListOf<Match>()
        var roundNumber = startRoundNumber
        val maxIterations = 10
        var iterations = 0

        while (!allPlayersHaveEqualMatches(players, state) && iterations < maxIterations) {
            iterations++

            val maxMatches = (state.matchCount.values.maxOrNull() ?: 0).coerceAtMost(targetMatches)

            // Find spillere der har færre kampe end max
            val playersNeedingMatches = players
                .filter {
                    val count = state.matchCount[it.id] ?: 0
                    count < maxMatches && count < targetMatches
                }
                .sortedBy { state.matchCount[it.id] ?: 0 }

            if (playersNeedingMatches.size < 4) break

            roundNumber++
            val matchesToCreate = (playersNeedingMatches.size / 4).coerceIn(1, numberOfCourts)

            val usedInRound = mutableSetOf<String>()
            for (m in 0 until matchesToCreate) {
                val available = playersNeedingMatches.filter { it.id !in usedInRound }
                if (available.size < 4) break

                val fourPlayers = available.take(4)
                val (config, _) = findBestTeamConfiguration(fourPlayers, state)

                val match = Match(
                    roundNumber = roundNumber,
                    courtNumber = m + 1,
                    team1Player1 = config[0],
                    team1Player2 = config[1],
                    team2Player1 = config[2],
                    team2Player2 = config[3]
                )

                balancingMatches.add(match)
                updateStateForMatch(match, state)
                fourPlayers.forEach { usedInRound.add(it.id) }
            }
        }

        return balancingMatches
    }

    private fun allPlayersHaveEqualMatches(players: List<Player>, state: GeneratorState): Boolean {
        if (players.isEmpty()) return true
        val firstCount = state.matchCount[players.first().id] ?: 0
        return players.all { (state.matchCount[it.id] ?: 0) == firstCount }
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
        state: GeneratorState
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
            score += (state.partnerCount[pairKey(p1.id, p2.id)] ?: 0) * 1000
            score += (state.partnerCount[pairKey(p3.id, p4.id)] ?: 0) * 1000

            // Straf for gentagne modstandere
            score += (state.opponentCount[pairKey(p1.id, p3.id)] ?: 0) * 10
            score += (state.opponentCount[pairKey(p1.id, p4.id)] ?: 0) * 10
            score += (state.opponentCount[pairKey(p2.id, p3.id)] ?: 0) * 10
            score += (state.opponentCount[pairKey(p2.id, p4.id)] ?: 0) * 10

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

    private fun updateStateForMatch(match: Match, state: GeneratorState) {
        val matchPlayers = listOf(
            match.team1Player1,
            match.team1Player2,
            match.team2Player1,
            match.team2Player2
        )

        matchPlayers.forEach { p ->
            state.matchCount[p.id] = (state.matchCount[p.id] ?: 0) + 1
        }

        incrementPartner(match.team1Player1, match.team1Player2, state)
        incrementPartner(match.team2Player1, match.team2Player2, state)

        state.usedPartnerPairs.add(pairKey(match.team1Player1.id, match.team1Player2.id))
        state.usedPartnerPairs.add(pairKey(match.team2Player1.id, match.team2Player2.id))

        updateOpponentCount(match, state.opponentCount)
    }

    private fun incrementPartner(p1: Player, p2: Player, state: GeneratorState) {
        val key = pairKey(p1.id, p2.id)
        state.partnerCount[key] = (state.partnerCount[key] ?: 0) + 1
    }
}
