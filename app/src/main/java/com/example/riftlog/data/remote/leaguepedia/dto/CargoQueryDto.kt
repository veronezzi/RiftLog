package com.example.riftlog.data.remote.leaguepedia.dto

import kotlinx.serialization.Serializable

@Serializable
data class CargoQueryResponseDto(
    val cargoquery: List<CargoQueryRowDto> = emptyList(),
    // MediaWiki's API answers errors (including rate-limiting) with HTTP 200 and this field
    // instead of a non-2xx status - an empty `cargoquery` alone doesn't mean "no data".
    val error: CargoErrorDto? = null,
)

@Serializable
data class CargoErrorDto(
    val code: String? = null,
    val info: String? = null,
)

@Serializable
data class CargoQueryRowDto(
    val title: CargoQueryTitleDto,
)

@Serializable
data class CargoQueryTitleDto(
    val Champion: String? = null,
    val Items: String? = null,
)
