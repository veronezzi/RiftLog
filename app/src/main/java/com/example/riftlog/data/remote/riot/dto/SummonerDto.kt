package com.example.riftlog.data.remote.riot.dto

import kotlinx.serialization.Serializable

@Serializable
data class SummonerDto(
    val id: String? = null,
    val profileIconId: Int,
    val summonerLevel: Long,
)
