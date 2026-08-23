package com.veronezzi.riftlog.data.local

import android.content.ContentValues
import com.veronezzi.riftlog.data.local.entities.FullMatchCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Caches the entire match-v5 response (all participants + teams) as a JSON blob, keyed by
 * matchId - same shape as StaticDataDao. Separate from `cached_matches`, which only ever holds
 * the tracked player's own row and must keep working unchanged. */
class FullMatchDao(private val dbHelper: RiftLogDbHelper) {

    suspend fun upsert(entity: FullMatchCacheEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("matchId", entity.matchId)
            put("json", entity.json)
            put("fetchedAt", entity.fetchedAt)
        }
        dbHelper.writableDatabase.replace("full_match_cache", null, values)
    }

    suspend fun get(matchId: String): FullMatchCacheEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "full_match_cache", null, "matchId = ?", arrayOf(matchId), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            FullMatchCacheEntity(
                matchId = cursor.getString(cursor.getColumnIndexOrThrow("matchId")),
                json = cursor.getString(cursor.getColumnIndexOrThrow("json")),
                fetchedAt = cursor.getLong(cursor.getColumnIndexOrThrow("fetchedAt")),
            )
        }
    }
}
