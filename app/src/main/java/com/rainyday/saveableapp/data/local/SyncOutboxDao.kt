package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    /** Replaces any pending row for the same (entityType, entityId) — see the unique index. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox ORDER BY enqueuedAt ASC LIMIT :limit")
    suspend fun peekBatch(limit: Int): List<SyncOutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)")
    suspend fun clear(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun pendingCount(): Int
}
