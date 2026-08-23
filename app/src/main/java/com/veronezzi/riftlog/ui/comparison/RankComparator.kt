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
}
