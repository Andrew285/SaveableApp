package com.rainyday.saveableapp.ui.components

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color.Gray)
