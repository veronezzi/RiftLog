package com.veronezzi.riftlog.domain.model

data class PlayerProfile(
    val puuid: String,
    val gameName: String,
    val tagLine: String,
    val profileIconId: Int,
    val summonerLevel: Long,
    val platformRegion: String,
    val rankEntries: List<RankEntry>,
)

data class RankEntry(
    val queueType: String,
    val tier: String,
    val rank: String,
    val leaguePoints: Int,
    val wins: Int,
    val losses: Int,
)
