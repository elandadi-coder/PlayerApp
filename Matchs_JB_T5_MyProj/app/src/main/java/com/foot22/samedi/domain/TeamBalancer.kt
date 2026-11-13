package com.foot22.samedi.domain

import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import kotlin.math.abs
import kotlin.random.Random

data class BalancedTeamsResult(
    val blueTeam: List<PlayerEntity>,
    val orangeTeam: List<PlayerEntity>,
    val blueTotalSkill: Double,
    val orangeTotalSkill: Double,
    val blueAvgSkill: Double,
    val orangeAvgSkill: Double,
    val skillDelta: Double,
    val effectiveRatings: Map<Long, Double>
)

object TeamBalancer {

    /**
     * Generates balanced Bleu vs Orange teams from present players.
     *
     * - Automatically excludes [PlayerStatus.BLESSE] and [PlayerStatus.INACTIF].
     * - Minimizes total effective skill difference (accounting for [PlayerStatus.EN_REPRISE]).
     * - Balances key positions (Goalkeepers, Defenders, Attackers).
     * - Uses [variantSeed] to cycle cleanly between top near-optimal splits when reshuffling.
     */
    fun generateTeams(
        presentPlayers: List<PlayerEntity>,
        recoveryFactor: Double = 0.85,
        variantSeed: Int = 0
    ): BalancedTeamsResult {
        val eligible = presentPlayers.filter {
            it.status != PlayerStatus.BLESSE && it.status != PlayerStatus.INACTIF
        }
        val ratings = eligible.associate { it.id to it.effectiveRating(recoveryFactor) }

        if (eligible.isEmpty()) {
            return BalancedTeamsResult(emptyList(), emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, emptyMap())
        }
        if (eligible.size == 1) {
            val p = eligible.first()
            val r = ratings[p.id] ?: p.skillRating
            return BalancedTeamsResult(listOf(p), emptyList(), r, 0.0, r, 0.0, r, ratings)
        }

        val targetBlueSize = eligible.size / 2
        val candidates = mutableListOf<Pair<Double, Pair<List<PlayerEntity>, List<PlayerEntity>>>>()
        val seenKeys = HashSet<String>()

        // Run diverse randomized greedy + 2-opt local search trials so reshuffles produce fresh balanced combos.
        val trials = 160
        for (trial in 0 until trials) {
            val rng = Random(trial * 7919L + 31L)
            val shuffled = eligible.shuffled(rng)
                .sortedByDescending { (ratings[it.id] ?: 0.0) + rng.nextDouble(-0.35, 0.35) }

            val blue = mutableListOf<PlayerEntity>()
            val orange = mutableListOf<PlayerEntity>()
            var blueSum = 0.0
            var orangeSum = 0.0

            // Greedy initial assignment respecting size cap
            for (player in shuffled) {
                val r = ratings[player.id] ?: 0.0
                val blueAllowed = blue.size < (eligible.size + 1) / 2
                val orangeAllowed = orange.size < (eligible.size + 1) / 2
                val assignBlue = when {
                    !orangeAllowed -> true
                    !blueAllowed -> false
                    blue.size < targetBlueSize && blueSum <= orangeSum -> true
                    orange.size < eligible.size - targetBlueSize && orangeSum < blueSum -> false
                    blueSum <= orangeSum && blue.size <= orange.size -> true
                    else -> false
                }
                if (assignBlue) {
                    blue.add(player)
                    blueSum += r
                } else {
                    orange.add(player)
                    orangeSum += r
                }
            }

            // Ensure exact target sizes (diff at most 1)
            while (blue.size > targetBlueSize + (eligible.size % 2)) {
                val moved = blue.removeAt(blue.lastIndex)
                orange.add(moved)
            }
            while (orange.size > eligible.size - targetBlueSize) {
                val moved = orange.removeAt(orange.lastIndex)
                blue.add(moved)
            }

            // Local 2-opt swap refinement
            var improved = true
            var currentCost = scoreSplit(blue, orange, ratings)
            var passes = 0
            while (improved && passes < 25) {
                improved = false
                passes++
                for (i in blue.indices) {
                    for (j in orange.indices) {
                        val bPlayer = blue[i]
                        val oPlayer = orange[j]
                        blue[i] = oPlayer
                        orange[j] = bPlayer
                        val newCost = scoreSplit(blue, orange, ratings)
                        if (newCost + 1e-6 < currentCost) {
                            currentCost = newCost
                            improved = true
                        } else {
                            blue[i] = bPlayer
                            orange[j] = oPlayer
                        }
                    }
                }
            }

            val key = canonicalSplitKey(blue, orange)
            if (seenKeys.add(key)) {
                candidates.add(currentCost to (blue.toList() to orange.toList()))
            }
        }

        val sortedCandidates = candidates.sortedBy { it.first }
        val bestCost = sortedCandidates.first().first
        // Keep near-optimal alternatives within a tight margin so reshuffle stays fair
        val nearOptimalPool = sortedCandidates.filter { it.first <= bestCost + 0.65 }.take(8)
        val picked = nearOptimalPool[Math.floorMod(variantSeed, nearOptimalPool.size)].second

        val blueSorted = sortForDisplay(picked.first, ratings)
        val orangeSorted = sortForDisplay(picked.second, ratings)

        return summarize(blueSorted, orangeSorted, ratings)
    }

    fun summarize(
        blueTeam: List<PlayerEntity>,
        orangeTeam: List<PlayerEntity>,
        ratings: Map<Long, Double>
    ): BalancedTeamsResult {
        val blueTotal = blueTeam.sumOf { ratings[it.id] ?: it.skillRating }
        val orangeTotal = orangeTeam.sumOf { ratings[it.id] ?: it.skillRating }
        val blueAvg = if (blueTeam.isEmpty()) 0.0 else blueTotal / blueTeam.size
        val orangeAvg = if (orangeTeam.isEmpty()) 0.0 else orangeTotal / orangeTeam.size
        return BalancedTeamsResult(
            blueTeam = blueTeam,
            orangeTeam = orangeTeam,
            blueTotalSkill = blueTotal,
            orangeTotalSkill = orangeTotal,
            blueAvgSkill = blueAvg,
            orangeAvgSkill = orangeAvg,
            skillDelta = abs(blueTotal - orangeTotal),
            effectiveRatings = ratings
        )
    }

    private fun scoreSplit(
        blue: List<PlayerEntity>,
        orange: List<PlayerEntity>,
        ratings: Map<Long, Double>
    ): Double {
        val blueSum = blue.sumOf { ratings[it.id] ?: it.skillRating }
        val orangeSum = orange.sumOf { ratings[it.id] ?: it.skillRating }
        val skillDiff = abs(blueSum - orangeSum)

        val blueAvg = if (blue.isEmpty()) 0.0 else blueSum / blue.size
        val orangeAvg = if (orange.isEmpty()) 0.0 else orangeSum / orange.size
        val avgDiff = abs(blueAvg - orangeAvg)

        val gkDiff = abs(
            blue.count { it.position == PlayerPosition.GARDIEN } -
                orange.count { it.position == PlayerPosition.GARDIEN }
        )
        val defDiff = abs(
            blue.count { it.position == PlayerPosition.DEFENSEUR } -
                orange.count { it.position == PlayerPosition.DEFENSEUR }
        )
        val attDiff = abs(
            blue.count { it.position == PlayerPosition.ATTAQUANT } -
                orange.count { it.position == PlayerPosition.ATTAQUANT }
        )
        val sizeDiff = abs(blue.size - orange.size)

        return skillDiff * 2.0 +
            avgDiff * 3.0 +
            gkDiff * 2.5 +
            defDiff * 0.7 +
            attDiff * 0.7 +
            sizeDiff * 10.0
    }

    private fun canonicalSplitKey(blue: List<PlayerEntity>, orange: List<PlayerEntity>): String {
        val bIds = blue.map { it.id }.sorted().joinToString(",")
        val oIds = orange.map { it.id }.sorted().joinToString(",")
        // Treat (A vs B) and (B vs A) as distinct or same? Distinct lets blue/orange shirts vary,
        // while keeping min(bIds, oIds) avoids mirrored duplicates if desired; keep ordered so shirt swap can happen or sort.
        return if (bIds <= oIds) "$bIds|$oIds" else "$oIds|$bIds"
    }

    private fun sortForDisplay(team: List<PlayerEntity>, ratings: Map<Long, Double>): List<PlayerEntity> =
        team.sortedWith(
            compareBy<PlayerEntity> {
                when (it.position) {
                    PlayerPosition.GARDIEN -> 0
                    PlayerPosition.DEFENSEUR -> 1
                    PlayerPosition.MILIEU -> 2
                    PlayerPosition.POLYVALENT -> 3
                    PlayerPosition.ATTAQUANT -> 4
                }
            }.thenByDescending { ratings[it.id] ?: it.skillRating }
        )
}
