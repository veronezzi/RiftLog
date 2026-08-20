package com.veronezzi.riftlog.data.remote.riot.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChampionMasteryDto(
    val championId: Long,
    val championLevel: Int,
    val championPoints: Long,
    // Defaulted rather than required: these are documented on champion-mastery-v4 but the app
    // has been burned before by assuming a Riot field name/shape without a real response to
    // check against, so a missing key degrades to "no progress info" instead of a parse crash.
    val championPointsSinceLastLevel: Long = 0,
    val championPointsUntilNextLevel: Long = 0, // 0 once the champion is at max mastery level
    val tokensEarned: Int = 0,
)
