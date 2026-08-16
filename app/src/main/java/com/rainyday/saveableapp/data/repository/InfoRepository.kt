package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.InfoBlockDao
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.data.local.InfoCategoryDao
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class InfoCategorySnapshot(val category: InfoCategoryEntity, val blocks: List<InfoBlockEntity>)

class InfoRepository(
    private val categoryDao: InfoCategoryDao,
    private val blockDao: InfoBlockDao
) {
    fun observeCategories(): Flow<List<InfoCategoryEntity>> = categoryDao.observeCategories()

    fun observeCategory(categoryId: Long): Flow<InfoCategoryEntity?> = categoryDao.observeById(categoryId)

    suspend fun createCategory(name: String, icon: String, colorHex: String): Long =
        categoryDao.insert(InfoCategoryEntity(name = name, icon = icon, colorHex = colorHex))

    suspend fun updateCategory(category: InfoCategoryEntity) = categoryDao.update(category)

    suspend fun deleteCategoryWithSnapshot(category: InfoCategoryEntity): InfoCategorySnapshot {
        val blocks = blockDao.observeBlocksForCategory(category.id).first()
        categoryDao.delete(category)
        return InfoCategorySnapshot(category, blocks)
    }

    suspend fun restoreCategory(snapshot: InfoCategorySnapshot) {
        categoryDao.insert(snapshot.category)
        snapshot.blocks.forEach { blockDao.insert(it) }
    }

    fun observeBlocks(categoryId: Long): Flow<List<InfoBlockEntity>> = blockDao.observeBlocksForCategory(categoryId)

    fun observeFavorites(): Flow<List<InfoBlockEntity>> = blockDao.observeFavorites()

    fun searchBlocks(query: String): Flow<List<InfoBlockEntity>> = blockDao.search(query)

    suspend fun createBlock(
        categoryId: Long,
        title: String,
        content: String,
        isSensitive: Boolean,
        expiryDate: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        return blockDao.insert(
            InfoBlockEntity(
                categoryId = categoryId,
                title = title,
                content = content,
                isSensitive = isSensitive,
                createdAt = now,
                updatedAt = now,
                expiryDate = expiryDate
            )
        )
    }

    suspend fun updateBlock(block: InfoBlockEntity) =
        blockDao.update(block.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteBlock(block: InfoBlockEntity) = blockDao.delete(block)

    suspend fun restoreBlock(block: InfoBlockEntity) = blockDao.insert(block)

    suspend fun setFavorite(block: InfoBlockEntity, isFavorite: Boolean) =
        blockDao.update(block.copy(isFavorite = isFavorite))
}
