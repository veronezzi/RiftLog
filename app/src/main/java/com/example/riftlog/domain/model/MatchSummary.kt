package com.example.riftlog.domain.model

data class MatchSummary(
    val matchId: String,
    val championName: String,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val win: Boolean,
    val items: List<Int>, // item0..item6, zeros mean empty slot
    val summoner1Id: Int,
    val summoner2Id: Int,
    val teamPosition: String,
    val totalMinionsKilled: Int,
    val goldEarned: Int,
    val gameDurationSeconds: Long,
    val gameCreationMillis: Long,
)

data class ChampionAggregate(
    val championName: String,
    val gamesPlayed: Int,
    val wins: Int,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val recommendedBuild: List<Int>,
) {
    val losses: Int get() = gamesPlayed - wins
    val winRate: Double get() = if (gamesPlayed == 0) 0.0 else wins.toDouble() / gamesPlayed
    val avgKda: Double get() = if (avgDeaths == 0.0) avgKills + avgAssists else (avgKills + avgAssists) / avgDeaths
}
