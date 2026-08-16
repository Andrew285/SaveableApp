package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.ListProgress
import com.rainyday.saveableapp.data.local.SimpleListDao
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemDao
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class SimpleListSnapshot(val list: SimpleListEntity, val items: List<SimpleListItemEntity>)

class ListsRepository(
    private val listDao: SimpleListDao,
    private val itemDao: SimpleListItemDao
) {
    fun observeLists(): Flow<List<SimpleListEntity>> = listDao.observeLists()

    fun observeList(listId: Long): Flow<SimpleListEntity?> = listDao.observeById(listId)

    fun observeProgress(): Flow<List<ListProgress>> = itemDao.observeProgress()

    suspend fun createList(name: String, icon: String, colorHex: String, showCheckbox: Boolean): Long =
        listDao.insert(
            SimpleListEntity(
                name = name,
                icon = icon,
                colorHex = colorHex,
                showCheckbox = showCheckbox,
                createdAt = System.currentTimeMillis()
            )
        )

    suspend fun updateList(list: SimpleListEntity) = listDao.update(list)

    suspend fun deleteListWithSnapshot(list: SimpleListEntity): SimpleListSnapshot {
        val items = itemDao.observeItemsForList(list.id).first()
        listDao.delete(list)
        return SimpleListSnapshot(list, items)
    }

    suspend fun restoreList(snapshot: SimpleListSnapshot) {
        listDao.insert(snapshot.list)
        snapshot.items.forEach { itemDao.insert(it) }
    }

    fun observeItems(listId: Long): Flow<List<SimpleListItemEntity>> = itemDao.observeItemsForList(listId)

    fun searchItems(query: String): Flow<List<SimpleListItemEntity>> = itemDao.search(query)

    suspend fun createItem(listId: Long, text: String, note: String?): Long {
        val position = itemDao.observeItemsForList(listId).first().size
        return itemDao.insert(
            SimpleListItemEntity(
                listId = listId,
                text = text,
                note = note,
                position = position,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateItem(item: SimpleListItemEntity) = itemDao.update(item)

    suspend fun deleteItem(item: SimpleListItemEntity) = itemDao.delete(item)

    suspend fun restoreItem(item: SimpleListItemEntity) = itemDao.insert(item)

    suspend fun setItemChecked(item: SimpleListItemEntity, isChecked: Boolean) =
        itemDao.update(item.copy(isChecked = isChecked))

    suspend fun reorderItems(items: List<SimpleListItemEntity>) =
        itemDao.updateAll(items.mapIndexed { index, item -> item.copy(position = index) })
}
