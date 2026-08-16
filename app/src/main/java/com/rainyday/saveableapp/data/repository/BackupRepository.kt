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

    suspend fun exportToUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            todoLists = dao.getAllTodoLists(),
            todoTasks = dao.getAllTodoTasks(),
            tags = dao.getAllTags(),
            taskTagCrossRefs = dao.getAllTaskTagCrossRefs(),
            simpleLists = dao.getAllSimpleLists(),
            simpleListItems = dao.getAllSimpleListItems(),
            infoCategories = dao.getAllInfoCategories(),
            infoBlocks = dao.getAllInfoBlocks()
        )
        val text = json.encodeToString(BackupPayload.serializer(), payload)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: return@withContext
        val payload = json.decodeFromString(BackupPayload.serializer(), text)
        database.withTransaction {
            dao.clearTaskTagCrossRefs()
            dao.clearTodoTasks()
            dao.clearTodoLists()
            dao.clearTags()
            dao.clearSimpleListItems()
            dao.clearSimpleLists()
            dao.clearInfoBlocks()
            dao.clearInfoCategories()

            dao.insertTodoLists(payload.todoLists)
            dao.insertTags(payload.tags)
            dao.insertTodoTasks(payload.todoTasks)
            dao.insertTaskTagCrossRefs(payload.taskTagCrossRefs)
            dao.insertSimpleLists(payload.simpleLists)
            dao.insertSimpleListItems(payload.simpleListItems)
            dao.insertInfoCategories(payload.infoCategories)
            dao.insertInfoBlocks(payload.infoBlocks)
        }
    }
}
