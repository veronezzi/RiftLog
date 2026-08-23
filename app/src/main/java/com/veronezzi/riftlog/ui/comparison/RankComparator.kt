package com.veronezzi.riftlog.ui.comparison

import com.veronezzi.riftlog.domain.model.RankEntry

/** Orders two rank entries by tier, then division, then LP - the same order Riot's own client
 * sorts by. A tier/division absent from the lookup lists (unexpected API value) falls back to
 * index -1, i.e. it compares as below IRON/below division IV rather than crashing - it does not
 * tie against other unexpected values. */
object RankComparator {
    private val TIER_ORDER = listOf(
        "IRON", "BRONZE", "SILVER", "GOLD", "PLATINUM", "EMERALD", "DIAMOND",
        "MASTER", "GRANDMASTER", "CHALLENGER",
    )
    private val DIVISION_ORDER = listOf("IV", "III", "II", "I")

    /** Positive when [a] outranks [b], negative when [b] outranks [a], zero when tied or when
     * both sides are unranked in this queue. Being ranked at all beats being unranked. */
    fun compare(a: RankEntry?, b: RankEntry?): Int {
        if (a == null && b == null) return 0
        if (a == null) return -1
        if (b == null) return 1

        val tierDiff = TIER_ORDER.indexOf(a.tier.uppercase()) - TIER_ORDER.indexOf(b.tier.uppercase())
        if (tierDiff != 0) return tierDiff

        val divisionDiff = DIVISION_ORDER.indexOf(a.rank.uppercase()) - DIVISION_ORDER.indexOf(b.rank.uppercase())
        if (divisionDiff != 0) return divisionDiff

        return a.leaguePoints - b.leaguePoints
    }

    /** Collapses tier + division + LP into one increasing number, for plotting rank progression
     * on a single axis. Same ordering as [compare]: each tier is worth 400 "points" (100 per
     * division), plus the raw LP within it. An unrecognized tier/division falls back to index 0
     * (bottom of the scale) rather than -400/-100, so a bad API value doesn't plot as more
     * negative than IRON IV. */
    fun numericValue(tier: String, rank: String, leaguePoints: Int): Int {
        val tierIndex = TIER_ORDER.indexOf(tier.uppercase()).coerceAtLeast(0)
        val divisionIndex = DIVISION_ORDER.indexOf(rank.uppercase()).coerceAtLeast(0)
        return tierIndex * 400 + divisionIndex * 100 + leaguePoints
    }
}
