package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.FlashCardEntity
import kotlin.math.ceil

enum class CardRating { HARD, MEDIUM, EASY }

private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000
private const val MIN_EASE_FACTOR = 1.3

/**
 * Simplified SM-2-style spaced repetition. Every rating counts as a successful recall (there's no
 * "Again"/fail button), so repetitions always advance; the rating instead shapes how much the
 * interval grows and how the ease factor drifts for future reviews.
 */
object SpacedRepetition {
    fun schedule(card: FlashCardEntity, rating: CardRating, now: Long = System.currentTimeMillis()): FlashCardEntity {
        val repetitions = card.repetitions + 1

        val intervalDays = when (rating) {
            CardRating.HARD -> if (repetitions <= 1) 1 else ceil(card.intervalDays * 1.2).toInt()
            CardRating.MEDIUM -> when (repetitions) {
                1 -> 1
                2 -> 6
                else -> ceil(card.intervalDays * card.easeFactor).toInt()
            }
            CardRating.EASY -> when (repetitions) {
                1 -> 4
                2 -> 10
                else -> ceil(card.intervalDays * card.easeFactor * 1.3).toInt()
            }
        }.coerceAtLeast(1)

        val easeDelta = when (rating) {
            CardRating.HARD -> -0.15
            CardRating.MEDIUM -> 0.0
            CardRating.EASY -> 0.15
        }
        val easeFactor = (card.easeFactor + easeDelta).coerceAtLeast(MIN_EASE_FACTOR)

        return card.copy(
            repetitions = repetitions,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            dueAt = now + intervalDays * ONE_DAY_MILLIS
        )
    }
}
