package dk.dtu.padelbattle.model

import dk.dtu.padelbattle.model.utils.generateId
import kotlin.math.abs
import kotlin.random.Random

class Tournament(
    val id: String = generateId(),
    var name: String,
    val type: TournamentType,
    val dateCreated: Long,
    val numberOfCourts: Int = 1,
    val pointsPerMatch: Int = 16,
    val players: MutableList<Player> = mutableListOf(),
    val matches: MutableList<Match> = mutableListOf(),
    var isCompleted: Boolean = false
) {

    // --- TRACKING DATA ---
    // Ændret til MutableList for at kunne tælle antal opgør (vigtigt for forlængelse)
    private val playedAgainst = mutableMapOf<String, MutableList<String>>()
    private val partnerCount = mutableMapOf<Set<String>, Int>()
    private val matchCount = mutableMapOf<String, Int>()

    // --- PUBLIC API ---

    fun getMaxCourts(): Int = (players.size / 4).coerceAtLeast(1)

    fun getEffectiveCourts(): Int = numberOfCourts.coerceIn(1, getMaxCourts())

    fun startTournament(): Boolean {
        if (!validatePlayerCount()) return false

        matches.clear()
        resetTrackingData()

        return when (type) {
            TournamentType.AMERICANO -> {
                generateAmericanoRounds(untilAllHavePlayed = true)
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
                generateAmericanoRounds(untilAllHavePlayed = false)
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

        players.forEach { player ->
            playedAgainst[player.id] = mutableListOf() // Initialiser som liste
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

            recordOpponents(t1p1, t1p2, t2p1, t2p2)
            recordPartners(t1p1, t1p2)
            recordPartners(t2p1, t2p2)

            listOf(t1p1, t1p2, t2p1, t2p2).forEach { player ->
                matchCount[player.id] = (matchCount[player.id] ?: 0) + 1
            }
        }
    }

    private fun recordOpponents(t1p1: Player, t1p2: Player, t2p1: Player, t2p2: Player) {
        // Gemmer alle møder, så vi kan vægte 'antal gange mødt'
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

    // Manglende hjælpefunktion til at tjekke balance
    private fun allPlayersHaveEqualMatches(): Boolean {
        if (players.isEmpty()) return true
        // Tjek om alle spillere har samme antal kampe som den første spiller i listen
        val firstCount = matchCount[players.first().id] ?: 0
        return players.all { (matchCount[it.id] ?: 0) == firstCount }
    }

    // --- AMERICANO ALGORITHM ---

    private fun generateAmericanoRounds(untilAllHavePlayed: Boolean) {
        if (matches.isNotEmpty()) {
            rebuildTrackingFromMatches()
        }

        val courts = getEffectiveCourts()
        val playersPerRound = courts * 4

        var safetyCounter = 0
        val maxRounds = 200
        var roundsGenerated = 0

        while (safetyCounter < maxRounds) {
            val isBalanced = allPlayersHaveEqualMatches()
            val allPartnersMet = allPlayersHavePartneredWithAll()

            // Stop-betingelser
            if (roundsGenerated > 0) {
                if (untilAllHavePlayed) {
                    if (allPartnersMet && isBalanced) break
                } else {
                    if (isBalanced) break
                }
            }

            // 1. Udvælg spillere (dem med færrest kampe først)
            val activePlayers = players
                .sortedWith(compareBy<Player> { matchCount[it.id] ?: 0 }
                    .thenBy { Random.nextInt() })
                .take(playersPerRound)

            if (activePlayers.size < playersPerRound) break

            // 2. Find kampe
            val roundMatches = findOptimalRoundConfiguration(
                roundNumber = (matches.maxOfOrNull { it.roundNumber } ?: 0) + 1,
                activePlayers = activePlayers
            )

            if (roundMatches.isEmpty()) break

            matches.addAll(roundMatches)

            roundMatches.forEach { match ->
                updateTracking(match)
            }

            roundsGenerated++
            safetyCounter++
        }
    }

    private fun findOptimalRoundConfiguration(
        roundNumber: Int,
        activePlayers: List<Player>
    ): List<Match> {
        var bestMatches: List<Match> = emptyList()
        var minCost = Int.MAX_VALUE

        fun backtrack(remainingPlayers: MutableList<Player>, currentMatches: List<Match>, currentCost: Int) {
            if (currentCost >= minCost) return

            if (remainingPlayers.isEmpty()) {
                if (currentCost < minCost) {
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
                // Høj straf for at spille sammen igen
                val partnerCost = (partnersCount * partnersCount * 1000)

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
                // Nu hvor playedAgainst er en List, virker count() korrekt
                // til at straffe gentagne modstandere progressivt
                val meetings = playedAgainst[p1.id]?.count { it == p2.id } ?: 0
                score += meetings
            }
        }
        return score
    }

    private fun updateTracking(match: Match) {
        val t1p1 = match.team1Player1
        val t1p2 = match.team1Player2
        val t2p1 = match.team2Player1
        val t2p2 = match.team2Player2
        val players = listOf(t1p1, t1p2, t2p1, t2p2)

        players.forEach { p ->
            matchCount[p.id] = (matchCount[p.id] ?: 0) + 1
        }

        incrementPartner(t1p1, t1p2)
        incrementPartner(t2p1, t2p2)

        // Bruger nu lister i stedet for sets
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
                .toSet() // Sikrer at vi kun tæller unikke personer, selvom partnerCount keys kan være mange
            uniquePartners.size >= requiredPartners
        }
    }
}