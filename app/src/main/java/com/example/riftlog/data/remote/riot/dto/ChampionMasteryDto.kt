package com.example.riftlog.data.remote.riot.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChampionMasteryDto(
    val championId: Long,
    val championLevel: Int,
    val championPoints: Long,
)
