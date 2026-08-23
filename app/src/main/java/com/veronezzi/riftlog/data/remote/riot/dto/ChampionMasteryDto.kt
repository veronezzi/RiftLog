package com.veronezzi.riftlog.data.remote.riot.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChampionMasteryDto(
    val championId: Long,
    val championLevel: Int,
    val championPoints: Long,
    // Defaulted to null (not 0) rather than required: these are documented on champion-mastery-v4
    // but the app has been burned before by assuming a Riot field name/shape without a real
    // response to check against. A missing key becomes "progress unknown", not a fake 0 that's
    // indistinguishable from a genuine max-level or zero-points response.
    val championPointsSinceLastLevel: Long? = null,
    val championPointsUntilNextLevel: Long? = null, // 0 once the champion is at max mastery level
)
