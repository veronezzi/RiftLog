package com.example.riftlog.data.remote.ddragon.dto

import kotlinx.serialization.Serializable

@Serializable
data class ItemListDto(
    val data: Map<String, ItemSummaryDto>,
)

@Serializable
data class ItemSummaryDto(
    val name: String,
    val image: ImageDto,
)
