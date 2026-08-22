package com.rainyday.saveableapp.ui.screens.todo

import com.rainyday.saveableapp.data.local.Priority

fun Priority.accentHex(): String = when (this) {
    Priority.LOW -> "#8A8F98"
    Priority.MEDIUM -> "#4C8FE0"
    Priority.HIGH -> "#F5A623"
    Priority.URGENT -> "#F0465F"
}

fun Priority.label(): String = when (this) {
    Priority.LOW -> "Low"
    Priority.MEDIUM -> "Medium"
    Priority.HIGH -> "High"
    Priority.URGENT -> "Urgent"
}
