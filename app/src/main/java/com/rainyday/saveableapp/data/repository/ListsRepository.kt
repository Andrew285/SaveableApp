package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.FieldDefinitionDao
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldTemplate
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.FieldValueDao
import com.rainyday.saveableapp.data.local.FieldValueEntity
import com.rainyday.saveableapp.data.local.ListProgress
import com.rainyday.saveableapp.data.local.SimpleListDao
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemDao
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class SimpleListSnapshot(
    val list: SimpleListEntity,
    val items: List<SimpleListItemEntity>,
    val fields: List<FieldDefinitionEntity> = emptyList(),
    val fieldValues: List<FieldValueEntity> = emptyList()
)

data class SimpleListItemSnapshot(val item: SimpleListItemEntity, val fieldValues: List<FieldValueEntity>)

class ListsRepository(
    private val listDao: SimpleListDao,
    private val itemDao: SimpleListItemDao,
    private val fieldDefinitionDao: FieldDefinitionDao,
    private val fieldValueDao: FieldValueDao
) {
    fun observeLists(): Flow<List<SimpleListEntity>> = listDao.observeLists()

    fun observeList(listId: Long): Flow<SimpleListEntity?> = listDao.observeById(listId)

    fun observeProgress(): Flow<List<ListProgress>> = itemDao.observeProgress()

    suspend fun createList(
        name: String,
        icon: String,
        colorHex: String,
        showCheckbox: Boolean,
        fieldTemplates: List<FieldTemplate> = emptyList()
    ): Long {
        val listId = listDao.insert(
            SimpleListEntity(
                name = name,
                icon = icon,
                colorHex = colorHex,
                showCheckbox = showCheckbox,
                createdAt = System.currentTimeMillis()
            )
        )
        fieldTemplates.forEachIndexed { index, template ->
            fieldDefinitionDao.insert(
                FieldDefinitionEntity(
                    listId = listId,
                    name = template.name,
                    type = template.type,
                    colorHex = template.colorHex,
                    position = index,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return listId
    }

    suspend fun updateList(list: SimpleListEntity) = listDao.update(list)

    suspend fun deleteListWithSnapshot(list: SimpleListEntity): SimpleListSnapshot {
        val items = itemDao.observeItemsForList(list.id).first()
        val fields = fieldDefinitionDao.getFieldsForList(list.id)
        val fieldValues = fieldValueDao.observeValuesForList(list.id).first()
        listDao.delete(list)
        return SimpleListSnapshot(list, items, fields, fieldValues)
    }

    suspend fun restoreList(snapshot: SimpleListSnapshot) {
        listDao.insert(snapshot.list)
        snapshot.items.forEach { itemDao.insert(it) }
        snapshot.fields.forEach { fieldDefinitionDao.insert(it) }
        if (snapshot.fieldValues.isNotEmpty()) fieldValueDao.insertAll(snapshot.fieldValues)
    }

    fun observeItems(listId: Long): Flow<List<SimpleListItemEntity>> = itemDao.observeItemsForList(listId)

    fun searchItems(query: String): Flow<List<SimpleListItemEntity>> = itemDao.search(query)

    suspend fun createItem(listId: Long, text: String, note: String?, url: String? = null): Long {
        val position = itemDao.observeItemsForList(listId).first().size
        return itemDao.insert(
            SimpleListItemEntity(
                listId = listId,
                text = text,
                note = note,
                url = url,
                position = position,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateItem(item: SimpleListItemEntity) = itemDao.update(item)

    suspend fun deleteItemWithSnapshot(item: SimpleListItemEntity): SimpleListItemSnapshot {
        val values = fieldValueDao.getValuesForItem(item.id)
        itemDao.delete(item)
        return SimpleListItemSnapshot(item, values)
    }

    suspend fun restoreItemSnapshot(snapshot: SimpleListItemSnapshot) {
        itemDao.insert(snapshot.item)
        if (snapshot.fieldValues.isNotEmpty()) fieldValueDao.insertAll(snapshot.fieldValues)
    }

    suspend fun setItemChecked(item: SimpleListItemEntity, isChecked: Boolean) =
        itemDao.update(item.copy(isChecked = isChecked))

    suspend fun reorderItems(items: List<SimpleListItemEntity>) =
        itemDao.updateAll(items.mapIndexed { index, item -> item.copy(position = index) })

    fun observeFields(listId: Long): Flow<List<FieldDefinitionEntity>> = fieldDefinitionDao.observeFieldsForList(listId)

    fun observeFieldValues(listId: Long): Flow<List<FieldValueEntity>> = fieldValueDao.observeValuesForList(listId)

    suspend fun createField(listId: Long, name: String, type: FieldType, colorHex: String): Long {
        val position = fieldDefinitionDao.getFieldsForList(listId).size
        return fieldDefinitionDao.insert(
            FieldDefinitionEntity(
                listId = listId,
                name = name,
                type = type,
                colorHex = colorHex,
                position = position,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateField(field: FieldDefinitionEntity) = fieldDefinitionDao.update(field)

    suspend fun deleteField(field: FieldDefinitionEntity) = fieldDefinitionDao.delete(field)

    /** Replaces all of an item's field values with [values] (fieldId to raw value); blank values are dropped. */
    suspend fun setItemFieldValues(itemId: Long, values: Map<Long, String>) {
        fieldValueDao.deleteForItem(itemId)
        val entities = values.filterValues { it.isNotBlank() }.map { (fieldId, value) ->
            FieldValueEntity(itemId = itemId, fieldId = fieldId, value = value)
        }
        if (entities.isNotEmpty()) fieldValueDao.insertAll(entities)
    }
}
