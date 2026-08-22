package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.DeckCardCount
import com.rainyday.saveableapp.data.local.FlashCardDao
import com.rainyday.saveableapp.data.local.FlashCardDeckDao
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class FlashCardDeckSnapshot(val deck: FlashCardDeckEntity, val cards: List<FlashCardEntity>)

class FlashCardsRepository(
    private val deckDao: FlashCardDeckDao,
    private val cardDao: FlashCardDao
) {
    fun observeDecks(): Flow<List<FlashCardDeckEntity>> = deckDao.observeDecks()

    fun observeDeck(deckId: Long): Flow<FlashCardDeckEntity?> = deckDao.observeById(deckId)

    fun observeCounts(): Flow<List<DeckCardCount>> = cardDao.observeCounts()

    fun observeDueCounts(now: Long = System.currentTimeMillis()): Flow<List<DeckCardCount>> = cardDao.observeDueCounts(now)

    suspend fun createDeck(name: String, icon: String, colorHex: String): Long =
        deckDao.insert(
            FlashCardDeckEntity(
                name = name,
                icon = icon,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis()
            )
        )

    suspend fun updateDeck(deck: FlashCardDeckEntity) = deckDao.update(deck)

    suspend fun deleteDeckWithSnapshot(deck: FlashCardDeckEntity): FlashCardDeckSnapshot {
        val cards = cardDao.observeCardsForDeck(deck.id).first()
        deckDao.delete(deck)
        return FlashCardDeckSnapshot(deck, cards)
    }

    suspend fun restoreDeck(snapshot: FlashCardDeckSnapshot) {
        deckDao.insert(snapshot.deck)
        snapshot.cards.forEach { cardDao.insert(it) }
    }

    fun observeCards(deckId: Long): Flow<List<FlashCardEntity>> = cardDao.observeCardsForDeck(deckId)

    suspend fun createCard(deckId: Long, front: String, back: String): Long {
        val position = cardDao.observeCardsForDeck(deckId).first().size
        return cardDao.insert(
            FlashCardEntity(
                deckId = deckId,
                front = front,
                back = back,
                position = position,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateCard(card: FlashCardEntity) = cardDao.update(card)

    suspend fun deleteCard(card: FlashCardEntity) = cardDao.delete(card)

    suspend fun restoreCard(card: FlashCardEntity) = cardDao.insert(card)

    suspend fun reorderCards(cards: List<FlashCardEntity>) =
        cardDao.updateAll(cards.mapIndexed { index, card -> card.copy(position = index) })

    suspend fun getDueCards(deckId: Long, now: Long = System.currentTimeMillis()): List<FlashCardEntity> =
        cardDao.getDueCards(deckId, now)

    suspend fun getAllCardsForStudy(deckId: Long): List<FlashCardEntity> = cardDao.getAllCards(deckId)

    suspend fun recordReview(card: FlashCardEntity, rating: CardRating) {
        cardDao.update(SpacedRepetition.schedule(card, rating))
    }
}
