package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.data.repository.CardRating
import com.rainyday.saveableapp.data.repository.FlashCardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlashCardStudyViewModel(
    private val deckId: Long,
    private val repository: FlashCardsRepository
) : ViewModel() {
    val deck: StateFlow<FlashCardDeckEntity?> = repository.observeDeck(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // The study queue is a one-shot snapshot fetched when the session starts (or restarts), not a
    // live query — otherwise a card would vanish mid-session the instant it's rated and its due
    // date moves into the future.
    private val _queue = MutableStateFlow<List<FlashCardEntity>?>(null)
    val queue: StateFlow<List<FlashCardEntity>?> = _queue

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index

    init {
        loadDueCards()
    }

    private fun loadDueCards() {
        viewModelScope.launch {
            _index.value = 0
            _queue.value = repository.getDueCards(deckId)
        }
    }

    fun studyAllCards() {
        viewModelScope.launch {
            _index.value = 0
            _queue.value = repository.getAllCardsForStudy(deckId)
        }
    }

    fun rate(card: FlashCardEntity, rating: CardRating) {
        viewModelScope.launch {
            repository.recordReview(card, rating)
            _index.value += 1
        }
    }
}
