package com.example.riftlog.data.local

import android.content.ContentValues
import com.example.riftlog.data.local.entities.CachedProfileEntity
import com.example.riftlog.data.local.entities.CachedRankEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileDao(private val dbHelper: RiftLogDbHelper) {

    suspend fun upsertProfile(profile: CachedProfileEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("riotIdKey", profile.riotIdKey)
            put("platformRegion", profile.platformRegion)
            put("puuid", profile.puuid)
            put("gameName", profile.gameName)
            put("tagLine", profile.tagLine)
            put("profileIconId", profile.profileIconId)
            put("summonerLevel", profile.summonerLevel)
            put("fetchedAt", profile.fetchedAt)
        }
        dbHelper.writableDatabase.replace("cached_profiles", null, values)
    }

    suspend fun getProfile(riotIdKey: String): CachedProfileEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "cached_profiles", null, "riotIdKey = ?", arrayOf(riotIdKey), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            CachedProfileEntity(
                riotIdKey = cursor.getString(cursor.getColumnIndexOrThrow("riotIdKey")),
                platformRegion = cursor.getString(cursor.getColumnIndexOrThrow("platformRegion")),
                puuid = cursor.getString(cursor.getColumnIndexOrThrow("puuid")),
                gameName = cursor.getString(cursor.getColumnIndexOrThrow("gameName")),
                tagLine = cursor.getString(cursor.getColumnIndexOrThrow("tagLine")),
                profileIconId = cursor.getInt(cursor.getColumnIndexOrThrow("profileIconId")),
                summonerLevel = cursor.getLong(cursor.getColumnIndexOrThrow("summonerLevel")),
                fetchedAt = cursor.getLong(cursor.getColumnIndexOrThrow("fetchedAt")),
            )
        }
    }

    suspend fun upsertRankEntries(entries: List<CachedRankEntryEntity>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (entry in entries) {
                val values = ContentValues().apply {
                    put("puuid", entry.puuid)
                    put("queueType", entry.queueType)
                    put("tier", entry.tier)
                    put("rank", entry.rank)
                    put("leaguePoints", entry.leaguePoints)
                    put("wins", entry.wins)
                    put("losses", entry.losses)
                    put("fetchedAt", entry.fetchedAt)
                }
                db.replace("cached_rank_entries", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun clearRankEntries(puuid: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("cached_rank_entries", "puuid = ?", arrayOf(puuid))
        Unit
    }

    suspend fun getRankEntries(puuid: String): List<CachedRankEntryEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "cached_rank_entries", null, "puuid = ?", arrayOf(puuid), null, null, null
        ).use { cursor ->
            val results = mutableListOf<CachedRankEntryEntity>()
            while (cursor.moveToNext()) {
                results += CachedRankEntryEntity(
                    puuid = cursor.getString(cursor.getColumnIndexOrThrow("puuid")),
                    queueType = cursor.getString(cursor.getColumnIndexOrThrow("queueType")),
                    tier = cursor.getString(cursor.getColumnIndexOrThrow("tier")),
                    rank = cursor.getString(cursor.getColumnIndexOrThrow("rank")),
                    leaguePoints = cursor.getInt(cursor.getColumnIndexOrThrow("leaguePoints")),
                    wins = cursor.getInt(cursor.getColumnIndexOrThrow("wins")),
                    losses = cursor.getInt(cursor.getColumnIndexOrThrow("losses")),
                    fetchedAt = cursor.getLong(cursor.getColumnIndexOrThrow("fetchedAt")),
                )
            }
            results
        }
    }
}
