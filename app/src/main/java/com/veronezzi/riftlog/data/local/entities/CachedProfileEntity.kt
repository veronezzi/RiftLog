package com.veronezzi.riftlog.data.local.entities

/** One row per (platform region, riot id) lookup - the "profile" the user searched for. */
data class CachedProfileEntity(
    val riotIdKey: String, // "$platformRegion|$gameName#$tagLine"
    val platformRegion: String,
    val puuid: String,
    val gameName: String,
    val tagLine: String,
    val profileIconId: Int,
    val summonerLevel: Long,
    val fetchedAt: Long,
)
