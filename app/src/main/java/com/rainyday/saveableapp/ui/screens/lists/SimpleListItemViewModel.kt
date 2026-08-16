package com.rainyday.saveableapp.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.data.repository.ListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SimpleListItemViewModel(
    private val listId: Long,
    private val repository: ListsRepository
) : ViewModel() {
    val list: StateFlow<SimpleListEntity?> = repository.observeList(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _items = MutableStateFlow<List<SimpleListItemEntity>>(emptyList())
    val items: StateFlow<List<SimpleListItemEntity>> = _items

    init {
        viewModelScope.launch {
            repository.observeItems(listId).collect { _items.value = it }
        }
    }

    fun createItem(text: String, note: String?) {
        viewModelScope.launch { repository.createItem(listId, text, note) }
    }

    fun updateItem(item: SimpleListItemEntity, text: String, note: String?) {
        viewModelScope.launch { repository.updateItem(item.copy(text = text, note = note)) }
    }

    suspend fun deleteItemWithUndo(item: SimpleListItemEntity): SimpleListItemEntity {
        repository.deleteItem(item)
        return item
    }

    suspend fun restoreItem(item: SimpleListItemEntity) = repository.restoreItem(item)

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
}
