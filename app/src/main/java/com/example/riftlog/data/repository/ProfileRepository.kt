package com.example.riftlog.data.repository

import com.example.riftlog.data.local.ProfileDao
import com.example.riftlog.data.local.entities.CachedProfileEntity
import com.example.riftlog.data.local.entities.CachedRankEntryEntity
import com.example.riftlog.data.remote.RegionMapper
import com.example.riftlog.data.remote.RiotApiClient
import com.example.riftlog.data.remote.safeApiCall
import com.example.riftlog.domain.ApiResult
import com.example.riftlog.domain.model.PlayerProfile
import com.example.riftlog.domain.model.RankEntry

private const val LIVE_DATA_TTL_MILLIS = 5 * 60 * 1000L

/** Orchestrates account -> summoner -> league lookups and caches the merged result. */
class ProfileRepository(
    private val apiClient: RiotApiClient,
    private val profileDao: ProfileDao,
) {

    suspend fun getProfile(
        gameName: String,
        tagLine: String,
        platformRegion: String,
        forceRefresh: Boolean = false,
    ): ApiResult<PlayerProfile> {
        val riotIdKey = "$platformRegion|$gameName#$tagLine"

        if (!forceRefresh) {
            val cached = profileDao.getProfile(riotIdKey)
            if (cached != null && System.currentTimeMillis() - cached.fetchedAt < LIVE_DATA_TTL_MILLIS) {
                val rankEntries = profileDao.getRankEntries(cached.puuid)
                return ApiResult.Success(cached.toDomain(rankEntries))
            }
        }

        val regionalRouting = RegionMapper.regionalRoutingFor(platformRegion)

        val accountResult = safeApiCall {
            apiClient.regionalApi(regionalRouting).getAccountByRiotId(gameName, tagLine)
        }
        val account = when (accountResult) {
            is ApiResult.Success -> accountResult.data
            is ApiResult.Error -> return accountResult
        }

        val summonerResult = safeApiCall {
            apiClient.platformApi(platformRegion).getSummonerByPuuid(account.puuid)
        }
        val summoner = when (summonerResult) {
            is ApiResult.Success -> summonerResult.data
            is ApiResult.Error -> return summonerResult
        }

        val leagueResult = safeApiCall {
            apiClient.platformApi(platformRegion).getLeagueEntriesByPuuid(account.puuid)
        }
        val leagueEntries = when (leagueResult) {
            is ApiResult.Success -> leagueResult.data
            is ApiResult.Error -> return leagueResult
        }

        val now = System.currentTimeMillis()
        profileDao.upsertProfile(
            CachedProfileEntity(
                riotIdKey = riotIdKey,
                platformRegion = platformRegion,
                puuid = account.puuid,
                gameName = account.gameName ?: gameName,
                tagLine = account.tagLine ?: tagLine,
                profileIconId = summoner.profileIconId,
                summonerLevel = summoner.summonerLevel,
                fetchedAt = now,
            )
        )
        profileDao.clearRankEntries(account.puuid)
        profileDao.upsertRankEntries(
            leagueEntries.map {
                CachedRankEntryEntity(
                    puuid = account.puuid,
                    queueType = it.queueType,
                    tier = it.tier,
                    rank = it.rank,
                    leaguePoints = it.leaguePoints,
                    wins = it.wins,
                    losses = it.losses,
                    fetchedAt = now,
                )
            }
        )

        return ApiResult.Success(
            PlayerProfile(
                puuid = account.puuid,
                gameName = account.gameName ?: gameName,
                tagLine = account.tagLine ?: tagLine,
                profileIconId = summoner.profileIconId,
                summonerLevel = summoner.summonerLevel,
                platformRegion = platformRegion,
                rankEntries = leagueEntries.map {
                    RankEntry(it.queueType, it.tier, it.rank, it.leaguePoints, it.wins, it.losses)
                },
            )
        )
    }

    private fun CachedProfileEntity.toDomain(rankEntries: List<CachedRankEntryEntity>) = PlayerProfile(
        puuid = puuid,
        gameName = gameName,
        tagLine = tagLine,
        profileIconId = profileIconId,
        summonerLevel = summonerLevel,
        platformRegion = platformRegion,
        rankEntries = rankEntries.map {
            RankEntry(it.queueType, it.tier, it.rank, it.leaguePoints, it.wins, it.losses)
        },
    )
}
