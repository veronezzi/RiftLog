package com.veronezzi.riftlog.data.repository

import com.veronezzi.riftlog.data.remote.riot.dto.MatchInfoDto
import com.veronezzi.riftlog.data.remote.riot.dto.ParticipantDto
import com.veronezzi.riftlog.domain.model.MatchDetail
import com.veronezzi.riftlog.domain.model.MatchParticipantDetail
import com.veronezzi.riftlog.domain.model.MatchTeamDetail

/** Pure mapping from the wire shape to the domain model - no DB/network, so it's unit-testable
 * on its own. Handles two edge cases Riot's response doesn't guarantee: `info.teams` missing on
 * older matches (synthesize sides from each participant's teamId with zeroed-out objectives
 * instead of crashing), and a blank/null riotIdGameName-tagline pair on some matches (fall back
 * to the champion name rather than showing an empty row). */
fun MatchInfoDto.toMatchDetail(matchId: String, viewerPuuid: String): MatchDetail {
    val teamDetails = if (teams.isNotEmpty()) {
        teams.map { team ->
            MatchTeamDetail(
                teamId = team.teamId,
                win = team.win,
                baronKills = team.objectives.baron.kills,
                dragonKills = team.objectives.dragon.kills,
                towerKills = team.objectives.tower.kills,
                participants = participants.filter { it.teamId == team.teamId }.map { it.toDetail(viewerPuuid) },
            )
        }
    } else {
        participants.groupBy { it.teamId }.map { (teamId, teamParticipants) ->
            MatchTeamDetail(
                teamId = teamId,
                win = teamParticipants.firstOrNull()?.win ?: false,
                baronKills = 0,
                dragonKills = 0,
                towerKills = 0,
                participants = teamParticipants.map { it.toDetail(viewerPuuid) },
            )
        }
    }
    return MatchDetail(
        matchId = matchId,
        gameDurationSeconds = gameDuration,
        gameCreationMillis = gameCreation,
        teams = teamDetails,
    )
}

private fun ParticipantDto.toDetail(viewerPuuid: String) = MatchParticipantDetail(
    puuid = puuid,
    displayName = riotIdGameName?.takeIf { it.isNotBlank() }?.let { name ->
        val tag = riotIdTagline?.takeIf { it.isNotBlank() }
        if (tag != null) "$name#$tag" else name
    } ?: championName,
    championName = championName,
    teamPosition = teamPosition,
    kills = kills,
    deaths = deaths,
    assists = assists,
    champLevel = champLevel,
    items = listOf(item0, item1, item2, item3, item4, item5, item6),
    totalDamageDealtToChampions = totalDamageDealtToChampions,
    totalMinionsKilled = totalMinionsKilled,
    goldEarned = goldEarned,
    visionScore = visionScore,
    wardsPlaced = wardsPlaced,
    summoner1Id = summoner1Id,
    summoner2Id = summoner2Id,
    isViewer = puuid == viewerPuuid,
)
