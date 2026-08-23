package com.veronezzi.riftlog.data.local.entities

/**
 * One fully-fetched match, keyed by matchId. [json] is the serialized [MatchInfoDto] (all 10
 * participants + team objectives) - a JSON blob rather than normalized columns, same tradeoff
 * StaticDataCacheEntity makes for static data. Shared across every viewer who has this match in
 * their history, unlike `cached_matches` which is per-owner.
 */
data class FullMatchCacheEntity(
    val matchId: String,
    val json: String,
    val fetchedAt: Long,
)
