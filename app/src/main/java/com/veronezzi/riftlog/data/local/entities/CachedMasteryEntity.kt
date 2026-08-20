package com.veronezzi.riftlog.data.local.entities

data class CachedMasteryEntity(
    val puuid: String,
    val championId: Long,
    val championLevel: Int,
    val championPoints: Long,
    val championPointsSinceLastLevel: Long?,
    val championPointsUntilNextLevel: Long?,
    val fetchedAt: Long,
)
