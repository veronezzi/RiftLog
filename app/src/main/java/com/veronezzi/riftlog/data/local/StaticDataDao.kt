package com.veronezzi.riftlog.data.local

import android.content.ContentValues
import com.veronezzi.riftlog.data.local.entities.StaticDataCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StaticDataDao(private val dbHelper: RiftLogDbHelper) {

    suspend fun upsert(entity: StaticDataCacheEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("key", entity.key)
            put("version", entity.version)
            put("json", entity.json)
            put("fetchedAt", entity.fetchedAt)
        }
        dbHelper.writableDatabase.replace("static_data_cache", null, values)
    }

    suspend fun get(key: String): StaticDataCacheEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "static_data_cache", null, "key = ?", arrayOf(key), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            StaticDataCacheEntity(
                key = cursor.getString(cursor.getColumnIndexOrThrow("key")),
                version = cursor.getString(cursor.getColumnIndexOrThrow("version")),
                json = cursor.getString(cursor.getColumnIndexOrThrow("json")),
                fetchedAt = cursor.getLong(cursor.getColumnIndexOrThrow("fetchedAt")),
            )
        }
    }
}
