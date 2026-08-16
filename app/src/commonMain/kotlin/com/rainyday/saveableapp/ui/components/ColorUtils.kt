package com.rainyday.saveableapp.ui.components

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String): Color = runCatching {
    val cleaned = hex.removePrefix("#")
    val argb = when (cleaned.length) {
        6 -> 0xFF000000L or cleaned.toLong(16)
        8 -> cleaned.toLong(16)
        else -> error("Invalid hex color: $hex")
    }
    Color(argb.toInt())
}.getOrDefault(Color.Gray)
