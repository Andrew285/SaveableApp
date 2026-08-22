package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.repository.FlashCardDeckSnapshot
import com.rainyday.saveableapp.data.repository.FlashCardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FlashCardDeckUiModel(val deck: FlashCardDeckEntity, val cardCount: Int, val dueCount: Int)

class FlashCardDecksViewModel(private val repository: FlashCardsRepository) : ViewModel() {
    // null while the first Room emission hasn't arrived yet, so the UI can tell "loading" apart from "empty".
    private val _decks = MutableStateFlow<List<FlashCardDeckUiModel>?>(null)
    val decks: StateFlow<List<FlashCardDeckUiModel>?> = _decks

    init {
        viewModelScope.launch {
            combine(repository.observeDecks(), repository.observeCounts(), repository.observeDueCounts()) { decks, counts, dueCounts ->
                val countsByDeck = counts.associateBy { it.deckId }
                val dueByDeck = dueCounts.associateBy { it.deckId }
                decks.map { deck ->
                    FlashCardDeckUiModel(
                        deck,
                        cardCount = countsByDeck[deck.id]?.count ?: 0,
                        dueCount = dueByDeck[deck.id]?.count ?: 0
                    )
                }
            }.collect { _decks.value = it }
        }
    }

    fun createDeck(name: String, icon: String, colorHex: String) {
        viewModelScope.launch { repository.createDeck(name, icon, colorHex) }
    }

    fun updateDeck(deck: FlashCardDeckEntity, name: String, icon: String, colorHex: String) {
        viewModelScope.launch {
            repository.updateDeck(deck.copy(name = name, icon = icon, colorHex = colorHex))
        }
    }

    suspend fun deleteDeckWithUndo(deck: FlashCardDeckEntity): FlashCardDeckSnapshot =
        repository.deleteDeckWithSnapshot(deck)

    suspend fun restoreDeck(snapshot: FlashCardDeckSnapshot) = repository.restoreDeck(snapshot)
}
