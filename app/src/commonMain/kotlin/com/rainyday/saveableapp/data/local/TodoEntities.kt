package com.rainyday.saveableapp.data.local

import kotlinx.serialization.Serializable

@Serializable
data class TodoListEntity(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val icon: String,
    val position: Int = 0,
    val createdAt: Long
)

@Serializable
data class TodoTaskEntity(
    val id: Long = 0,
    val listId: Long,
    val title: String,
    val notes: String? = null,
    val isDone: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null,
    val colorHex: String? = null,
    val position: Int = 0,
    val createdAt: Long,
    val completedAt: Long? = null,
    val isArchived: Boolean = false
)

@Serializable
data class TagEntity(
    val id: Long = 0,
    val name: String,
    val colorHex: String
)

@Serializable
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)

data class TaskWithTags(
    val task: TodoTaskEntity,
    val tags: List<TagEntity>
)
