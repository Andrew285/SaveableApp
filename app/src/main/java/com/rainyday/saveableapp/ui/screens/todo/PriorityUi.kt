package com.rainyday.saveableapp.ui.screens.todo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.Priority

fun Priority.accentHex(): String = when (this) {
    Priority.LOW -> "#8A8F98"
    Priority.MEDIUM -> "#4C8FE0"
    Priority.HIGH -> "#F5A623"
    Priority.URGENT -> "#F0465F"
}

@Composable
fun Priority.label(): String = stringResource(
    when (this) {
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
        Priority.URGENT -> R.string.priority_urgent
    }
)
