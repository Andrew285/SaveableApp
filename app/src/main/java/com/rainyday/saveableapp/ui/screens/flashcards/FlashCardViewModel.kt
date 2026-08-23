package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.data.repository.FlashCardsRepository
import com.rainyday.saveableapp.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FlashCardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FlashCardsRepository
) : ViewModel() {
    private val deckId: String = savedStateHandle.toRoute<Screen.FlashCardDeckDetail>().deckId
    val deck: StateFlow<FlashCardDeckEntity?> = repository.observeDeck(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _cards = MutableStateFlow<List<FlashCardEntity>>(emptyList())
    val cards: StateFlow<List<FlashCardEntity>> = _cards

    init {
        viewModelScope.launch {
            repository.observeCards(deckId).collect { _cards.value = it }
        }
    }

    fun createCard(front: String, back: String) {
        viewModelScope.launch { repository.createCard(deckId, front, back) }
    }

    fun updateCard(card: FlashCardEntity, front: String, back: String) {
        viewModelScope.launch { repository.updateCard(card.copy(front = front, back = back)) }
    }

    suspend fun deleteCardWithUndo(card: FlashCardEntity): FlashCardEntity {
        repository.deleteCard(card)
        return card
    }

    suspend fun restoreCard(card: FlashCardEntity) = repository.restoreCard(card)

    /** Optimistically reflects a drag reorder in the UI, then persists the new order. */
    fun moveCard(from: Int, to: Int) {
        val current = _cards.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val card = current.removeAt(from)
        current.add(to, card)
        _cards.value = current
        viewModelScope.launch { repository.reorderCards(current) }
    }
}
