package com.rainyday.saveableapp.ui.screens.todo

import com.rainyday.saveableapp.data.local.Priority

fun Priority.accentHex(): String = when (this) {
    Priority.LOW -> "#6FB668"
    Priority.MEDIUM -> "#E8B23A"
    Priority.HIGH -> "#E4574C"
}

fun Priority.label(): String = when (this) {
    Priority.LOW -> "Low"
    Priority.MEDIUM -> "Medium"
    Priority.HIGH -> "High"
}
