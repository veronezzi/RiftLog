package com.veronezzi.riftlog.domain.model

/** The full 10-player match, grouped by side. */
data class MatchDetail(
    val matchId: String,
    val gameDurationSeconds: Long,
    val gameCreationMillis: Long,
    val teams: List<MatchTeamDetail>,
)

data class MatchTeamDetail(
    val teamId: Int,
    val win: Boolean,
    val baronKills: Int,
    val dragonKills: Int,
    val towerKills: Int,
    val participants: List<MatchParticipantDetail>,
)

data class MatchParticipantDetail(
    val puuid: String,
    /** Riot ID display name, "GameName#Tag" - falls back to the champion name when Riot didn't
     * return riotIdGameName/riotIdTagline for this participant (some older/edge-case matches). */
    val displayName: String,
    val championName: String,
    val teamPosition: String,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val champLevel: Int,
    val items: List<Int>,
    val totalDamageDealtToChampions: Int,
    val totalMinionsKilled: Int,
    val goldEarned: Int,
    val visionScore: Int,
    val wardsPlaced: Int,
    val summoner1Id: Int,
    val summoner2Id: Int,
    val isViewer: Boolean,
)
