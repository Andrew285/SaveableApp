package com.rainyday.saveableapp.ui.screens.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.SimpleListSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SimpleListUiModel(val list: SimpleListEntity, val checked: Int, val total: Int)

class SimpleListsViewModel(private val repository: ListsRepository) : ViewModel() {
    val lists: StateFlow<List<SimpleListUiModel>> =
        combine(repository.observeLists(), repository.observeProgress()) { lists, progress ->
            val progressByList = progress.associateBy { it.listId }
            lists.map { list ->
                val p = progressByList[list.id]
                SimpleListUiModel(list, checked = p?.checked ?: 0, total = p?.total ?: 0)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String, icon: String, colorHex: String, showCheckbox: Boolean) {
        viewModelScope.launch { repository.createList(name, icon, colorHex, showCheckbox) }
    }

    fun updateList(list: SimpleListEntity, name: String, icon: String, colorHex: String, showCheckbox: Boolean) {
        viewModelScope.launch {
            repository.updateList(
                list.copy(name = name, icon = icon, colorHex = colorHex, showCheckbox = showCheckbox)
            )
        }
    }

    suspend fun deleteListWithUndo(list: SimpleListEntity): SimpleListSnapshot =
        repository.deleteListWithSnapshot(list)

    suspend fun restoreList(snapshot: SimpleListSnapshot) = repository.restoreList(snapshot)
}
