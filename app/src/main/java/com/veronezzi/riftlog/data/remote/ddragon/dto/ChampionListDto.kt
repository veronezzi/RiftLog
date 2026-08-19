package com.veronezzi.riftlog.data.remote.ddragon.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChampionListDto(
    val data: Map<String, ChampionSummaryDto>,
)

@Serializable
data class ChampionSummaryDto(
    val id: String,
    val key: String,
    val name: String,
    val title: String,
    val image: ImageDto,
)

@Serializable
data class ImageDto(
    val full: String,
)
