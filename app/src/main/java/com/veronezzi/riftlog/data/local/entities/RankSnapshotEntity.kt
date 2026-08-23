package com.veronezzi.riftlog.data.local.entities

data class RankSnapshotEntity(
    val puuid: String,
    val queueType: String,
    val tier: String,
    val rank: String,
    val leaguePoints: Int,
    val timestamp: Long,
)
