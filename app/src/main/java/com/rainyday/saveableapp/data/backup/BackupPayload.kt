package com.rainyday.saveableapp.data.backup

import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldValueEntity
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskTagCrossRef
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val todoLists: List<TodoListEntity>,
    val todoTasks: List<TodoTaskEntity>,
    val tags: List<TagEntity>,
    val taskTagCrossRefs: List<TaskTagCrossRef>,
    val simpleLists: List<SimpleListEntity>,
    val simpleListItems: List<SimpleListItemEntity>,
    val infoCategories: List<InfoCategoryEntity>,
    val infoBlocks: List<InfoBlockEntity>,
    val flashCardDecks: List<FlashCardDeckEntity> = emptyList(),
    val flashCards: List<FlashCardEntity> = emptyList(),
    val fieldDefinitions: List<FieldDefinitionEntity> = emptyList(),
    val fieldValues: List<FieldValueEntity> = emptyList()
)
