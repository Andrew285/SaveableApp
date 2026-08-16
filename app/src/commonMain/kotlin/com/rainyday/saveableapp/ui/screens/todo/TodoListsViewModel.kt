package com.rainyday.saveableapp.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.repository.TodoListSnapshot
import com.rainyday.saveableapp.data.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoListsViewModel(private val repository: TodoRepository) : ViewModel() {
    val lists: StateFlow<List<TodoListEntity>> = repository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String, icon: String, colorHex: String) {
        viewModelScope.launch { repository.createList(name, colorHex, icon) }
    }

    fun updateList(list: TodoListEntity, name: String, icon: String, colorHex: String) {
        viewModelScope.launch { repository.updateList(list.copy(name = name, icon = icon, colorHex = colorHex)) }
    }

    suspend fun deleteListWithUndo(list: TodoListEntity): TodoListSnapshot =
        repository.deleteListWithSnapshot(list)

    suspend fun restoreList(snapshot: TodoListSnapshot) = repository.restoreList(snapshot)
}
