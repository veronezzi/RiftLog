package com.example.riftlog.data.repository

import com.example.riftlog.data.local.MasteryDao
import com.example.riftlog.data.local.StaticDataDao
import com.example.riftlog.data.local.entities.CachedMasteryEntity
import com.example.riftlog.data.local.entities.StaticDataCacheEntity
import com.example.riftlog.data.remote.RiotApiClient
import com.example.riftlog.data.remote.ddragon.DDragonApi
import com.example.riftlog.data.remote.ddragon.DDragonUrls
import com.example.riftlog.data.remote.ddragon.dto.ChampionDetailListDto
import com.example.riftlog.data.remote.ddragon.dto.ChampionListDto
import com.example.riftlog.data.remote.ddragon.dto.ItemListDto
import com.example.riftlog.data.remote.safeApiCall
import com.example.riftlog.domain.ApiResult
import com.example.riftlog.domain.model.AbilityInfo
import com.example.riftlog.domain.model.ChampionDetail
import com.example.riftlog.domain.model.ChampionInfo
import com.example.riftlog.domain.model.ChampionMastery
import com.example.riftlog.domain.model.ItemInfo
import kotlinx.serialization.json.Json

/** Data Dragon static data (patch-versioned, cached indefinitely) merged with champion mastery. */
class ChampionRepository(
    private val dDragonApi: DDragonApi,
    private val riotApiClient: RiotApiClient,
    private val staticDataDao: StaticDataDao,
    private val masteryDao: MasteryDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLatestVersion(): ApiResult<String> = safeApiCall {
        val cached = staticDataDao.get(KEY_VERSION)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < VERSION_TTL_MILLIS) {
            return@safeApiCall cached.version
        }

        val versions = dDragonApi.getVersions()
        val latest = versions.first()
        staticDataDao.upsert(StaticDataCacheEntity(KEY_VERSION, latest, latest, System.currentTimeMillis()))
        latest
    }

    suspend fun getChampions(version: String, puuid: String?): ApiResult<List<ChampionInfo>> = safeApiCall {
        val key = "champion_list_$version"
        val cached = staticDataDao.get(key)
        val listDto = if (cached != null) {
            json.decodeFromString(ChampionListDto.serializer(), cached.json)
        } else {
            dDragonApi.getChampionList(version).also {
                staticDataDao.upsert(
                    StaticDataCacheEntity(key, version, json.encodeToString(ChampionListDto.serializer(), it), System.currentTimeMillis())
                )
            }
        }

        val masteryByChampionId = if (puuid != null) {
            masteryDao.getMasteriesForPuuid(puuid).associateBy { it.championId }
        } else {
            emptyMap()
        }

        listDto.data.values.map { summary ->
            val championId = summary.key.toLongOrNull() ?: 0L
            ChampionInfo(
                id = summary.id,
                championId = championId,
                name = summary.name,
                title = summary.title,
                squareImageUrl = DDragonUrls.championSquare(version, summary.image.full),
                splashImageUrl = DDragonUrls.championSplash(summary.id),
                mastery = masteryByChampionId[championId]?.let {
                    ChampionMastery(it.championLevel, it.championPoints)
                },
            )
        }
    }

    suspend fun getChampionDetail(version: String, championId: String, championInfo: ChampionInfo): ApiResult<ChampionDetail> = safeApiCall {
        val key = "champion_detail_${version}_$championId"
        val cached = staticDataDao.get(key)
        val detailListDto = if (cached != null) {
            json.decodeFromString(ChampionDetailListDto.serializer(), cached.json)
        } else {
            dDragonApi.getChampionDetail(version, championId).also {
                staticDataDao.upsert(
                    StaticDataCacheEntity(key, version, json.encodeToString(ChampionDetailListDto.serializer(), it), System.currentTimeMillis())
                )
            }
        }
        val detail = detailListDto.data.getValue(championId)

        ChampionDetail(
            info = championInfo,
            hp = detail.stats.hp,
            mp = detail.stats.mp,
            armor = detail.stats.armor,
            magicResist = detail.stats.spellblock,
            attackDamage = detail.stats.attackdamage,
            attackSpeed = detail.stats.attackspeed,
            moveSpeed = detail.stats.movespeed,
            passive = AbilityInfo(
                name = detail.passive.name,
                description = detail.passive.description,
                imageUrl = "https://ddragon.leagueoflegends.com/cdn/$version/img/passive/${detail.passive.image.full}",
            ),
            spells = detail.spells.map {
                AbilityInfo(
                    name = it.name,
                    description = it.description,
                    imageUrl = "https://ddragon.leagueoflegends.com/cdn/$version/img/spell/${it.image.full}",
                )
            },
        )
    }

    suspend fun getItems(version: String): ApiResult<Map<Int, ItemInfo>> = safeApiCall {
        val key = "item_list_$version"
        val cached = staticDataDao.get(key)
        val listDto = if (cached != null) {
            json.decodeFromString(ItemListDto.serializer(), cached.json)
        } else {
            dDragonApi.getItemList(version).also {
                staticDataDao.upsert(
                    StaticDataCacheEntity(key, version, json.encodeToString(ItemListDto.serializer(), it), System.currentTimeMillis())
                )
            }
        }
        listDto.data.mapNotNull { (idString, item) ->
            val itemId = idString.toIntOrNull() ?: return@mapNotNull null
            itemId to ItemInfo(itemId, item.name, DDragonUrls.itemIcon(version, itemId))
        }.toMap()
    }

    suspend fun refreshChampionMasteries(puuid: String, platformRegion: String): ApiResult<Unit> = safeApiCall {
        val masteries = riotApiClient.platformApi(platformRegion).getChampionMasteries(puuid)
        val now = System.currentTimeMillis()
        masteryDao.upsertMasteries(
            masteries.map {
                CachedMasteryEntity(puuid, it.championId, it.championLevel, it.championPoints, now)
            }
        )
    }

    private companion object {
        const val KEY_VERSION = "ddragon_latest_version"
        const val VERSION_TTL_MILLIS = 6 * 60 * 60 * 1000L // 6h - the patch version itself only changes ~biweekly
    }
}
