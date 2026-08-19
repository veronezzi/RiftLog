package com.veronezzi.riftlog.data.local

import android.content.ContentValues
import com.veronezzi.riftlog.data.local.entities.CachedMasteryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MasteryDao(private val dbHelper: RiftLogDbHelper) {

    suspend fun upsertMasteries(masteries: List<CachedMasteryEntity>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (mastery in masteries) {
                val values = ContentValues().apply {
                    put("puuid", mastery.puuid)
                    put("championId", mastery.championId)
                    put("championLevel", mastery.championLevel)
                    put("championPoints", mastery.championPoints)
                    put("fetchedAt", mastery.fetchedAt)
                }
                db.replace("cached_masteries", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun getMasteriesForPuuid(puuid: String): List<CachedMasteryEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "cached_masteries", null, "puuid = ?", arrayOf(puuid), null, null, "championPoints DESC"
        ).use { cursor ->
            val results = mutableListOf<CachedMasteryEntity>()
            while (cursor.moveToNext()) {
                results += CachedMasteryEntity(
                    puuid = cursor.getString(cursor.getColumnIndexOrThrow("puuid")),
                    championId = cursor.getLong(cursor.getColumnIndexOrThrow("championId")),
                    championLevel = cursor.getInt(cursor.getColumnIndexOrThrow("championLevel")),
                    championPoints = cursor.getLong(cursor.getColumnIndexOrThrow("championPoints")),
                    fetchedAt = cursor.getLong(cursor.getColumnIndexOrThrow("fetchedAt")),
                )
            }
            results
        }
    }
}
