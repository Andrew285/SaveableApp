package com.rainyday.saveableapp.data.local

import kotlinx.serialization.Serializable

@Serializable
data class SimpleListEntity(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val showCheckbox: Boolean = true,
    val position: Int = 0,
    val createdAt: Long
)

@Serializable
data class SimpleListItemEntity(
    val id: Long = 0,
    val listId: Long,
    val text: String,
    val note: String? = null,
    val isChecked: Boolean = false,
    val position: Int = 0,
    val createdAt: Long
)

data class ListProgress(val listId: Long, val total: Int, val checked: Int)
