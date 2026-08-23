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
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.sync.SyncOutbox
import java.util.UUID
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
    private val fieldValueDao: FieldValueDao,
    private val syncOutbox: SyncOutbox
) {
    fun observeLists(): Flow<List<SimpleListEntity>> = listDao.observeLists()

    fun observeList(listId: String): Flow<SimpleListEntity?> = listDao.observeById(listId)

    fun observeProgress(): Flow<List<ListProgress>> = itemDao.observeProgress()

    suspend fun createList(
        name: String,
        icon: String,
        colorHex: String,
        showCheckbox: Boolean,
        fieldTemplates: List<FieldTemplate> = emptyList()
    ): String {
        val now = System.currentTimeMillis()
        val listId = UUID.randomUUID().toString()
        listDao.insert(
            SimpleListEntity(
                id = listId,
                name = name,
                icon = icon,
                colorHex = colorHex,
                showCheckbox = showCheckbox,
                createdAt = now,
                updatedAt = now
            )
        )
        syncOutbox.upsert(SyncEntityType.SIMPLE_LIST, listId)
        fieldTemplates.forEachIndexed { index, template ->
            val fieldId = UUID.randomUUID().toString()
            fieldDefinitionDao.insert(
                FieldDefinitionEntity(
                    id = fieldId,
                    listId = listId,
                    name = template.name,
                    type = template.type,
                    colorHex = template.colorHex,
                    position = index,
                    createdAt = now,
                    updatedAt = now
                )
            )
            syncOutbox.upsert(SyncEntityType.FIELD_DEFINITION, fieldId)
        }
        return listId
    }

    suspend fun updateList(list: SimpleListEntity) {
        listDao.update(list.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.SIMPLE_LIST, list.id)
    }

    /**
     * Deletes [list] and everything under it. Room's `ON DELETE CASCADE` handles the local removal
     * of items/fields/values, but that cascade is invisible to the outbox — so every cascaded child
     * id is explicitly enqueued for deletion here too, or they'd become orphaned docs in Firestore.
     */
    suspend fun deleteListWithSnapshot(list: SimpleListEntity): SimpleListSnapshot {
        val items = itemDao.observeItemsForList(list.id).first()
        val fields = fieldDefinitionDao.getFieldsForList(list.id)
        val fieldValues = fieldValueDao.observeValuesForList(list.id).first()
        listDao.delete(list)
        syncOutbox.deleteAll(SyncEntityType.FIELD_VALUE, fieldValues.map { it.id })
        syncOutbox.deleteAll(SyncEntityType.FIELD_DEFINITION, fields.map { it.id })
        syncOutbox.deleteAll(SyncEntityType.SIMPLE_LIST_ITEM, items.map { it.id })
        syncOutbox.delete(SyncEntityType.SIMPLE_LIST, list.id)
        return SimpleListSnapshot(list, items, fields, fieldValues)
    }

    suspend fun restoreList(snapshot: SimpleListSnapshot) {
        listDao.insert(snapshot.list)
        snapshot.items.forEach { itemDao.insert(it) }
        snapshot.fields.forEach { fieldDefinitionDao.insert(it) }
        if (snapshot.fieldValues.isNotEmpty()) fieldValueDao.insertAll(snapshot.fieldValues)
        syncOutbox.upsert(SyncEntityType.SIMPLE_LIST, snapshot.list.id)
        syncOutbox.upsertAll(SyncEntityType.SIMPLE_LIST_ITEM, snapshot.items.map { it.id })
        syncOutbox.upsertAll(SyncEntityType.FIELD_DEFINITION, snapshot.fields.map { it.id })
        syncOutbox.upsertAll(SyncEntityType.FIELD_VALUE, snapshot.fieldValues.map { it.id })
    }

    fun observeItems(listId: String): Flow<List<SimpleListItemEntity>> = itemDao.observeItemsForList(listId)

    fun searchItems(query: String): Flow<List<SimpleListItemEntity>> = itemDao.search(query)

    suspend fun createItem(listId: String, text: String, note: String?, url: String? = null): String {
        val position = itemDao.observeItemsForList(listId).first().size
        val now = System.currentTimeMillis()
        val itemId = UUID.randomUUID().toString()
        itemDao.insert(
            SimpleListItemEntity(
                id = itemId,
                listId = listId,
                text = text,
                note = note,
                url = url,
                position = position,
                createdAt = now,
                updatedAt = now
            )
        )
        syncOutbox.upsert(SyncEntityType.SIMPLE_LIST_ITEM, itemId)
        return itemId
    }

    suspend fun updateItem(item: SimpleListItemEntity) {
        itemDao.update(item.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.SIMPLE_LIST_ITEM, item.id)
    }

    suspend fun deleteItemWithSnapshot(item: SimpleListItemEntity): SimpleListItemSnapshot {
        val values = fieldValueDao.getValuesForItem(item.id)
        itemDao.delete(item)
        syncOutbox.deleteAll(SyncEntityType.FIELD_VALUE, values.map { it.id })
        syncOutbox.delete(SyncEntityType.SIMPLE_LIST_ITEM, item.id)
        return SimpleListItemSnapshot(item, values)
    }

    suspend fun restoreItemSnapshot(snapshot: SimpleListItemSnapshot) {
        itemDao.insert(snapshot.item)
        if (snapshot.fieldValues.isNotEmpty()) fieldValueDao.insertAll(snapshot.fieldValues)
        syncOutbox.upsert(SyncEntityType.SIMPLE_LIST_ITEM, snapshot.item.id)
        syncOutbox.upsertAll(SyncEntityType.FIELD_VALUE, snapshot.fieldValues.map { it.id })
    }

    suspend fun setItemChecked(item: SimpleListItemEntity, isChecked: Boolean) {
        itemDao.update(item.copy(isChecked = isChecked, updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.SIMPLE_LIST_ITEM, item.id)
    }

    suspend fun reorderItems(items: List<SimpleListItemEntity>) {
        val now = System.currentTimeMillis()
        itemDao.updateAll(items.mapIndexed { index, item -> item.copy(position = index, updatedAt = now) })
        syncOutbox.upsertAll(SyncEntityType.SIMPLE_LIST_ITEM, items.map { it.id })
    }

    fun observeFields(listId: String): Flow<List<FieldDefinitionEntity>> = fieldDefinitionDao.observeFieldsForList(listId)

    fun observeAllFields(): Flow<List<FieldDefinitionEntity>> = fieldDefinitionDao.observeAllFields()

    fun observeFieldValues(listId: String): Flow<List<FieldValueEntity>> = fieldValueDao.observeValuesForList(listId)

    suspend fun createField(listId: String, name: String, type: FieldType, colorHex: String): String {
        val position = fieldDefinitionDao.getFieldsForList(listId).size
        val now = System.currentTimeMillis()
        val fieldId = UUID.randomUUID().toString()
        fieldDefinitionDao.insert(
            FieldDefinitionEntity(
                id = fieldId,
                listId = listId,
                name = name,
                type = type,
                colorHex = colorHex,
                position = position,
                createdAt = now,
                updatedAt = now
            )
        )
        syncOutbox.upsert(SyncEntityType.FIELD_DEFINITION, fieldId)
        return fieldId
    }

    suspend fun updateField(field: FieldDefinitionEntity) {
        fieldDefinitionDao.update(field.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.FIELD_DEFINITION, field.id)
    }

    /** Deleting a field cascades (locally) to every item's stored value for it — enqueue those too. */
    suspend fun deleteField(field: FieldDefinitionEntity) {
        val values = fieldValueDao.getValuesForField(field.id)
        fieldDefinitionDao.delete(field)
        syncOutbox.deleteAll(SyncEntityType.FIELD_VALUE, values.map { it.id })
        syncOutbox.delete(SyncEntityType.FIELD_DEFINITION, field.id)
    }

    /** Replaces all of an item's field values with [values] (fieldId to raw value); blank values are dropped. */
    suspend fun setItemFieldValues(itemId: String, values: Map<String, String>) {
        val oldValues = fieldValueDao.getValuesForItem(itemId)
        fieldValueDao.deleteForItem(itemId)
        syncOutbox.deleteAll(SyncEntityType.FIELD_VALUE, oldValues.map { it.id })
        val now = System.currentTimeMillis()
        val entities = values.filterValues { it.isNotBlank() }.map { (fieldId, value) ->
            FieldValueEntity(id = UUID.randomUUID().toString(), itemId = itemId, fieldId = fieldId, value = value, updatedAt = now)
        }
        if (entities.isNotEmpty()) {
            fieldValueDao.insertAll(entities)
            syncOutbox.upsertAll(SyncEntityType.FIELD_VALUE, entities.map { it.id })
        }
    }
}
