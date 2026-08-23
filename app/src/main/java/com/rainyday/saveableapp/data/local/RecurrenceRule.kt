package com.rainyday.saveableapp.data.local

import kotlinx.serialization.Serializable

@Serializable
enum class RecurrenceRule {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}
