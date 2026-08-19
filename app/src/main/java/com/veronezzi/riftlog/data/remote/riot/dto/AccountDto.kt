package com.veronezzi.riftlog.data.remote.riot.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val puuid: String,
    val gameName: String? = null,
    val tagLine: String? = null,
)
