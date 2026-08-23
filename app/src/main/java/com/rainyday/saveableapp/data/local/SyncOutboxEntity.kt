package com.rainyday.saveableapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SyncOperation { UPSERT, DELETE }

/**
 * Every synced Room table, mirroring the collection names used under `users/{uid}/…` in Firestore.
 * Task-tag membership (`TaskTagCrossRef`) has no entry here — it's embedded as a `tagIds` array on
 * the TODO_TASK document instead of its own collection; see [com.rainyday.saveableapp.data.repository.TodoRepository].
 */
enum class SyncEntityType {
    TODO_LIST, TODO_TASK, TAG,
    SIMPLE_LIST, SIMPLE_LIST_ITEM,
    INFO_CATEGORY, INFO_BLOCK,
    FLASH_CARD_DECK, FLASH_CARD,
    FIELD_DEFINITION, FIELD_VALUE
}

/**
 * A durable local queue of not-yet-pushed changes, drained by `SyncWorker`. Local-only — never
 * synced itself, never included in [com.rainyday.saveableapp.data.backup.BackupPayload].
 *
 * Re-enqueuing the same (entityType, entityId) before it's drained just replaces the pending row
 * (see the DAO's upsert), so rapid edits collapse into a single pending push instead of growing
 * unboundedly.
 */
@Entity(tableName = "sync_outbox", indices = [Index(value = ["entityType", "entityId"], unique = true)])
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: SyncEntityType,
    val entityId: String,
    val operation: SyncOperation,
    val enqueuedAt: Long
)
