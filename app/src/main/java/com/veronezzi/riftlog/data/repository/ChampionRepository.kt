package com.veronezzi.riftlog.data.repository

import com.veronezzi.riftlog.data.local.MasteryDao
import com.veronezzi.riftlog.data.local.StaticDataDao
import com.veronezzi.riftlog.data.local.entities.CachedMasteryEntity
import com.veronezzi.riftlog.data.local.entities.StaticDataCacheEntity
import com.veronezzi.riftlog.data.remote.RiotApiClient
import com.veronezzi.riftlog.data.remote.ddragon.DDragonApi
import com.veronezzi.riftlog.data.remote.ddragon.DDragonUrls
import com.veronezzi.riftlog.data.remote.ddragon.dto.ChampionDetailListDto
import com.veronezzi.riftlog.data.remote.ddragon.dto.ChampionListDto
import com.veronezzi.riftlog.data.remote.ddragon.dto.ItemListDto
import com.veronezzi.riftlog.data.remote.ddragon.dto.RuneTreeDto
import com.veronezzi.riftlog.data.remote.safeApiCall
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.domain.model.AbilityInfo
import com.veronezzi.riftlog.domain.model.ChampionDetail
import com.veronezzi.riftlog.domain.model.ChampionInfo
import com.veronezzi.riftlog.domain.model.ChampionMastery
import com.veronezzi.riftlog.domain.model.ItemInfo
import com.veronezzi.riftlog.domain.model.RuneCatalog
import com.veronezzi.riftlog.domain.model.RuneIconInfo
import kotlinx.serialization.builtins.ListSerializer
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
                    ChampionMastery(
                        championLevel = it.championLevel,
                        championPoints = it.championPoints,
                        championPointsSinceLastLevel = it.championPointsSinceLastLevel,
                        championPointsUntilNextLevel = it.championPointsUntilNextLevel,
                        tokensEarned = it.tokensEarned,
                    )
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

    suspend fun getRunes(version: String): ApiResult<RuneCatalog> = safeApiCall {
        val key = "runes_reforged_$version"
        val cached = staticDataDao.get(key)
        val serializer = ListSerializer(RuneTreeDto.serializer())
        val trees = if (cached != null) {
            json.decodeFromString(serializer, cached.json)
        } else {
            dDragonApi.getRunesReforged(version).also {
                staticDataDao.upsert(
                    StaticDataCacheEntity(key, version, json.encodeToString(serializer, it), System.currentTimeMillis())
                )
            }
        }

        val stylesById = trees.associate { tree ->
            tree.id to RuneIconInfo(tree.id, tree.name, DDragonUrls.runeIcon(tree.icon))
        }
        val runesById = trees.flatMap { it.slots }.flatMap { it.runes }.associate { rune ->
            rune.id to RuneIconInfo(rune.id, rune.name, DDragonUrls.runeIcon(rune.icon))
        }
        RuneCatalog(stylesById, runesById)
    }

    suspend fun refreshChampionMasteries(puuid: String, platformRegion: String): ApiResult<Unit> = safeApiCall {
        val masteries = riotApiClient.platformApi(platformRegion).getChampionMasteries(puuid)
        val now = System.currentTimeMillis()
        masteryDao.upsertMasteries(
            masteries.map {
                CachedMasteryEntity(
                    puuid = puuid,
                    championId = it.championId,
                    championLevel = it.championLevel,
                    championPoints = it.championPoints,
                    championPointsSinceLastLevel = it.championPointsSinceLastLevel,
                    championPointsUntilNextLevel = it.championPointsUntilNextLevel,
                    tokensEarned = it.tokensEarned,
                    fetchedAt = now,
                )
            }
        )
    }

    private companion object {
        const val KEY_VERSION = "ddragon_latest_version"
        const val VERSION_TTL_MILLIS = 6 * 60 * 60 * 1000L // 6h - the patch version itself only changes ~biweekly
    }
}
