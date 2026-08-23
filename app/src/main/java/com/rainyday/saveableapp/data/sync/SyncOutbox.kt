package com.rainyday.saveableapp.data.sync

import androidx.work.WorkManager
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.local.SyncOperation
import com.rainyday.saveableapp.data.local.SyncOutboxDao
import com.rainyday.saveableapp.data.local.SyncOutboxEntity

/**
 * Thin wrapper repositories call after a local write commits, to enqueue it for [SyncWorker]'s
 * push path (and wake that worker up promptly, rather than waiting for its periodic run).
 * Enqueuing the same (type, id) again before it's drained just replaces the pending row (see
 * [SyncOutboxDao.enqueue]'s unique index), so bursts of edits collapse into one push.
 */
class SyncOutbox(private val dao: SyncOutboxDao, private val workManager: WorkManager) {
    suspend fun upsert(type: SyncEntityType, id: String) = enqueue(type, id, SyncOperation.UPSERT)

    suspend fun upsertAll(type: SyncEntityType, ids: Collection<String>) {
        ids.forEach { upsert(type, it) }
    }

    suspend fun delete(type: SyncEntityType, id: String) = enqueue(type, id, SyncOperation.DELETE)

    suspend fun deleteAll(type: SyncEntityType, ids: Collection<String>) {
        ids.forEach { delete(type, it) }
    }

    private suspend fun enqueue(type: SyncEntityType, id: String, operation: SyncOperation) {
        dao.enqueue(
            SyncOutboxEntity(
                entityType = type,
                entityId = id,
                operation = operation,
                enqueuedAt = System.currentTimeMillis()
            )
        )
        SyncWorker.triggerNow(workManager)
    }
}
