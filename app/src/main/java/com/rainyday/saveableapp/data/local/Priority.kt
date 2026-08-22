package com.rainyday.saveableapp.data.local

import kotlinx.serialization.Serializable

@Serializable
enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}
