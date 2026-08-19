package com.veronezzi.riftlog.data.local.entities

data class CachedRankEntryEntity(
    val puuid: String,
    val queueType: String,
    val tier: String,
    val rank: String,
    val leaguePoints: Int,
    val wins: Int,
    val losses: Int,
    val fetchedAt: Long,
)
