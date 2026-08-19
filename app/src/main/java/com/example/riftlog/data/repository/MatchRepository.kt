package com.example.riftlog.data.repository

import com.example.riftlog.data.local.MatchDao
import com.example.riftlog.data.local.entities.CachedMatchEntity
import com.example.riftlog.data.remote.RegionMapper
import com.example.riftlog.data.remote.RiotApiClient
import com.example.riftlog.data.remote.riot.dto.ParticipantDto
import com.example.riftlog.data.remote.safeApiCall
import com.example.riftlog.domain.ApiResult
import com.example.riftlog.domain.model.ChampionAggregate
import com.example.riftlog.domain.model.MatchSummary

private const val LIVE_DATA_TTL_MILLIS = 5 * 60 * 1000L

/** A page of match summaries. [hasMore] reflects whether Riot's match-id list itself came back
 * full (`count` ids), not the size of the locally cached/filtered result after participants that
 * couldn't be matched get dropped - the previous version could hide "load more" while pages still
 * existed, because a few filtered-out matches made the final list come up short of `count` even
 * though more data was available. This is still a "got a full page, so assume there might be
 * more" heuristic, not a lookahead - a player with *exactly* `count` remaining games will still
 * show one extra (now-empty) "load more" tap before hasMore turns false, same tradeoff most
 * offset-based pagination makes. */
data class MatchPage(val matches: List<MatchSummary>, val hasMore: Boolean)

/** Fetches match ids + details and computes per-champion aggregates from the cached window. */
class MatchRepository(
    private val apiClient: RiotApiClient,
    private val matchDao: MatchDao,
) {

    suspend fun getRecentMatches(
        puuid: String,
        platformRegion: String,
        count: Int = 20,
        forceRefresh: Boolean = false,
    ): ApiResult<MatchPage> {
        if (!forceRefresh) {
            val cached = matchDao.getMatchesForPuuid(puuid)
            val freshEnough = cached.isNotEmpty() &&
                System.currentTimeMillis() - cached.first().fetchedAt < LIVE_DATA_TTL_MILLIS
            if (freshEnough) {
                return ApiResult.Success(MatchPage(cached.take(count).map { it.toDomain() }, hasMore = cached.size >= count))
            }
        }

        val regionalRouting = RegionMapper.regionalRoutingFor(platformRegion)
        val regionalApi = apiClient.regionalApi(regionalRouting)

        val idsResult = safeApiCall { regionalApi.getMatchIds(puuid, start = 0, count = count) }
        val matchIds = when (idsResult) {
            is ApiResult.Success -> idsResult.data
            is ApiResult.Error -> return idsResult
        }
        val hasMore = matchIds.size >= count

        val alreadyCached = matchDao.getCachedMatchIds(puuid).toSet()
        val idsToFetch = matchIds.filterNot { it in alreadyCached }

        val now = System.currentTimeMillis()
        val newEntities = mutableListOf<CachedMatchEntity>()
        for (matchId in idsToFetch) {
            val matchResult = safeApiCall { regionalApi.getMatchDetail(matchId) }
            val match = when (matchResult) {
                is ApiResult.Success -> matchResult.data
                is ApiResult.Error -> {
                    // Persist whatever we already fetched before propagating the error, so a
                    // transient failure partway through doesn't discard earlier successful fetches.
                    if (newEntities.isNotEmpty()) matchDao.upsertMatches(newEntities)
                    return matchResult
                }
            }
            val participant = match.info.participants.firstOrNull { it.puuid == puuid } ?: continue
            newEntities += participant.toEntity(matchId, puuid, match.info.gameDuration, match.info.gameCreation, now)
        }
        if (newEntities.isNotEmpty()) {
            matchDao.upsertMatches(newEntities)
        }

        val allMatches = matchDao.getMatchesForPuuid(puuid)
        return ApiResult.Success(MatchPage(allMatches.take(count).map { it.toDomain() }, hasMore))
    }

    suspend fun getChampionAggregate(puuid: String, championName: String): ChampionAggregate? {
        val matches = matchDao.getMatchesForChampion(puuid, championName)
        if (matches.isEmpty()) return null

        val wins = matches.count { it.win }
        val itemSetCounts = matches
            .map { listOf(it.item0, it.item1, it.item2, it.item3, it.item4, it.item5, it.item6) }
            .groupingBy { it }
            .eachCount()
        val recommendedBuild = itemSetCounts.maxByOrNull { it.value }?.key ?: emptyList()

        return ChampionAggregate(
            championName = championName,
            gamesPlayed = matches.size,
            wins = wins,
            avgKills = matches.map { it.kills }.average(),
            avgDeaths = matches.map { it.deaths }.average(),
            avgAssists = matches.map { it.assists }.average(),
            recommendedBuild = recommendedBuild,
        )
    }

    private fun ParticipantDto.toEntity(
        matchId: String,
        ownerPuuid: String,
        gameDuration: Long,
        gameCreation: Long,
        fetchedAt: Long,
    ) = CachedMatchEntity(
        matchId = matchId,
        ownerPuuid = ownerPuuid,
        championName = championName,
        kills = kills,
        deaths = deaths,
        assists = assists,
        win = win,
        item0 = item0,
        item1 = item1,
        item2 = item2,
        item3 = item3,
        item4 = item4,
        item5 = item5,
        item6 = item6,
        summoner1Id = summoner1Id,
        summoner2Id = summoner2Id,
        teamPosition = teamPosition,
        totalMinionsKilled = totalMinionsKilled,
        goldEarned = goldEarned,
        gameDuration = gameDuration,
        gameCreation = gameCreation,
        fetchedAt = fetchedAt,
    )

    private fun CachedMatchEntity.toDomain() = MatchSummary(
        matchId = matchId,
        championName = championName,
        kills = kills,
        deaths = deaths,
        assists = assists,
        win = win,
        items = listOf(item0, item1, item2, item3, item4, item5, item6),
        summoner1Id = summoner1Id,
        summoner2Id = summoner2Id,
        teamPosition = teamPosition,
        totalMinionsKilled = totalMinionsKilled,
        goldEarned = goldEarned,
        gameDurationSeconds = gameDuration,
        gameCreationMillis = gameCreation,
    )
}
