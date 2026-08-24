package com.veronezzi.riftlog.data.repository

import com.veronezzi.riftlog.data.remote.riot.dto.MatchInfoDto
import com.veronezzi.riftlog.data.remote.riot.dto.ObjectiveDto
import com.veronezzi.riftlog.data.remote.riot.dto.ObjectivesDto
import com.veronezzi.riftlog.data.remote.riot.dto.ParticipantDto
import com.veronezzi.riftlog.data.remote.riot.dto.TeamDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchDetailMapperTest {

    private fun participant(
        puuid: String,
        teamId: Int,
        win: Boolean,
        riotIdGameName: String? = "Player",
        riotIdTagline: String? = "NA1",
    ) = ParticipantDto(
        puuid = puuid,
        championName = "Ahri",
        kills = 5, deaths = 2, assists = 7, win = win,
        item0 = 0, item1 = 0, item2 = 0, item3 = 0, item4 = 0, item5 = 0, item6 = 0,
        summoner1Id = 4, summoner2Id = 7,
        teamPosition = "MIDDLE",
        totalMinionsKilled = 180,
        goldEarned = 12000,
        riotIdGameName = riotIdGameName,
        riotIdTagline = riotIdTagline,
        teamId = teamId,
    )

    private fun objectives(kills: Int) = ObjectivesDto(
        baron = ObjectiveDto(kills), dragon = ObjectiveDto(kills), tower = ObjectiveDto(kills),
        inhibitor = ObjectiveDto(kills), riftHerald = ObjectiveDto(kills), champion = ObjectiveDto(kills),
    )

    @Test
    fun `maps riot id and marks the viewer's participant`() {
        val viewer = participant("viewer-puuid", teamId = 100, win = true)
        val info = MatchInfoDto(
            gameDuration = 1800, gameCreation = 0,
            participants = listOf(viewer, participant("other-puuid", teamId = 200, win = false)),
            teams = listOf(
                TeamDto(100, win = true, objectives = objectives(2)),
                TeamDto(200, win = false, objectives = objectives(1)),
            ),
        )

        val detail = info.toMatchDetail("MATCH1", "viewer-puuid")

        val viewerTeam = detail.teams.first { it.teamId == 100 }
        val viewerDetail = viewerTeam.participants.single()
        assertEquals("Player#NA1", viewerDetail.displayName)
        assertTrue(viewerDetail.isViewer)
        assertEquals(2, viewerTeam.baronKills)
    }

    @Test
    fun `falls back to champion name when riot id is blank`() {
        val info = MatchInfoDto(
            gameDuration = 1800, gameCreation = 0,
            participants = listOf(participant("p1", teamId = 100, win = true, riotIdGameName = "", riotIdTagline = null)),
            teams = listOf(TeamDto(100, win = true, objectives = objectives(0))),
        )

        val detail = info.toMatchDetail("MATCH1", "someone-else")

        assertEquals("Ahri", detail.teams.single().participants.single().displayName)
    }

    @Test
    fun `synthesizes teams from participant teamId when Riot omits the teams array`() {
        val info = MatchInfoDto(
            gameDuration = 1800, gameCreation = 0,
            participants = listOf(
                participant("p1", teamId = 100, win = true),
                participant("p2", teamId = 200, win = false),
            ),
            teams = emptyList(),
        )

        val detail = info.toMatchDetail("OLD_MATCH", "p1")

        assertEquals(2, detail.teams.size)
        val blueTeam = detail.teams.first { it.teamId == 100 }
        assertTrue(blueTeam.win)
        assertEquals(0, blueTeam.baronKills)
    }
}
