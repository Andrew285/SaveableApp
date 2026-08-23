package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.InfoBlockDao
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.data.local.InfoCategoryDao
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.sync.SyncOutbox
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class InfoCategorySnapshot(val category: InfoCategoryEntity, val blocks: List<InfoBlockEntity>)

class InfoRepository(
    private val categoryDao: InfoCategoryDao,
    private val blockDao: InfoBlockDao,
    private val syncOutbox: SyncOutbox
) {
    fun observeCategories(): Flow<List<InfoCategoryEntity>> = categoryDao.observeCategories()

    fun observeCategoryCounts(): Flow<List<com.rainyday.saveableapp.data.local.InfoCategoryCount>> = blockDao.observeCounts()

    fun observeCategory(categoryId: String): Flow<InfoCategoryEntity?> = categoryDao.observeById(categoryId)

    suspend fun createCategory(name: String, icon: String, colorHex: String): String {
        val id = UUID.randomUUID().toString()
        categoryDao.insert(
            InfoCategoryEntity(id = id, name = name, icon = icon, colorHex = colorHex, updatedAt = System.currentTimeMillis())
        )
        syncOutbox.upsert(SyncEntityType.INFO_CATEGORY, id)
        return id
    }

    suspend fun updateCategory(category: InfoCategoryEntity) {
        categoryDao.update(category.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.INFO_CATEGORY, category.id)
    }

    /** Room cascades the local delete of [category]'s blocks; enqueue their ids too or they orphan in Firestore. */
    suspend fun deleteCategoryWithSnapshot(category: InfoCategoryEntity): InfoCategorySnapshot {
        val blocks = blockDao.observeBlocksForCategory(category.id).first()
        categoryDao.delete(category)
        syncOutbox.deleteAll(SyncEntityType.INFO_BLOCK, blocks.map { it.id })
        syncOutbox.delete(SyncEntityType.INFO_CATEGORY, category.id)
        return InfoCategorySnapshot(category, blocks)
    }

    suspend fun restoreCategory(snapshot: InfoCategorySnapshot) {
        categoryDao.insert(snapshot.category)
        snapshot.blocks.forEach { blockDao.insert(it) }
        syncOutbox.upsert(SyncEntityType.INFO_CATEGORY, snapshot.category.id)
        syncOutbox.upsertAll(SyncEntityType.INFO_BLOCK, snapshot.blocks.map { it.id })
    }

    fun observeBlocks(categoryId: String): Flow<List<InfoBlockEntity>> = blockDao.observeBlocksForCategory(categoryId)

    fun observeFavorites(): Flow<List<InfoBlockEntity>> = blockDao.observeFavorites()

    fun searchBlocks(query: String): Flow<List<InfoBlockEntity>> = blockDao.search(query)

    suspend fun createBlock(
        categoryId: String,
        title: String,
        content: String,
        isSensitive: Boolean,
        expiryDate: Long? = null
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        blockDao.insert(
            InfoBlockEntity(
                id = id,
                categoryId = categoryId,
                title = title,
                content = content,
                isSensitive = isSensitive,
                createdAt = now,
                updatedAt = now,
                expiryDate = expiryDate
            )
        )
        syncOutbox.upsert(SyncEntityType.INFO_BLOCK, id)
        return id
    }

    suspend fun updateBlock(block: InfoBlockEntity) {
        blockDao.update(block.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.INFO_BLOCK, block.id)
    }

    suspend fun deleteBlock(block: InfoBlockEntity) {
        blockDao.delete(block)
        syncOutbox.delete(SyncEntityType.INFO_BLOCK, block.id)
    }

    suspend fun restoreBlock(block: InfoBlockEntity) {
        blockDao.insert(block)
        syncOutbox.upsert(SyncEntityType.INFO_BLOCK, block.id)
    }

    suspend fun setFavorite(block: InfoBlockEntity, isFavorite: Boolean) {
        blockDao.update(block.copy(isFavorite = isFavorite, updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.INFO_BLOCK, block.id)
    }
}
