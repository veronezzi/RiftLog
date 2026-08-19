package com.veronezzi.riftlog.data.repository

import com.veronezzi.riftlog.data.local.StaticDataDao
import com.veronezzi.riftlog.data.local.entities.StaticDataCacheEntity
import com.veronezzi.riftlog.data.remote.leaguepedia.LeaguepediaApi
import com.veronezzi.riftlog.data.remote.safeApiCall
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.domain.model.ProBuild
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val RECENT_GAMES_QUERIED = 20
private const val BUILD_SIZE = 6
private const val CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L // Leaguepedia's public API rate-limits
// aggressively per IP, and pro item builds don't change minute to minute, so a several-hour
// cache both protects against that and means a transient rate-limit/outage doesn't lose data
// that was already fetched once.

@Serializable
private data class CachedProBuildDto(val itemIds: List<Int>, val sampleGames: Int)

/** Builds a "what pros actually bought" item list from real, recent official matches (via
 * Leaguepedia), instead of guessing at a "meta" build from no data. `ApiResult.Success(null)`
 * means the fetch worked but the champion genuinely has no recent pro games with a recorded
 * build - that's a fact about the champion, not a failure, and the UI should say so differently
 * than an actual `ApiResult.Error` (network hiccup or Leaguepedia's rate limit). */
class ProBuildRepository(
    private val leaguepediaApi: LeaguepediaApi,
    private val championRepository: ChampionRepository,
    private val staticDataDao: StaticDataDao,
) {
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    suspend fun getProBuild(championName: String, version: String): ApiResult<ProBuild?> {
        val cacheKey = "pro_build_$championName"
        val itemsByIdResult = championRepository.getItems(version)
        val itemsById = (itemsByIdResult as? ApiResult.Success)?.data ?: emptyMap()

        val cached = staticDataDao.get(cacheKey)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MILLIS) {
            return ApiResult.Success(cached.toProBuild(itemsById))
        }

        return safeApiCall {
            val itemsByName = itemsById.values.associateBy { it.name }
            val escapedName = championName.replace("\"", "\\\"")
            val response = leaguepediaApi.queryItemBuilds(
                where = "ScoreboardPlayers.Champion=\"$escapedName\" AND ScoreboardPlayers.Items IS NOT NULL AND ScoreboardPlayers.Items!=\"\"",
                limit = RECENT_GAMES_QUERIED,
            )
            if (response.error != null) {
                error("Leaguepedia error ${response.error.code}: ${response.error.info}")
            }

            val itemCounts = mutableMapOf<String, Int>()
            var sampleGames = 0
            for (row in response.cargoquery) {
                val itemNames = row.title.Items?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() }
                if (itemNames.isNullOrEmpty()) continue
                sampleGames++
                itemNames.forEach { name -> itemCounts[name] = (itemCounts[name] ?: 0) + 1 }
            }

            val topItems = itemCounts.entries
                .sortedByDescending { it.value }
                .mapNotNull { (name, _) -> itemsByName[name] }
                .take(BUILD_SIZE)

            val result = if (sampleGames == 0 || topItems.isEmpty()) null else ProBuild(topItems, sampleGames)

            staticDataDao.upsert(
                StaticDataCacheEntity(
                    key = cacheKey,
                    version = version,
                    json = jsonFormat.encodeToString(
                        CachedProBuildDto.serializer(),
                        CachedProBuildDto(topItems.map { it.itemId }, sampleGames),
                    ),
                    fetchedAt = System.currentTimeMillis(),
                )
            )

            result
        }
    }

    private fun StaticDataCacheEntity.toProBuild(itemsById: Map<Int, com.veronezzi.riftlog.domain.model.ItemInfo>): ProBuild? {
        val cachedDto = jsonFormat.decodeFromString(CachedProBuildDto.serializer(), this.json)
        if (cachedDto.sampleGames == 0 || cachedDto.itemIds.isEmpty()) return null
        val items = cachedDto.itemIds.mapNotNull { itemsById[it] }
        return if (items.isEmpty()) null else ProBuild(items, cachedDto.sampleGames)
    }
}
