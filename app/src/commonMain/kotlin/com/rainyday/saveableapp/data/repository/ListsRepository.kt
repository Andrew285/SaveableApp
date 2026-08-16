package com.rainyday.saveableapp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.rainyday.saveableapp.data.local.ListProgress
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.db.AppDatabase
import com.rainyday.saveableapp.db.Simple_list_items
import com.rainyday.saveableapp.db.Simple_lists
import com.rainyday.saveableapp.platform.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class SimpleListSnapshot(val list: SimpleListEntity, val items: List<SimpleListItemEntity>)

internal fun Simple_lists.toEntity() = SimpleListEntity(
    id = id, name = name, icon = icon, colorHex = colorHex, showCheckbox = showCheckbox,
    position = position.toInt(), createdAt = createdAt
)

internal fun Simple_list_items.toEntity() = SimpleListItemEntity(
    id = id, listId = listId, text = text, note = note, isChecked = isChecked,
    position = position.toInt(), createdAt = createdAt
)

class ListsRepository(private val db: AppDatabase) {
    private val listQueries = db.simpleListQueries
    private val itemQueries = db.simpleListItemQueries

    fun observeLists(): Flow<List<SimpleListEntity>> =
        listQueries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    fun observeList(listId: Long): Flow<SimpleListEntity?> =
        listQueries.selectById(listId).asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.toEntity() }

    fun observeProgress(): Flow<List<ListProgress>> =
        itemQueries.selectProgress().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { ListProgress(it.listId, it.total.toInt(), (it.checked ?: 0).toInt()) }
        }

    suspend fun createList(name: String, icon: String, colorHex: String, showCheckbox: Boolean): Long {
        listQueries.insertNew(
            name = name, icon = icon, colorHex = colorHex, showCheckbox = showCheckbox,
            position = 0, createdAt = nowMillis()
        )
        return listQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateList(list: SimpleListEntity) {
        listQueries.update(
            name = list.name, icon = list.icon, colorHex = list.colorHex, showCheckbox = list.showCheckbox,
            position = list.position.toLong(), createdAt = list.createdAt, id = list.id
        )
    }

    suspend fun deleteListWithSnapshot(list: SimpleListEntity): SimpleListSnapshot {
        val items = observeItems(list.id).first()
        listQueries.deleteById(list.id)
        return SimpleListSnapshot(list, items)
    }

    suspend fun restoreList(snapshot: SimpleListSnapshot) {
        listQueries.insertOrReplace(
            id = snapshot.list.id, name = snapshot.list.name, icon = snapshot.list.icon,
            colorHex = snapshot.list.colorHex, showCheckbox = snapshot.list.showCheckbox,
            position = snapshot.list.position.toLong(), createdAt = snapshot.list.createdAt
        )
        snapshot.items.forEach { insertItemOrReplace(it) }
    }

    fun observeItems(listId: Long): Flow<List<SimpleListItemEntity>> =
        itemQueries.selectForList(listId).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    fun searchItems(query: String): Flow<List<SimpleListItemEntity>> =
        itemQueries.search(query).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    suspend fun createItem(listId: Long, text: String, note: String?): Long {
        val position = observeItems(listId).first().size
        itemQueries.insertNew(
            listId = listId, text = text, note = note, isChecked = false,
            position = position.toLong(), createdAt = nowMillis()
        )
        return itemQueries.lastInsertRowId().executeAsOne()
    }

    private fun insertItemOrReplace(item: SimpleListItemEntity) {
        itemQueries.insertOrReplace(
            id = item.id, listId = item.listId, text = item.text, note = item.note, isChecked = item.isChecked,
            position = item.position.toLong(), createdAt = item.createdAt
        )
    }

    private fun updateItemRow(item: SimpleListItemEntity) {
        itemQueries.update(
            listId = item.listId, text = item.text, note = item.note, isChecked = item.isChecked,
            position = item.position.toLong(), createdAt = item.createdAt, id = item.id
        )
    }

    suspend fun updateItem(item: SimpleListItemEntity) = updateItemRow(item)

    suspend fun deleteItem(item: SimpleListItemEntity) = itemQueries.deleteById(item.id)

    suspend fun restoreItem(item: SimpleListItemEntity) = insertItemOrReplace(item)

    suspend fun setItemChecked(item: SimpleListItemEntity, isChecked: Boolean) =
        updateItemRow(item.copy(isChecked = isChecked))

    suspend fun reorderItems(items: List<SimpleListItemEntity>) {
        itemQueries.transaction {
            items.mapIndexed { index, item -> item.copy(position = index) }.forEach { updateItemRow(it) }
        }
    }
}
