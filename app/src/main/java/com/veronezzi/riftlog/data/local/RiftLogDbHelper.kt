package com.veronezzi.riftlog.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Plain SQLiteOpenHelper cache, no ORM. Room's KSP compiler currently can't build against
 * AGP 9's built-in Kotlin (KSP hard-refuses to run under it, and the classic Kotlin Gradle
 * plugin can't be applied under AGP 9 either), so this app does the couple of tables by hand
 * instead - same shape (a few tables + a timestamp column per row), no annotation processor.
 */
class RiftLogDbHelper(context: Context) : SQLiteOpenHelper(context, "riftlog.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cached_profiles (
                riotIdKey TEXT PRIMARY KEY,
                platformRegion TEXT NOT NULL,
                puuid TEXT NOT NULL,
                gameName TEXT NOT NULL,
                tagLine TEXT NOT NULL,
                profileIconId INTEGER NOT NULL,
                summonerLevel INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cached_rank_entries (
                puuid TEXT NOT NULL,
                queueType TEXT NOT NULL,
                tier TEXT NOT NULL,
                rank TEXT NOT NULL,
                leaguePoints INTEGER NOT NULL,
                wins INTEGER NOT NULL,
                losses INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL,
                PRIMARY KEY (puuid, queueType)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cached_matches (
                matchId TEXT NOT NULL,
                ownerPuuid TEXT NOT NULL,
                championName TEXT NOT NULL,
                kills INTEGER NOT NULL,
                deaths INTEGER NOT NULL,
                assists INTEGER NOT NULL,
                win INTEGER NOT NULL,
                item0 INTEGER NOT NULL,
                item1 INTEGER NOT NULL,
                item2 INTEGER NOT NULL,
                item3 INTEGER NOT NULL,
                item4 INTEGER NOT NULL,
                item5 INTEGER NOT NULL,
                item6 INTEGER NOT NULL,
                summoner1Id INTEGER NOT NULL,
                summoner2Id INTEGER NOT NULL,
                teamPosition TEXT NOT NULL,
                totalMinionsKilled INTEGER NOT NULL,
                goldEarned INTEGER NOT NULL,
                gameDuration INTEGER NOT NULL,
                gameCreation INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL,
                PRIMARY KEY (matchId, ownerPuuid)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cached_masteries (
                puuid TEXT NOT NULL,
                championId INTEGER NOT NULL,
                championLevel INTEGER NOT NULL,
                championPoints INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL,
                PRIMARY KEY (puuid, championId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE static_data_cache (
                key TEXT PRIMARY KEY,
                version TEXT NOT NULL,
                json TEXT NOT NULL,
                fetchedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No prior versions yet.
    }

    /** Wipes every cache table. Used by Settings' "clear cached data" action. */
    fun clearAll() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (table in listOf(
                "cached_profiles", "cached_rank_entries", "cached_matches",
                "cached_masteries", "static_data_cache",
            )) {
                db.delete(table, null, null)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
