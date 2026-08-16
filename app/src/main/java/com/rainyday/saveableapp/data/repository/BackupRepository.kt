package com.rainyday.saveableapp.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.rainyday.saveableapp.data.backup.BackupPayload
import com.rainyday.saveableapp.data.local.AppDatabase
import com.rainyday.saveableapp.data.local.BackupDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupRepository(
    private val database: AppDatabase,
    private val dao: BackupDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun buildPayload(): BackupPayload = withContext(Dispatchers.IO) {
        BackupPayload(
            exportedAt = System.currentTimeMillis(),
            todoLists = dao.getAllTodoLists(),
            todoTasks = dao.getAllTodoTasks(),
            tags = dao.getAllTags(),
            taskTagCrossRefs = dao.getAllTaskTagCrossRefs(),
            simpleLists = dao.getAllSimpleLists(),
            simpleListItems = dao.getAllSimpleListItems(),
            infoCategories = dao.getAllInfoCategories(),
            infoBlocks = dao.getAllInfoBlocks(),
            flashCardDecks = dao.getAllFlashCardDecks(),
            flashCards = dao.getAllFlashCards(),
            fieldDefinitions = dao.getAllFieldDefinitions(),
            fieldValues = dao.getAllFieldValues()
        )
    }

    fun encode(payload: BackupPayload): String = json.encodeToString(BackupPayload.serializer(), payload)

    fun decode(text: String): BackupPayload = json.decodeFromString(BackupPayload.serializer(), text)

    suspend fun restorePayload(payload: BackupPayload) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.clearTaskTagCrossRefs()
            dao.clearTodoTasks()
            dao.clearTodoLists()
            dao.clearTags()
            dao.clearFieldValues()
            dao.clearFieldDefinitions()
            dao.clearSimpleListItems()
            dao.clearSimpleLists()
            dao.clearInfoBlocks()
            dao.clearInfoCategories()
            dao.clearFlashCards()
            dao.clearFlashCardDecks()

            dao.insertTodoLists(payload.todoLists)
            dao.insertTags(payload.tags)
            dao.insertTodoTasks(payload.todoTasks)
            dao.insertTaskTagCrossRefs(payload.taskTagCrossRefs)
            dao.insertSimpleLists(payload.simpleLists)
            dao.insertSimpleListItems(payload.simpleListItems)
            dao.insertInfoCategories(payload.infoCategories)
            dao.insertInfoBlocks(payload.infoBlocks)
            dao.insertFlashCardDecks(payload.flashCardDecks)
            dao.insertFlashCards(payload.flashCards)
            dao.insertFieldDefinitions(payload.fieldDefinitions)
            dao.insertFieldValues(payload.fieldValues)
        }
    }

    suspend fun exportToUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val text = encode(buildPayload())
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: return@withContext
        restorePayload(decode(text))
    }
}
