package com.veronezzi.riftlog.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.veronezzi.riftlog.data.local.entities.RankSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Timestamped rank/LP history, one row per real profile fetch per queue - powers the rank
 * progression graph on the Profile screen. */
class RankHistoryDao(private val dbHelper: RiftLogDbHelper) {

    /** Only inserts when the rank actually differs from the most recently stored snapshot for
     * this puuid+queue. Without this, repeatedly re-fetching an unchanged rank (e.g. mashing
     * retry, or reopening the profile after the 5-minute cache expires but before any game was
     * played) would spam near-duplicate points that add nothing to the graph. */
    suspend fun recordSnapshotIfChanged(snapshot: RankSnapshotEntity) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val last = db.query(
            "rank_snapshots", arrayOf("tier", "rank", "leaguePoints"),
            "puuid = ? AND queueType = ?", arrayOf(snapshot.puuid, snapshot.queueType),
            // rowid as the tiebreaker (not just timestamp) so a device clock jumping backwards
            // can't make an older-timestamped-but-earlier-inserted row look "most recent".
            null, null, "timestamp DESC, rowid DESC", "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) null
            else Triple(cursor.getString(0), cursor.getString(1), cursor.getInt(2))
        }
        if (last == Triple(snapshot.tier, snapshot.rank, snapshot.leaguePoints)) {
            return@withContext
        }

        db.insert(
            "rank_snapshots", null,
            ContentValues().apply {
                put("puuid", snapshot.puuid)
                put("queueType", snapshot.queueType)
                put("tier", snapshot.tier)
                put("rank", snapshot.rank)
                put("leaguePoints", snapshot.leaguePoints)
                put("timestamp", snapshot.timestamp)
            }
        )
        pruneOldSnapshots(db, snapshot.puuid, snapshot.queueType)
    }

    /** Keeps at most [MAX_SNAPSHOTS_PER_QUEUE] rows per puuid+queue so the table doesn't grow
     * unbounded for a player tracked over years. */
    private fun pruneOldSnapshots(db: SQLiteDatabase, puuid: String, queueType: String) {
        db.execSQL(
            """
            DELETE FROM rank_snapshots WHERE puuid = ? AND queueType = ? AND rowid NOT IN (
                SELECT rowid FROM rank_snapshots WHERE puuid = ? AND queueType = ?
                ORDER BY timestamp DESC, rowid DESC LIMIT $MAX_SNAPSHOTS_PER_QUEUE
            )
            """.trimIndent(),
            arrayOf(puuid, queueType, puuid, queueType)
        )
    }

    /** Returns up to [MAX_SNAPSHOTS_PER_QUEUE] snapshots, oldest first (chronological order for
     * plotting). */
    suspend fun getSnapshots(puuid: String, queueType: String): List<RankSnapshotEntity> =
        withContext(Dispatchers.IO) {
            dbHelper.readableDatabase.query(
                "rank_snapshots", null, "puuid = ? AND queueType = ?", arrayOf(puuid, queueType),
                null, null, "timestamp DESC, rowid DESC", MAX_SNAPSHOTS_PER_QUEUE.toString()
            ).use { cursor ->
                val results = mutableListOf<RankSnapshotEntity>()
                while (cursor.moveToNext()) {
                    results += RankSnapshotEntity(
                        puuid = cursor.getString(cursor.getColumnIndexOrThrow("puuid")),
                        queueType = cursor.getString(cursor.getColumnIndexOrThrow("queueType")),
                        tier = cursor.getString(cursor.getColumnIndexOrThrow("tier")),
                        rank = cursor.getString(cursor.getColumnIndexOrThrow("rank")),
                        leaguePoints = cursor.getInt(cursor.getColumnIndexOrThrow("leaguePoints")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    )
                }
                results.asReversed()
            }
        }

    companion object {
        private const val MAX_SNAPSHOTS_PER_QUEUE = 60
    }
}
