package com.rainyday.saveableapp.ui.screens.info

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.data.repository.InfoRepository
import com.rainyday.saveableapp.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InfoBlockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: InfoRepository
) : ViewModel() {
    private val categoryId: String = savedStateHandle.toRoute<Screen.InfoCategoryDetail>().categoryId
    val category: StateFlow<InfoCategoryEntity?> = repository.observeCategory(categoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val blocks: StateFlow<List<InfoBlockEntity>> = repository.observeBlocks(categoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createBlock(title: String, content: String, isSensitive: Boolean, expiryDate: Long?) {
        viewModelScope.launch { repository.createBlock(categoryId, title, content, isSensitive, expiryDate) }
    }

    fun updateBlock(block: InfoBlockEntity, title: String, content: String, isSensitive: Boolean, expiryDate: Long?) {
        viewModelScope.launch {
            repository.updateBlock(
                block.copy(title = title, content = content, isSensitive = isSensitive, expiryDate = expiryDate)
            )
        }
    }

    suspend fun deleteBlockWithUndo(block: InfoBlockEntity): InfoBlockEntity {
        repository.deleteBlock(block)
        return block
    }

    suspend fun restoreBlock(block: InfoBlockEntity) = repository.restoreBlock(block)

    fun setFavorite(block: InfoBlockEntity, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(block, favorite) }
    }
}
