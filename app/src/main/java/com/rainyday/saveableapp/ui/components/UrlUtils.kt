package com.rainyday.saveableapp.ui.components

private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

/** Normalizes user-entered link text into a URL a browser can open, or null if blank. */
fun normalizeUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return if (schemeRegex.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
}
