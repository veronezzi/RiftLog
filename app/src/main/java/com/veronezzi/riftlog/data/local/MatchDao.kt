package com.veronezzi.riftlog.data.local

import android.content.ContentValues
import com.veronezzi.riftlog.data.local.entities.CachedMatchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MatchDao(private val dbHelper: RiftLogDbHelper) {

    suspend fun upsertMatches(matches: List<CachedMatchEntity>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (match in matches) {
                db.replace("cached_matches", null, match.toContentValues())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun getMatchesForPuuid(puuid: String): List<CachedMatchEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "cached_matches", null, "ownerPuuid = ?", arrayOf(puuid), null, null, "gameCreation DESC"
        ).use { it.toEntityList() }
    }

    suspend fun getCachedMatchIds(puuid: String): List<String> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "cached_matches", arrayOf("matchId"), "ownerPuuid = ?", arrayOf(puuid), null, null, null
        ).use { cursor ->
            val ids = mutableListOf<String>()
            while (cursor.moveToNext()) {
                ids += cursor.getString(cursor.getColumnIndexOrThrow("matchId"))
            }
            ids
        }
    }

    suspend fun getMatchesForChampion(puuid: String, championName: String): List<CachedMatchEntity> =
        withContext(Dispatchers.IO) {
            dbHelper.readableDatabase.query(
                "cached_matches",
                null,
                "ownerPuuid = ? AND championName = ?",
                arrayOf(puuid, championName),
                null,
                null,
                "gameCreation DESC",
            ).use { it.toEntityList() }
        }

    private fun android.database.Cursor.toEntityList(): List<CachedMatchEntity> {
        val results = mutableListOf<CachedMatchEntity>()
        while (moveToNext()) {
            results += CachedMatchEntity(
                matchId = getString(getColumnIndexOrThrow("matchId")),
                ownerPuuid = getString(getColumnIndexOrThrow("ownerPuuid")),
                championName = getString(getColumnIndexOrThrow("championName")),
                kills = getInt(getColumnIndexOrThrow("kills")),
                deaths = getInt(getColumnIndexOrThrow("deaths")),
                assists = getInt(getColumnIndexOrThrow("assists")),
                win = getInt(getColumnIndexOrThrow("win")) != 0,
                item0 = getInt(getColumnIndexOrThrow("item0")),
                item1 = getInt(getColumnIndexOrThrow("item1")),
                item2 = getInt(getColumnIndexOrThrow("item2")),
                item3 = getInt(getColumnIndexOrThrow("item3")),
                item4 = getInt(getColumnIndexOrThrow("item4")),
                item5 = getInt(getColumnIndexOrThrow("item5")),
                item6 = getInt(getColumnIndexOrThrow("item6")),
                summoner1Id = getInt(getColumnIndexOrThrow("summoner1Id")),
                summoner2Id = getInt(getColumnIndexOrThrow("summoner2Id")),
                teamPosition = getString(getColumnIndexOrThrow("teamPosition")),
                totalMinionsKilled = getInt(getColumnIndexOrThrow("totalMinionsKilled")),
                goldEarned = getInt(getColumnIndexOrThrow("goldEarned")),
                gameDuration = getLong(getColumnIndexOrThrow("gameDuration")),
                gameCreation = getLong(getColumnIndexOrThrow("gameCreation")),
                fetchedAt = getLong(getColumnIndexOrThrow("fetchedAt")),
            )
        }
        return results
    }

    private fun CachedMatchEntity.toContentValues() = ContentValues().apply {
        put("matchId", matchId)
        put("ownerPuuid", ownerPuuid)
        put("championName", championName)
        put("kills", kills)
        put("deaths", deaths)
        put("assists", assists)
        put("win", if (win) 1 else 0)
        put("item0", item0)
        put("item1", item1)
        put("item2", item2)
        put("item3", item3)
        put("item4", item4)
        put("item5", item5)
        put("item6", item6)
        put("summoner1Id", summoner1Id)
        put("summoner2Id", summoner2Id)
        put("teamPosition", teamPosition)
        put("totalMinionsKilled", totalMinionsKilled)
        put("goldEarned", goldEarned)
        put("gameDuration", gameDuration)
        put("gameCreation", gameCreation)
        put("fetchedAt", fetchedAt)
    }
}
