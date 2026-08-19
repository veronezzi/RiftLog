package com.example.riftlog.data.local.entities

data class CachedMasteryEntity(
    val puuid: String,
    val championId: Long,
    val championLevel: Int,
    val championPoints: Long,
    val fetchedAt: Long,
)
