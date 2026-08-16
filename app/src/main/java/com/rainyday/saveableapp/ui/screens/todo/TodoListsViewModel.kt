package com.rainyday.saveableapp.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.repository.TodoListSnapshot
import com.rainyday.saveableapp.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodoListsViewModel(private val repository: TodoRepository) : ViewModel() {
    // null while the first Room emission hasn't arrived yet, so the UI can tell "loading" apart from "empty".
    private val _lists = MutableStateFlow<List<TodoListEntity>?>(null)
    val lists: StateFlow<List<TodoListEntity>?> = _lists

    init {
        viewModelScope.launch {
            repository.observeLists().collect { _lists.value = it }
        }
    }

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
