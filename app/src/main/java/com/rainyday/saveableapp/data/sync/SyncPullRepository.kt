package com.rainyday.saveableapp.data.sync

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rainyday.saveableapp.data.local.FieldDefinitionDao
import com.rainyday.saveableapp.data.local.FieldValueDao
import com.rainyday.saveableapp.data.local.FlashCardDao
import com.rainyday.saveableapp.data.local.FlashCardDeckDao
import com.rainyday.saveableapp.data.local.InfoBlockDao
import com.rainyday.saveableapp.data.local.InfoCategoryDao
import com.rainyday.saveableapp.data.local.SimpleListDao
import com.rainyday.saveableapp.data.local.SimpleListItemDao
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.local.TagDao
import com.rainyday.saveableapp.data.local.TaskTagCrossRef
import com.rainyday.saveableapp.data.local.TodoListDao
import com.rainyday.saveableapp.data.local.TodoTaskDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "SyncPullRepository"
private const val MAX_UPSERT_ATTEMPTS = 8

/** An upsert whose parent row (FK) didn't exist locally yet — Firestore's per-collection listeners
 * fire independently, so a child document can easily arrive before its parent has synced down. */
private class PendingUpsert(val type: SyncEntityType, val data: Map<String, Any?>, var attempts: Int = 0)

/**
 * Listens to every synced Firestore collection under `users/{uid}/…` and applies incoming changes
 * to Room directly through the DAOs — never through the repositories, so pulled changes don't loop
 * back into [SyncOutbox]. Conflict resolution is last-write-wins by `updatedAt`: a remote change is
 * only applied if it's strictly newer than the local row, which is also what makes this safe against
 * echoes of our own pushes (they come back with the exact `updatedAt` we just wrote, so they're
 * skipped as "not newer").
 */
class SyncPullRepository(
    private val firestore: FirebaseFirestore,
    private val todoListDao: TodoListDao,
    private val todoTaskDao: TodoTaskDao,
    private val tagDao: TagDao,
    private val simpleListDao: SimpleListDao,
    private val simpleListItemDao: SimpleListItemDao,
    private val infoCategoryDao: InfoCategoryDao,
    private val infoBlockDao: InfoBlockDao,
    private val flashCardDeckDao: FlashCardDeckDao,
    private val flashCardDao: FlashCardDao,
    private val fieldDefinitionDao: FieldDefinitionDao,
    private val fieldValueDao: FieldValueDao
) {
    private var scopeJob: Job? = null
    private var scope: CoroutineScope? = null
    private val registrations = mutableListOf<ListenerRegistration>()
    private val pendingMutex = Mutex()
    private val pendingUpserts = mutableListOf<PendingUpsert>()

    fun start(uid: String) {
        stop()
        val job = SupervisorJob()
        scopeJob = job
        val activeScope = CoroutineScope(Dispatchers.IO + job)
        scope = activeScope

        val userDoc = firestore.collection("users").document(uid)
        SyncEntityType.entries.forEach { type ->
            val registration = userDoc.collection(type.firestoreCollectionName())
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    activeScope.launch { applyChanges(type, snapshot.documentChanges) }
                }
            registrations += registration
        }
    }

    fun stop() {
        registrations.forEach { it.remove() }
        registrations.clear()
        scopeJob?.cancel()
        scopeJob = null
        scope = null
        pendingUpserts.clear()
    }

    private suspend fun applyChanges(type: SyncEntityType, changes: List<DocumentChange>) {
        // A previously-blocked child's parent may have arrived via a different collection's
        // listener since the last flush, so always try the backlog before new work too.
        flushPendingUpserts()
        for (change in changes) {
            val id = change.document.id
            when (change.type) {
                DocumentChange.Type.REMOVED -> applyRemoteDelete(type, id)
                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                    applyUpsertOrQueue(type, change.document.data)
            }
        }
        flushPendingUpserts()
    }

    /** Applies the upsert; if it fails on a missing-parent FK, queues it for [flushPendingUpserts] instead of crashing. */
    private suspend fun applyUpsertOrQueue(type: SyncEntityType, data: Map<String, Any?>) {
        try {
            applyRemoteUpsert(type, data)
        } catch (e: SQLiteConstraintException) {
            pendingMutex.withLock { pendingUpserts += PendingUpsert(type, data) }
        }
    }

    private suspend fun flushPendingUpserts() {
        val toRetry = pendingMutex.withLock { pendingUpserts.toList() }
        for (item in toRetry) {
            try {
                applyRemoteUpsert(item.type, item.data)
                pendingMutex.withLock { pendingUpserts.removeAll { it === item } }
            } catch (e: SQLiteConstraintException) {
                item.attempts++
                if (item.attempts >= MAX_UPSERT_ATTEMPTS) {
                    Log.w(TAG, "Giving up on ${item.type} after $MAX_UPSERT_ATTEMPTS attempts — parent never arrived.")
                    pendingMutex.withLock { pendingUpserts.removeAll { it === item } }
                }
            }
        }
    }

    private suspend fun applyRemoteDelete(type: SyncEntityType, id: String) {
        when (type) {
            SyncEntityType.TODO_LIST -> todoListDao.deleteById(id)
            SyncEntityType.TODO_TASK -> todoTaskDao.deleteById(id)
            SyncEntityType.TAG -> tagDao.deleteById(id)
            SyncEntityType.SIMPLE_LIST -> simpleListDao.deleteById(id)
            SyncEntityType.SIMPLE_LIST_ITEM -> simpleListItemDao.deleteById(id)
            SyncEntityType.INFO_CATEGORY -> infoCategoryDao.deleteById(id)
            SyncEntityType.INFO_BLOCK -> infoBlockDao.deleteById(id)
            SyncEntityType.FLASH_CARD_DECK -> flashCardDeckDao.deleteById(id)
            SyncEntityType.FLASH_CARD -> flashCardDao.deleteById(id)
            SyncEntityType.FIELD_DEFINITION -> fieldDefinitionDao.deleteById(id)
            SyncEntityType.FIELD_VALUE -> fieldValueDao.deleteById(id)
        }
    }

    private suspend fun applyRemoteUpsert(type: SyncEntityType, data: Map<String, Any?>) {
        when (type) {
            SyncEntityType.TODO_LIST -> {
                val remote = data.toTodoListEntity()
                val local = todoListDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) todoListDao.insert(remote) else todoListDao.update(remote)
            }
            SyncEntityType.TODO_TASK -> {
                val remote = data.toTodoTaskEntity()
                val local = todoTaskDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) todoTaskDao.insert(remote) else todoTaskDao.update(remote)
                // Tags belong to whichever tags currently exist locally — a tag id this device
                // hasn't pulled down yet is silently dropped rather than crashing on the FK.
                tagDao.clearTagsForTask(remote.id)
                data.remoteTagIds().forEach { tagId ->
                    if (tagDao.getById(tagId) != null) tagDao.insertCrossRef(TaskTagCrossRef(remote.id, tagId))
                }
            }
            SyncEntityType.TAG -> {
                val remote = data.toTagEntity()
                val local = tagDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) tagDao.insert(remote) else tagDao.update(remote)
            }
            SyncEntityType.SIMPLE_LIST -> {
                val remote = data.toSimpleListEntity()
                val local = simpleListDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) simpleListDao.insert(remote) else simpleListDao.update(remote)
            }
            SyncEntityType.SIMPLE_LIST_ITEM -> {
                val remote = data.toSimpleListItemEntity()
                val local = simpleListItemDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) simpleListItemDao.insert(remote) else simpleListItemDao.update(remote)
            }
            SyncEntityType.INFO_CATEGORY -> {
                val remote = data.toInfoCategoryEntity()
                val local = infoCategoryDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) infoCategoryDao.insert(remote) else infoCategoryDao.update(remote)
            }
            SyncEntityType.INFO_BLOCK -> {
                val remote = data.toInfoBlockEntity()
                val local = infoBlockDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) infoBlockDao.insert(remote) else infoBlockDao.update(remote)
            }
            SyncEntityType.FLASH_CARD_DECK -> {
                val remote = data.toFlashCardDeckEntity()
                val local = flashCardDeckDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) flashCardDeckDao.insert(remote) else flashCardDeckDao.update(remote)
            }
            SyncEntityType.FLASH_CARD -> {
                val remote = data.toFlashCardEntity()
                val local = flashCardDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) flashCardDao.insert(remote) else flashCardDao.update(remote)
            }
            SyncEntityType.FIELD_DEFINITION -> {
                val remote = data.toFieldDefinitionEntity()
                val local = fieldDefinitionDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) fieldDefinitionDao.insert(remote) else fieldDefinitionDao.update(remote)
            }
            SyncEntityType.FIELD_VALUE -> {
                val remote = data.toFieldValueEntity()
                val local = fieldValueDao.getById(remote.id)
                if (isStale(local?.updatedAt, remote.updatedAt)) return
                if (local == null) fieldValueDao.insertAll(listOf(remote)) else fieldValueDao.update(remote)
            }
        }
    }

    /** True if the remote write isn't newer than what's already local (includes our own echoed-back pushes). */
    private fun isStale(localUpdatedAt: Long?, remoteUpdatedAt: Long): Boolean =
        localUpdatedAt != null && remoteUpdatedAt <= localUpdatedAt
}
