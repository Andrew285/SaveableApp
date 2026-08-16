package com.rainyday.saveableapp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.db.AppDatabase
import com.rainyday.saveableapp.db.Info_blocks
import com.rainyday.saveableapp.db.Info_categories
import com.rainyday.saveableapp.platform.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class InfoCategorySnapshot(val category: InfoCategoryEntity, val blocks: List<InfoBlockEntity>)

internal fun Info_categories.toEntity() = InfoCategoryEntity(
    id = id, name = name, icon = icon, colorHex = colorHex, position = position.toInt()
)

internal fun Info_blocks.toEntity() = InfoBlockEntity(
    id = id, categoryId = categoryId, title = title, content = content, isSensitive = isSensitive,
    isFavorite = isFavorite, position = position.toInt(), createdAt = createdAt, updatedAt = updatedAt,
    expiryDate = expiryDate
)

class InfoRepository(private val db: AppDatabase) {
    private val categoryQueries = db.infoCategoryQueries
    private val blockQueries = db.infoBlockQueries

    fun observeCategories(): Flow<List<InfoCategoryEntity>> =
        categoryQueries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    fun observeCategory(categoryId: Long): Flow<InfoCategoryEntity?> =
        categoryQueries.selectById(categoryId).asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.toEntity() }

    suspend fun createCategory(name: String, icon: String, colorHex: String): Long {
        categoryQueries.insertNew(name = name, icon = icon, colorHex = colorHex, position = 0)
        return categoryQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateCategory(category: InfoCategoryEntity) {
        categoryQueries.update(
            name = category.name, icon = category.icon, colorHex = category.colorHex,
            position = category.position.toLong(), id = category.id
        )
    }

    suspend fun deleteCategoryWithSnapshot(category: InfoCategoryEntity): InfoCategorySnapshot {
        val blocks = observeBlocks(category.id).first()
        categoryQueries.deleteById(category.id)
        return InfoCategorySnapshot(category, blocks)
    }

    suspend fun restoreCategory(snapshot: InfoCategorySnapshot) {
        categoryQueries.insertOrReplace(
            id = snapshot.category.id, name = snapshot.category.name, icon = snapshot.category.icon,
            colorHex = snapshot.category.colorHex, position = snapshot.category.position.toLong()
        )
        snapshot.blocks.forEach { insertBlockOrReplace(it) }
    }

    fun observeBlocks(categoryId: Long): Flow<List<InfoBlockEntity>> =
        blockQueries.selectForCategory(categoryId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toEntity() } }

    fun observeFavorites(): Flow<List<InfoBlockEntity>> =
        blockQueries.selectFavorites().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    fun searchBlocks(query: String): Flow<List<InfoBlockEntity>> =
        blockQueries.search(query).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    suspend fun createBlock(
        categoryId: Long,
        title: String,
        content: String,
        isSensitive: Boolean,
        expiryDate: Long? = null
    ): Long {
        val now = nowMillis()
        blockQueries.insertNew(
            categoryId = categoryId, title = title, content = content, isSensitive = isSensitive,
            isFavorite = false, position = 0, createdAt = now, updatedAt = now, expiryDate = expiryDate
        )
        return blockQueries.lastInsertRowId().executeAsOne()
    }

    private fun insertBlockOrReplace(block: InfoBlockEntity) {
        blockQueries.insertOrReplace(
            id = block.id, categoryId = block.categoryId, title = block.title, content = block.content,
            isSensitive = block.isSensitive, isFavorite = block.isFavorite, position = block.position.toLong(),
            createdAt = block.createdAt, updatedAt = block.updatedAt, expiryDate = block.expiryDate
        )
    }

    private fun updateBlockRow(block: InfoBlockEntity) {
        blockQueries.update(
            categoryId = block.categoryId, title = block.title, content = block.content,
            isSensitive = block.isSensitive, isFavorite = block.isFavorite, position = block.position.toLong(),
            createdAt = block.createdAt, updatedAt = block.updatedAt, expiryDate = block.expiryDate, id = block.id
        )
    }

    suspend fun updateBlock(block: InfoBlockEntity) = updateBlockRow(block.copy(updatedAt = nowMillis()))

    suspend fun deleteBlock(block: InfoBlockEntity) = blockQueries.deleteById(block.id)

    suspend fun restoreBlock(block: InfoBlockEntity) = insertBlockOrReplace(block)

    suspend fun setFavorite(block: InfoBlockEntity, isFavorite: Boolean) = updateBlockRow(block.copy(isFavorite = isFavorite))
}
