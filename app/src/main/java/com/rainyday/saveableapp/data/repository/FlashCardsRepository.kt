package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.DeckCardCount
import com.rainyday.saveableapp.data.local.FlashCardDao
import com.rainyday.saveableapp.data.local.FlashCardDeckDao
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.sync.SyncOutbox
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class FlashCardDeckSnapshot(val deck: FlashCardDeckEntity, val cards: List<FlashCardEntity>)

class FlashCardsRepository(
    private val deckDao: FlashCardDeckDao,
    private val cardDao: FlashCardDao,
    private val syncOutbox: SyncOutbox
) {
    fun observeDecks(): Flow<List<FlashCardDeckEntity>> = deckDao.observeDecks()

    fun observeDeck(deckId: String): Flow<FlashCardDeckEntity?> = deckDao.observeById(deckId)

    fun observeCounts(): Flow<List<DeckCardCount>> = cardDao.observeCounts()

    fun observeDueCounts(now: Long = System.currentTimeMillis()): Flow<List<DeckCardCount>> = cardDao.observeDueCounts(now)

    suspend fun createDeck(name: String, icon: String, colorHex: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        deckDao.insert(FlashCardDeckEntity(id = id, name = name, icon = icon, colorHex = colorHex, createdAt = now, updatedAt = now))
        syncOutbox.upsert(SyncEntityType.FLASH_CARD_DECK, id)
        return id
    }

    suspend fun updateDeck(deck: FlashCardDeckEntity) {
        deckDao.update(deck.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.FLASH_CARD_DECK, deck.id)
    }

    /** Room cascades the local delete of [deck]'s cards; enqueue their ids too or they orphan in Firestore. */
    suspend fun deleteDeckWithSnapshot(deck: FlashCardDeckEntity): FlashCardDeckSnapshot {
        val cards = cardDao.observeCardsForDeck(deck.id).first()
        deckDao.delete(deck)
        syncOutbox.deleteAll(SyncEntityType.FLASH_CARD, cards.map { it.id })
        syncOutbox.delete(SyncEntityType.FLASH_CARD_DECK, deck.id)
        return FlashCardDeckSnapshot(deck, cards)
    }

    suspend fun restoreDeck(snapshot: FlashCardDeckSnapshot) {
        deckDao.insert(snapshot.deck)
        snapshot.cards.forEach { cardDao.insert(it) }
        syncOutbox.upsert(SyncEntityType.FLASH_CARD_DECK, snapshot.deck.id)
        syncOutbox.upsertAll(SyncEntityType.FLASH_CARD, snapshot.cards.map { it.id })
    }

    fun observeCards(deckId: String): Flow<List<FlashCardEntity>> = cardDao.observeCardsForDeck(deckId)

    suspend fun createCard(deckId: String, front: String, back: String): String {
        val position = cardDao.observeCardsForDeck(deckId).first().size
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        cardDao.insert(
            FlashCardEntity(
                id = id,
                deckId = deckId,
                front = front,
                back = back,
                position = position,
                createdAt = now,
                updatedAt = now
            )
        )
        syncOutbox.upsert(SyncEntityType.FLASH_CARD, id)
        return id
    }

    suspend fun updateCard(card: FlashCardEntity) {
        cardDao.update(card.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.FLASH_CARD, card.id)
    }

    suspend fun deleteCard(card: FlashCardEntity) {
        cardDao.delete(card)
        syncOutbox.delete(SyncEntityType.FLASH_CARD, card.id)
    }

    suspend fun restoreCard(card: FlashCardEntity) {
        cardDao.insert(card)
        syncOutbox.upsert(SyncEntityType.FLASH_CARD, card.id)
    }

    suspend fun reorderCards(cards: List<FlashCardEntity>) {
        val now = System.currentTimeMillis()
        cardDao.updateAll(cards.mapIndexed { index, card -> card.copy(position = index, updatedAt = now) })
        syncOutbox.upsertAll(SyncEntityType.FLASH_CARD, cards.map { it.id })
    }

    suspend fun getDueCards(deckId: String, now: Long = System.currentTimeMillis()): List<FlashCardEntity> =
        cardDao.getDueCards(deckId, now)

    suspend fun getAllCardsForStudy(deckId: String): List<FlashCardEntity> = cardDao.getAllCards(deckId)

    suspend fun recordReview(card: FlashCardEntity, rating: CardRating) {
        val updated = SpacedRepetition.schedule(card, rating).copy(updatedAt = System.currentTimeMillis())
        cardDao.update(updated)
        syncOutbox.upsert(SyncEntityType.FLASH_CARD, card.id)
    }
}
