package com.rainyday.saveableapp.data.local

import kotlinx.serialization.Serializable

@Serializable
data class InfoCategoryEntity(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val position: Int = 0
)

@Serializable
data class InfoBlockEntity(
    val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val content: String,
    val isSensitive: Boolean = false,
    val isFavorite: Boolean = false,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val expiryDate: Long? = null
)
