package com.rainyday.saveableapp.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.rainyday.saveableapp.data.auth.FirebaseAuthRepository
import com.rainyday.saveableapp.data.local.FieldDefinitionDao
import com.rainyday.saveableapp.data.local.FieldValueDao
import com.rainyday.saveableapp.data.local.FlashCardDao
import com.rainyday.saveableapp.data.local.FlashCardDeckDao
import com.rainyday.saveableapp.data.local.InfoBlockDao
import com.rainyday.saveableapp.data.local.InfoCategoryDao
import com.rainyday.saveableapp.data.local.SimpleListDao
import com.rainyday.saveableapp.data.local.SimpleListItemDao
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.local.SyncOperation
import com.rainyday.saveableapp.data.local.SyncOutboxDao
import com.rainyday.saveableapp.data.local.SyncOutboxEntity
import com.rainyday.saveableapp.data.local.TagDao
import com.rainyday.saveableapp.data.local.TodoListDao
import com.rainyday.saveableapp.data.local.TodoTaskDao
import kotlinx.coroutines.tasks.await

private const val BATCH_SIZE = 400 // headroom under Firestore's 500-write batch limit

/**
 * Drains [SyncOutboxDao] into Firestore, batched. UPSERTs re-read the entity's *current* row at
 * push time (the outbox only ever stored the id + operation, not a snapshot) — if the row is gone
 * by then, the entity was deleted after being queued and this is just skipped, since a DELETE entry
 * for it either already ran or is still queued behind this one.
 */
class SyncPushRepository(
    private val outboxDao: SyncOutboxDao,
    private val firebaseAuthRepository: FirebaseAuthRepository,
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
    /** Pushes everything currently queued, in batches. Returns the number of entries pushed. */
    suspend fun pushPending(): Int {
        val uid = firebaseAuthRepository.currentUser?.uid ?: return 0
        var totalPushed = 0
        while (true) {
            val pending = outboxDao.peekBatch(BATCH_SIZE)
            if (pending.isEmpty()) break
            pushBatch(uid, pending)
            outboxDao.clear(pending.map { it.id })
            totalPushed += pending.size
        }
        return totalPushed
    }

    private suspend fun pushBatch(uid: String, entries: List<SyncOutboxEntity>) {
        val batch = firestore.batch()
        val userDoc = firestore.collection("users").document(uid)
        for (entry in entries) {
            val collection = userDoc.collection(entry.entityType.firestoreCollectionName())
            val docRef = collection.document(entry.entityId)
            when (entry.operation) {
                SyncOperation.DELETE -> batch.delete(docRef)
                SyncOperation.UPSERT -> {
                    val map = buildDocument(entry.entityType, entry.entityId) ?: continue
                    batch.set(docRef, map)
                }
            }
        }
        batch.commit().await()
    }

    private suspend fun buildDocument(type: SyncEntityType, id: String): Map<String, Any?>? = when (type) {
        SyncEntityType.TODO_LIST -> todoListDao.getById(id)?.toFirestoreMap()
        SyncEntityType.TODO_TASK -> todoTaskDao.getByIdWithTags(id)?.toFirestoreMap()
        SyncEntityType.TAG -> tagDao.getById(id)?.toFirestoreMap()
        SyncEntityType.SIMPLE_LIST -> simpleListDao.getById(id)?.toFirestoreMap()
        SyncEntityType.SIMPLE_LIST_ITEM -> simpleListItemDao.getById(id)?.toFirestoreMap()
        SyncEntityType.INFO_CATEGORY -> infoCategoryDao.getById(id)?.toFirestoreMap()
        SyncEntityType.INFO_BLOCK -> infoBlockDao.getById(id)?.toFirestoreMap()
        SyncEntityType.FLASH_CARD_DECK -> flashCardDeckDao.getById(id)?.toFirestoreMap()
        SyncEntityType.FLASH_CARD -> flashCardDao.getById(id)?.toFirestoreMap()
        SyncEntityType.FIELD_DEFINITION -> fieldDefinitionDao.getById(id)?.toFirestoreMap()
        SyncEntityType.FIELD_VALUE -> fieldValueDao.getById(id)?.toFirestoreMap()
    }
}
