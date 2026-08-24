package com.veronezzi.riftlog.data.local

import android.content.ContentValues
import com.veronezzi.riftlog.data.local.entities.FullMatchCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Caches the entire match-v5 response (all participants + teams) as a JSON blob, keyed by
 * matchId - same shape as StaticDataDao. Separate from `cached_matches`, which only ever holds
 * the tracked player's own row and must keep working unchanged. */
class FullMatchDao(private val dbHelper: RiftLogDbHelper) {

    suspend fun upsert(entity: FullMatchCacheEntity) = upsertAll(listOf(entity))

    /** Batches every insert into one transaction - upserting one at a time in a loop would
     * otherwise be a separate uncommitted disk write per match on a multi-match refresh, unlike
     * MatchDao's batch upsert. */
    suspend fun upsertAll(entities: List<FullMatchCacheEntity>) = withContext(Dispatchers.IO) {
        if (entities.isEmpty()) return@withContext
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (entity in entities) {
                val values = ContentValues().apply {
                    put("matchId", entity.matchId)
                    put("json", entity.json)
                    put("fetchedAt", entity.fetchedAt)
                }
                db.replace("full_match_cache", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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
