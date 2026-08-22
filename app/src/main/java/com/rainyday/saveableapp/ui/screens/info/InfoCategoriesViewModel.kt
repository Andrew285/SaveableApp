package com.rainyday.saveableapp.ui.screens.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.data.repository.InfoCategorySnapshot
import com.rainyday.saveableapp.data.repository.InfoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class InfoCategoryUiModel(val category: InfoCategoryEntity, val itemCount: Int)

class InfoCategoriesViewModel(private val repository: InfoRepository) : ViewModel() {
    // null while the first Room emission hasn't arrived yet, so the UI can tell "loading" apart from "empty".
    private val _categories = MutableStateFlow<List<InfoCategoryUiModel>?>(null)
    val categories: StateFlow<List<InfoCategoryUiModel>?> = _categories

    init {
        viewModelScope.launch {
            combine(repository.observeCategories(), repository.observeCategoryCounts()) { categories, counts ->
                val countsById = counts.associateBy { it.categoryId }
                categories.map { category ->
                    InfoCategoryUiModel(category, countsById[category.id]?.total ?: 0)
                }
            }.collect { _categories.value = it }
        }
    }

    fun createCategory(name: String, icon: String, colorHex: String) {
        viewModelScope.launch { repository.createCategory(name, icon, colorHex) }
    }

    fun updateCategory(category: InfoCategoryEntity, name: String, icon: String, colorHex: String) {
        viewModelScope.launch { repository.updateCategory(category.copy(name = name, icon = icon, colorHex = colorHex)) }
    }

    suspend fun deleteCategoryWithUndo(category: InfoCategoryEntity): InfoCategorySnapshot =
        repository.deleteCategoryWithSnapshot(category)

    suspend fun restoreCategory(snapshot: InfoCategorySnapshot) = repository.restoreCategory(snapshot)
}
