package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.backup.BackupPayload
import com.rainyday.saveableapp.db.AppDatabase
import com.rainyday.saveableapp.platform.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private fun com.rainyday.saveableapp.db.Task_tag_cross_ref.toEntity() =
    com.rainyday.saveableapp.data.local.TaskTagCrossRef(taskId = taskId, tagId = tagId)

class BackupRepository(private val db: AppDatabase) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(): String = withContext(Dispatchers.Default) {
        val payload = BackupPayload(
            exportedAt = nowMillis(),
            todoLists = db.todoListQueries.selectAll().executeAsList().map { it.toEntity() },
            todoTasks = db.todoTaskQueries.selectAllForBackup().executeAsList().map { it.toEntity() },
            tags = db.tagQueries.selectAll().executeAsList().map { it.toEntity() },
            taskTagCrossRefs = db.taskTagCrossRefQueries.selectAll().executeAsList().map { it.toEntity() },
            simpleLists = db.simpleListQueries.selectAll().executeAsList().map { it.toEntity() },
            simpleListItems = db.simpleListItemQueries.selectAllForBackup().executeAsList().map { it.toEntity() },
            infoCategories = db.infoCategoryQueries.selectAll().executeAsList().map { it.toEntity() },
            infoBlocks = db.infoBlockQueries.selectAllForBackup().executeAsList().map { it.toEntity() }
        )
        json.encodeToString(BackupPayload.serializer(), payload)
    }

    suspend fun importJson(content: String) = withContext(Dispatchers.Default) {
        val payload = json.decodeFromString(BackupPayload.serializer(), content)
        db.todoListQueries.transaction {
            db.taskTagCrossRefQueries.clear()
            db.todoTaskQueries.clear()
            db.todoListQueries.clear()
            db.tagQueries.clear()
            db.simpleListItemQueries.clear()
            db.simpleListQueries.clear()
            db.infoBlockQueries.clear()
            db.infoCategoryQueries.clear()

            payload.todoLists.forEach { list ->
                db.todoListQueries.insertOrReplace(
                    id = list.id, name = list.name, colorHex = list.colorHex, icon = list.icon,
                    position = list.position.toLong(), createdAt = list.createdAt
                )
            }
            payload.tags.forEach { tag ->
                db.tagQueries.insertOrReplace(id = tag.id, name = tag.name, colorHex = tag.colorHex)
            }
            payload.todoTasks.forEach { task ->
                db.todoTaskQueries.insertOrReplace(
                    id = task.id, listId = task.listId, title = task.title, notes = task.notes,
                    isDone = task.isDone, priority = task.priority, dueDate = task.dueDate, colorHex = task.colorHex,
                    position = task.position.toLong(), createdAt = task.createdAt, completedAt = task.completedAt,
                    isArchived = task.isArchived
                )
            }
            payload.taskTagCrossRefs.forEach { ref ->
                db.taskTagCrossRefQueries.insertOrReplace(ref.taskId, ref.tagId)
            }
            payload.simpleLists.forEach { list ->
                db.simpleListQueries.insertOrReplace(
                    id = list.id, name = list.name, icon = list.icon, colorHex = list.colorHex,
                    showCheckbox = list.showCheckbox, position = list.position.toLong(), createdAt = list.createdAt
                )
            }
            payload.simpleListItems.forEach { item ->
                db.simpleListItemQueries.insertOrReplace(
                    id = item.id, listId = item.listId, text = item.text, note = item.note,
                    isChecked = item.isChecked, position = item.position.toLong(), createdAt = item.createdAt
                )
            }
            payload.infoCategories.forEach { category ->
                db.infoCategoryQueries.insertOrReplace(
                    id = category.id, name = category.name, icon = category.icon, colorHex = category.colorHex,
                    position = category.position.toLong()
                )
            }
            payload.infoBlocks.forEach { block ->
                db.infoBlockQueries.insertOrReplace(
                    id = block.id, categoryId = block.categoryId, title = block.title, content = block.content,
                    isSensitive = block.isSensitive, isFavorite = block.isFavorite, position = block.position.toLong(),
                    createdAt = block.createdAt, updatedAt = block.updatedAt, expiryDate = block.expiryDate
                )
            }
        }
    }
}
