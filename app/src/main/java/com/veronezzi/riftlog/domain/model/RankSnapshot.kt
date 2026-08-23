package com.veronezzi.riftlog.domain.model

/** A single point in a player's rank/LP history for one queue, oldest-first when returned as a
 * list. */
data class RankSnapshot(
    val tier: String,
    val rank: String,
    val leaguePoints: Int,
    val timestamp: Long,
)
