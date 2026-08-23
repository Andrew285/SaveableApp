package com.rainyday.saveableapp.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.FieldValueEntity
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.SimpleListItemSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SimpleListItemViewModel(
    private val listId: Long,
    private val repository: ListsRepository
) : ViewModel() {
    val list: StateFlow<SimpleListEntity?> = repository.observeList(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fields: StateFlow<List<FieldDefinitionEntity>> = repository.observeFields(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _items = MutableStateFlow<List<SimpleListItemEntity>>(emptyList())
    val items: StateFlow<List<SimpleListItemEntity>> = _items

    val fieldValuesByItem: StateFlow<Map<Long, List<FieldValueEntity>>> = repository.observeFieldValues(listId)
        .map { values -> values.groupBy { it.itemId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            repository.observeItems(listId).collect { _items.value = it }
        }
    }

    fun createItem(text: String, note: String?, url: String?, fieldValues: Map<Long, String>) {
        viewModelScope.launch {
            val itemId = repository.createItem(listId, text, note, url)
            if (fieldValues.isNotEmpty()) repository.setItemFieldValues(itemId, fieldValues)
        }
    }

    fun updateItem(item: SimpleListItemEntity, text: String, note: String?, url: String?, fieldValues: Map<Long, String>) {
        viewModelScope.launch {
            repository.updateItem(item.copy(text = text, note = note, url = url))
            repository.setItemFieldValues(item.id, fieldValues)
        }
    }

    suspend fun deleteItemWithUndo(item: SimpleListItemEntity): SimpleListItemSnapshot =
        repository.deleteItemWithSnapshot(item)

    suspend fun restoreItem(snapshot: SimpleListItemSnapshot) = repository.restoreItemSnapshot(snapshot)

    suspend fun bulkDeleteWithUndo(items: List<SimpleListItemEntity>): List<SimpleListItemSnapshot> =
        items.map { repository.deleteItemWithSnapshot(it) }

    suspend fun restoreItems(snapshot: List<SimpleListItemSnapshot>) {
        snapshot.forEach { repository.restoreItemSnapshot(it) }
    }

    fun bulkSetChecked(items: List<SimpleListItemEntity>, checked: Boolean) {
        viewModelScope.launch { items.forEach { repository.setItemChecked(it, checked) } }
    }

    fun setChecked(item: SimpleListItemEntity, checked: Boolean) {
        viewModelScope.launch { repository.setItemChecked(item, checked) }
    }

    /** Optimistically reflects a drag reorder in the UI, then persists the new order. */
    fun moveItem(from: Int, to: Int) {
        val current = _items.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _items.value = current
        viewModelScope.launch { repository.reorderItems(current) }
    }

    fun addField(name: String, type: FieldType, colorHex: String) {
        viewModelScope.launch { repository.createField(listId, name, type, colorHex) }
    }

    fun updateField(field: FieldDefinitionEntity, name: String, type: FieldType, colorHex: String) {
        viewModelScope.launch {
            repository.updateField(field.copy(name = name, type = type, colorHex = colorHex))
        }
    }

    fun deleteField(field: FieldDefinitionEntity) {
        viewModelScope.launch { repository.deleteField(field) }
    }
}
