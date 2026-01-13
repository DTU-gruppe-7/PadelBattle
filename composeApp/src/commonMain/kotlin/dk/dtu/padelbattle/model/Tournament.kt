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
    private val partnerCount = mutableMapOf<Set<String>, Int>()
    private val opponentCountGlobal = mutableMapOf<Set<String>, Int>()
    private val matchCount = mutableMapOf<String, Int>()
    // Tracker hvornår spilleren sidst spillede (lavere = længere siden)
    private val lastPlayedRound = mutableMapOf<String, Int>()
    // Cache for hurtig spiller-opslag
    private val playerById: Map<String, Player> get() = players.associateBy { it.id }
    
    // Hjælpefunktion til konsistent par-nøgle
    private fun pairKey(id1: String, id2: String): Set<String> = setOf(id1, id2)

    // --- PUBLIC API ---

    fun getMaxCourts(): Int = (players.size / 4).coerceIn(1, MAX_COURTS)

    companion object {
        const val MAX_COURTS = 8
        const val MAX_PLAYERS = 32
        const val MIN_PLAYERS = 4
        /** Maksimalt antal kampe per spiller ved initial turneringsgenerering */
        const val MAX_MATCHES_PER_PLAYER = 8
    }

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
            TournamentType.MEXICANO -> {
                generateMexicanoRound(roundNumber = 1, isFirstRound = true)
                true
            }
        }
    }

    /**
     * Tjekker om Mexicano-kriterierne for afslutning er mødt.
     * Reglen: Alle skal have spillet mindst [minMatches] kampe OG alle skal have lige mange kampe.
     */
    fun isMexicanoFinished(minMatches: Int = 3): Boolean {
        // Hvis der er kampe der mangler at blive spillet, er vi ikke færdige
        if (matches.any { !it.isPlayed }) return false

        rebuildTrackingFromMatches() // Sikr at vores tællere er opdaterede

        // Tjek det laveste og højeste antal kampe
        val minPlayed = matchCount.values.minOrNull() ?: 0
        val maxPlayed = matchCount.values.maxOrNull() ?: 0
        
        // Alle skal have spillet mindst minMatches OG alle skal have lige mange kampe
        return minPlayed >= minMatches && minPlayed == maxPlayed
    }

    fun extendTournament(): Boolean {
        if (!validatePlayerCount()) return false

        // VIGTIGT: Når vi henter fra DB, er tracking data nulstillet.
        // Vi skal genopbygge historikken før vi kan lave nye kampe.
        rebuildTrackingFromMatches()

        if (matches.isEmpty()) return startTournament()

        return when (type) {
            TournamentType.AMERICANO -> {
                extendAmericanoTournament()
                true
            }
            TournamentType.MEXICANO -> {
                extendMexicanoTournament()
                true
            }
        }
    }

    /**
     * Udvider Mexicano-turneringen med 1 ny runde.
     * Mexicano genererer kun 1 runde ad gangen, da næste rundes hold
     * afhænger af ranglisten efter den nuværende runde er spillet.
     */
    private fun extendMexicanoTournament() {
        val lastRoundNumber = matches.maxOfOrNull { it.roundNumber } ?: 0
        
        // Generer næste Mexicano-runde baseret på nuværende rangliste
        generateMexicanoRound(lastRoundNumber + 1, isFirstRound = false)
    }

    // Husk at sikre dig, at generateMexicanoRound bruger "1+3 vs 2+4" logikken fra før:
    private fun generateMexicanoRound(roundNumber: Int, isFirstRound: Boolean) {
        val courts = getEffectiveCourts()
        val playersNeeded = courts * 4

        var activePlayers = selectActivePlayersForRound(playersNeeded, roundNumber)
        if (activePlayers.size < 4) return

        activePlayers = if (isFirstRound) {
            activePlayers.shuffled()
        } else {
            // Sorter efter point (højest først), derefter tilfældigt
            activePlayers.sortedWith(
                compareByDescending<Player> { it.totalPoints }
                    .thenBy { Random.nextInt() }
            )
        }

        for (i in 0 until courts) {
            val baseIdx = i * 4
            if (baseIdx + 4 <= activePlayers.size) {
                val p1 = activePlayers[baseIdx]
                val p2 = activePlayers[baseIdx + 1]
                val p3 = activePlayers[baseIdx + 2]
                val p4 = activePlayers[baseIdx + 3]

                // Regel: 1+3 mod 2+4
                val match = Match(
                    roundNumber = roundNumber,
                    courtNumber = i + 1,
                    team1Player1 = p1, team1Player2 = p3,
                    team2Player1 = p2, team2Player2 = p4
                )
                matches.add(match)
                updateTracking(match, roundNumber)
            }
        }
    }

    // --- PRIVATE HELPERS ---

    private fun validatePlayerCount(): Boolean {
        if (players.size < MIN_PLAYERS) throw IllegalStateException("Fejl: Der skal være mindst $MIN_PLAYERS spillere.")
        if (players.size > MAX_PLAYERS) throw IllegalStateException("Fejl: Maksimalt $MAX_PLAYERS spillere understøttes.")
        return true
    }

    private fun resetTrackingData() {
        partnerCount.clear()
        opponentCountGlobal.clear()
        matchCount.clear()
        lastPlayedRound.clear()

        players.forEach { player ->
            matchCount[player.id] = 0
            lastPlayedRound[player.id] = 0
        }
    }

    private fun rebuildTrackingFromMatches() {
        resetTrackingData()

        // Registrer alle kampe for tracking data
        matches.forEach { match ->
            val t1p1 = match.team1Player1
            val t1p2 = match.team1Player2
            val t2p1 = match.team2Player1
            val t2p2 = match.team2Player2

            // Registrer opponents og partners for alle kampe (også uafspillede)
            recordOpponents(t1p1, t1p2, t2p1, t2p2)
            recordPartners(t1p1, t1p2)
            recordPartners(t2p1, t2p2)

            // Kun tæl spillede kampe for matchCount og lastPlayedRound
            if (match.isPlayed) {
                listOf(t1p1, t1p2, t2p1, t2p2).forEach { player ->
                    matchCount[player.id] = (matchCount[player.id] ?: 0) + 1
                    lastPlayedRound[player.id] = match.roundNumber
                }
            }
        }
    }

    private fun recordOpponents(t1p1: Player, t1p2: Player, t2p1: Player, t2p2: Player) {
        val team1 = listOf(t1p1, t1p2)
        val team2 = listOf(t2p1, t2p2)
        for (p1 in team1) {
            for (p2 in team2) {
                val key = pairKey(p1.id, p2.id)
                opponentCountGlobal[key] = (opponentCountGlobal[key] ?: 0) + 1
            }
        }
    }

    private fun recordPartners(p1: Player, p2: Player) {
        val key = pairKey(p1.id, p2.id)
        partnerCount[key] = (partnerCount[key] ?: 0) + 1
    }

    private fun allPlayersHaveEqualMatches(): Boolean {
        if (players.isEmpty()) return true
        val firstCount = matchCount[players.first().id] ?: 0
        return players.all { (matchCount[it.id] ?: 0) == firstCount }
    }

    // --- MINIMAL AMERICANO ALGORITHM ---

    /**
     * Genererer en turnering hvor:
     * 1. Alle spillere får så mange forskellige makkere som muligt
     * 2. Stopper når ENTEN alle har spillet med alle ELLER alle har spillet MAX_MATCHES_PER_PLAYER kampe
     * 3. Modstandere varieres så meget som muligt
     *
     * Eksempler:
     * - 8 spillere: Stopper efter 7 kampe/spiller (alle har spillet med alle)
     * - 16 spillere: Stopper efter 8 kampe/spiller (max grænse)
     * - 32 spillere: Stopper efter 8 kampe/spiller (max grænse)
     */
    private fun generateMinimalAmericanoTournament() {
        // Track hvilke par der allerede har spillet sammen
        val usedPartnerPairs = mutableSetOf<Set<String>>()
        
        // Generer alle mulige partnerpar for at tracke hvornår alle har spillet med alle
        val allPossiblePairs = generateAllPairs()
        val remainingPairs = allPossiblePairs.toMutableSet()
        
        // Track modstandere for at variere dem
        val opponentCount = mutableMapOf<Set<String>, Int>()
        
        val courts = getEffectiveCourts()
        
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
            
            matches.addAll(roundMatches)
            roundMatches.forEach { match ->
                updateTracking(match, roundNumber)
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
        balanceMatchCountsWithLimit(
            startRoundNumber = matches.maxOfOrNull { it.roundNumber } ?: 0, 
            opponentCount = opponentCount, 
            usedPartnerPairs = usedPartnerPairs,
            targetMatches = targetMatchesPerPlayer
        )
    }
    
    /**
     * Genererer kampe for en runde med fokus på nye makkere og varierende modstandere.
     */
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
    
    /**
     * Finder de bedste 4 spillere til en kamp.
     * Returnerer (team1Player1, team1Player2, team2Player1, team2Player2)
     */
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
    
    /**
     * Finder den bedste holdfordeling for 4 spillere.
     * Returnerer (konfiguration, score) hvor lavere score er bedre.
     */
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
    
    /**
     * Balancerer kampantal så alle har præcis targetMatches kampe.
     */
    private fun balanceMatchCountsWithLimit(
        startRoundNumber: Int, 
        opponentCount: MutableMap<Set<String>, Int>,
        usedPartnerPairs: MutableSet<Set<String>>,
        targetMatches: Int
    ) {
        var roundNumber = startRoundNumber
        val maxIterations = 10
        var iterations = 0
        
        while (!allPlayersHaveEqualMatches() && iterations < maxIterations) {
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
            val courts = getEffectiveCourts()
            val matchesToCreate = (playersNeedingMatches.size / 4).coerceIn(1, courts)
            
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
                
                matches.add(match)
                updateTracking(match, roundNumber)
                usedPartnerPairs.add(pairKey(config[0].id, config[1].id))
                usedPartnerPairs.add(pairKey(config[2].id, config[3].id))
                updateOpponentCount(match, opponentCount)
                fourPlayers.forEach { usedInRound.add(it.id) }
            }
        }
    }
    
    /**
     * Opdaterer modstander-tælleren for en kamp.
     */
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
    
    /**
     * Balancerer kampantal så alle spillere har præcis samme antal kampe.
     * Minimerer antallet af ekstra kampe der skal tilføjes.
     */
    private fun balanceMatchCountsOptimal(startRoundNumber: Int, opponentCount: MutableMap<Set<String>, Int>) {
        var roundNumber = startRoundNumber
        val maxIterations = 50
        var iterations = 0
        
        while (!allPlayersHaveEqualMatches() && iterations < maxIterations) {
            iterations++
            
            val maxMatches = matchCount.values.maxOrNull() ?: 0
            
            // Find spillere der har færre kampe end max
            val playersNeedingMatches = players
                .filter { (matchCount[it.id] ?: 0) < maxMatches }
                .sortedBy { matchCount[it.id] ?: 0 }
            
            if (playersNeedingMatches.isEmpty()) break
            
            if (playersNeedingMatches.size < 4) {
                // Ikke nok spillere der mangler kampe - tilføj filler spillere
                // Vælg filler spillere der har færrest ekstra kampe
                val fillerPlayers = players
                    .filter { (matchCount[it.id] ?: 0) == maxMatches }
                    .shuffled()
                    .take(4 - playersNeedingMatches.size)
                
                val allPlayers = (playersNeedingMatches + fillerPlayers).take(4)
                if (allPlayers.size == 4) {
                    roundNumber++
                    val match = createBalancingMatch(roundNumber, 1, allPlayers, opponentCount)
                    matches.add(match)
                    updateTracking(match, roundNumber)
                    updateOpponentCount(match, opponentCount)
                } else {
                    // Ikke muligt at lave en kamp - stop
                    break
                }
                // Fortsæt løkken for at tjekke om alle nu har lige mange kampe
                continue
            }
            
            // Lav kampe med de spillere der har færrest kampe
            roundNumber++
            val courts = getEffectiveCourts()
            val matchesToCreate = (playersNeedingMatches.size / 4).coerceIn(1, courts)
            
            val usedPlayers = mutableSetOf<String>()
            for (m in 0 until matchesToCreate) {
                val available = playersNeedingMatches.filter { it.id !in usedPlayers }.take(4)
                if (available.size < 4) break
                
                val match = createBalancingMatch(roundNumber, m + 1, available, opponentCount)
                matches.add(match)
                updateTracking(match, roundNumber)
                updateOpponentCount(match, opponentCount)
                available.forEach { usedPlayers.add(it.id) }
            }
        }
    }
    
    /**
     * Opretter en balancerings-kamp med optimal makker/modstander-fordeling.
     */
    private fun createBalancingMatch(
        roundNumber: Int,
        courtNumber: Int,
        fourPlayers: List<Player>,
        opponentCount: Map<Set<String>, Int>
    ): Match {
        // Find den bedste parring (mindst tidligere partnerskaber + mindst modstander-gentagelser)
        var bestConfig: List<Player>? = null
        var bestScore = Int.MAX_VALUE
        
        // Prøv alle mulige hold-kombinationer
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

    /**
     * Udvider turneringen med flere kampe så alle spillere har lige mange kampe.
     * Tilføjer mindst 2 runder, derefter fortsætter indtil alle har lige mange kampe.
     */
    private fun extendAmericanoTournament() {
        rebuildTrackingFromMatches()
        
        // Rebuild opponent count from existing matches
        val opponentCount = mutableMapOf<Set<String>, Int>()
        matches.forEach { match ->
            updateOpponentCount(match, opponentCount)
        }

        val startRoundNumber = matches.maxOfOrNull { it.roundNumber } ?: 0
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
                matches.add(match)
                updateTracking(match, roundNumber)
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
                    matches.add(match)
                    updateTracking(match, roundNumber)
                    updateOpponentCount(match, opponentCount)
                } else {
                    // Ikke nok spillere - stop
                    break
                }
            }
        }
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

        // Opdater global opponent count
        recordOpponents(t1p1, t1p2, t2p1, t2p2)
    }

    private fun incrementPartner(p1: Player, p2: Player) {
        val key = pairKey(p1.id, p2.id)
        partnerCount[key] = (partnerCount[key] ?: 0) + 1
    }

}