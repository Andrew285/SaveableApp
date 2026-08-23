package com.rainyday.saveableapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchResults(
    val tasks: List<TaskWithTags> = emptyList(),
    val items: List<SimpleListItemEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val listsRepository: ListsRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val results: StateFlow<SearchResults> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(SearchResults())
            } else {
                combine(
                    todoRepository.searchTasks(q),
                    listsRepository.searchItems(q)
                ) { tasks, items -> SearchResults(tasks, items) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    fun setQuery(value: String) {
        _query.value = value
    }
}
