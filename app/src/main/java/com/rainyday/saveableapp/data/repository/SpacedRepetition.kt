package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.FlashCardEntity
import kotlin.math.ceil

enum class CardRating { AGAIN, HARD, GOOD, EASY }

private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000
private const val MIN_EASE_FACTOR = 1.3

/** Simplified SM-2-style spaced repetition, with a standard "Again" (forgot it) branch that
 *  resets the repetition streak and schedules a near-immediate re-review. */
object SpacedRepetition {
    fun schedule(card: FlashCardEntity, rating: CardRating, now: Long = System.currentTimeMillis()): FlashCardEntity {
        if (rating == CardRating.AGAIN) {
            val easeFactor = (card.easeFactor - 0.2).coerceAtLeast(MIN_EASE_FACTOR)
            return card.copy(
                repetitions = 0,
                easeFactor = easeFactor,
                intervalDays = 0,
                dueAt = now + (10 * 60 * 1000L) // ~10 minutes
            )
        }

        val repetitions = card.repetitions + 1

        val intervalDays = when (rating) {
            CardRating.HARD -> if (repetitions <= 1) 1 else ceil(card.intervalDays * 1.2).toInt()
            CardRating.GOOD -> when (repetitions) {
                1 -> 1
                2 -> 6
                else -> ceil(card.intervalDays * card.easeFactor).toInt()
            }
            CardRating.EASY -> when (repetitions) {
                1 -> 4
                2 -> 10
                else -> ceil(card.intervalDays * card.easeFactor * 1.3).toInt()
            }
            CardRating.AGAIN -> 0 // unreachable, handled above
        }.coerceAtLeast(1)

        val easeDelta = when (rating) {
            CardRating.HARD -> -0.15
            CardRating.GOOD -> 0.0
            CardRating.EASY -> 0.15
            CardRating.AGAIN -> 0.0 // unreachable, handled above
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
