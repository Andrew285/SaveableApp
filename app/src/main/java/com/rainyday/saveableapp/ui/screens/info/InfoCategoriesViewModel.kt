package com.rainyday.saveableapp.ui.screens.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.data.repository.InfoCategorySnapshot
import com.rainyday.saveableapp.data.repository.InfoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InfoCategoriesViewModel(private val repository: InfoRepository) : ViewModel() {
    val categories: StateFlow<List<InfoCategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
