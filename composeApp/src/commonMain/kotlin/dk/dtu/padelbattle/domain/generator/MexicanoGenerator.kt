package dk.dtu.padelbattle.domain.generator

import dk.dtu.padelbattle.domain.model.Match
import dk.dtu.padelbattle.domain.model.Player
import kotlin.random.Random

/**
 * Generator for Mexicano-turneringer (stateless).
 * 
 * Mexicano-regler:
 * 1. Første runde: Tilfældig parring
 * 2. Efterfølgende runder: Parring baseret på rangliste (1+3 vs 2+4)
 * 3. Genererer kun én runde ad gangen (næste runde afhænger af resultater)
 * 4. Spillere med færrest kampe prioriteres
 * 
 * Denne generator er stateless - al tracking data beregnes fra input parameters.
 */
class MexicanoGenerator : TournamentGenerator {

    /**
     * Intern data class til tracking af spillerstatistik under generering.
     */
    private data class PlayerTracking(
        val matchCount: Map<String, Int>,
        val lastPlayedRound: Map<String, Int>
    )

    override fun generateInitialMatches(
        players: List<Player>,
        numberOfCourts: Int
    ): List<Match> {
        val tracking = createInitialTracking(players)

        // Mexicano starter med kun 1 runde (første runde er tilfældig)
        return generateMexicanoRound(
            players = players,
            numberOfCourts = numberOfCourts,
            roundNumber = 1,
            isFirstRound = true,
            tracking = tracking
        )
    }

    override fun generateExtensionMatches(
        players: List<Player>,
        existingMatches: List<Match>,
        numberOfCourts: Int
    ): List<Match> {
        // Byg tracking fra eksisterende kampe
        val tracking = buildTrackingFromMatches(players, existingMatches)
        val lastRoundNumber = existingMatches.maxOfOrNull { it.roundNumber } ?: 0

        // Generer næste Mexicano-runde baseret på nuværende rangliste
        return generateMexicanoRound(
            players = players,
            numberOfCourts = numberOfCourts,
            roundNumber = lastRoundNumber + 1,
            isFirstRound = false,
            tracking = tracking
        )
    }

    // --- PRIVATE HELPERS ---

    private fun createInitialTracking(players: List<Player>): PlayerTracking {
        return PlayerTracking(
            matchCount = players.associate { it.id to 0 },
            lastPlayedRound = players.associate { it.id to 0 }
        )
    }

    private fun buildTrackingFromMatches(players: List<Player>, matches: List<Match>): PlayerTracking {
        val matchCount = players.associate { it.id to 0 }.toMutableMap()
        val lastPlayedRound = players.associate { it.id to 0 }.toMutableMap()

        matches.forEach { match ->
            // Kun tæl spillede kampe
            if (match.isPlayed) {
                listOf(
                    match.team1Player1,
                    match.team1Player2,
                    match.team2Player1,
                    match.team2Player2
                ).forEach { player ->
                    matchCount[player.id] = (matchCount[player.id] ?: 0) + 1
                    lastPlayedRound[player.id] = match.roundNumber
                }
            }
        }

        return PlayerTracking(matchCount, lastPlayedRound)
    }

    /**
     * Genererer en Mexicano-runde.
     * 
     * Første runde: Tilfældig parring
     * Efterfølgende runder: Spillere sorteres efter points, derefter parres 1+3 vs 2+4
     */
    private fun generateMexicanoRound(
        players: List<Player>,
        numberOfCourts: Int,
        roundNumber: Int,
        isFirstRound: Boolean,
        tracking: PlayerTracking
    ): List<Match> {
        val generatedMatches = mutableListOf<Match>()
        val playersNeeded = numberOfCourts * 4

        // Vælg aktive spillere for denne runde
        var activePlayers = selectActivePlayersForRound(players, playersNeeded, tracking)
        if (activePlayers.size < 4) return emptyList()

        // Sorter spillere
        activePlayers = if (isFirstRound) {
            activePlayers.shuffled()
        } else {
            // Sorter efter point (højest først), derefter tilfældigt ved lige point
            activePlayers.sortedWith(
                compareByDescending<Player> { it.totalPoints }
                    .thenBy { Random.nextInt() }
            )
        }

        // Generer kampe for hver bane
        for (i in 0 until numberOfCourts) {
            val baseIdx = i * 4
            if (baseIdx + 4 <= activePlayers.size) {
                val p1 = activePlayers[baseIdx]
                val p2 = activePlayers[baseIdx + 1]
                val p3 = activePlayers[baseIdx + 2]
                val p4 = activePlayers[baseIdx + 3]

                // Mexicano-regel: 1+3 mod 2+4
                val match = Match(
                    roundNumber = roundNumber,
                    courtNumber = i + 1,
                    team1Player1 = p1,
                    team1Player2 = p3,
                    team2Player1 = p2,
                    team2Player2 = p4
                )
                generatedMatches.add(match)
            }
        }

        return generatedMatches
    }

    /**
     * Vælger spillere til en runde baseret på:
     * 1. Hvem der har færrest kampe
     * 2. Hvem der har siddet over længst tid
     * 3. Tilfældighed ved lige prioritet
     */
    private fun selectActivePlayersForRound(
        players: List<Player>,
        playersNeeded: Int,
        tracking: PlayerTracking
    ): List<Player> {
        return players
            .sortedWith(
                compareBy<Player> { tracking.matchCount[it.id] ?: 0 }
                    .thenBy { tracking.lastPlayedRound[it.id] ?: 0 }
                    .thenBy { Random.nextInt() }
            )
            .take(playersNeeded.coerceAtMost(players.size))
    }
}
