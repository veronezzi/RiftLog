package com.veronezzi.riftlog.data.local.entities

/**
 * Generic key/value cache for Data Dragon static data (champion list, item list, per-champion
 * detail json...), keyed by a cache key that already embeds the patch version
 * (e.g. "champion_list_14.1.1"). No TTL - static data only changes per patch, so a new version
 * simply gets a new key and callers compare against the latest known version separately.
 */
data class StaticDataCacheEntity(
    val key: String,
    val version: String,
    val json: String,
    val fetchedAt: Long,
)
