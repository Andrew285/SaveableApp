package com.rainyday.saveableapp.ui.theme

import androidx.compose.ui.graphics.Color

// Light scheme — same "vault" design language as dark, lighter surfaces.
val LightPrimary = Color(0xFF0FAF82)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD3F5E6)
val LightOnPrimaryContainer = Color(0xFF063D2C)

val LightSecondary = Color(0xFF4C8FE0)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFDCE9FA)
val LightOnSecondaryContainer = Color(0xFF16324F)

val LightTertiary = Color(0xFF8B5FD9)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFEADDFB)
val LightOnTertiaryContainer = Color(0xFF2C1854)

val LightBackground = Color(0xFFF6F7F9)
val LightOnBackground = Color(0xFF14161A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF14161A)
val LightSurfaceVariant = Color(0xFFFFFFFF)
val LightOnSurfaceVariant = Color(0xFF62666E)
val LightOutline = Color(0xFFDDE0E4)

val LightError = Color(0xFFD8324B)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFBDADF)
val LightOnErrorContainer = Color(0xFF410007)

// Dark scheme — matches the "Vault" mockups: near-black background, solid dark
// cards, mint accent, monospace-flavored meta text.
val DarkPrimary = Color(0xFF2EE6A8)
val DarkOnPrimary = Color(0xFF00382A)
val DarkPrimaryContainer = Color(0xFF0B4A38)
val DarkOnPrimaryContainer = Color(0xFFB6FBE0)

val DarkSecondary = Color(0xFF4C8FE0)
val DarkOnSecondary = Color(0xFF06264B)
val DarkSecondaryContainer = Color(0xFF12386B)
val DarkOnSecondaryContainer = Color(0xFFD6E6FF)

val DarkTertiary = Color(0xFFA855F7)
val DarkOnTertiary = Color(0xFF2C0A54)
val DarkTertiaryContainer = Color(0xFF432370)
val DarkOnTertiaryContainer = Color(0xFFEBDDFF)

val DarkBackground = Color(0xFF0A0B0D)
val DarkOnBackground = Color(0xFFF5F6F7)
val DarkSurface = Color(0xFF0A0B0D)
val DarkOnSurface = Color(0xFFF5F6F7)
val DarkSurfaceVariant = Color(0xFF171A1F)
val DarkOnSurfaceVariant = Color(0xFF8A8F98)
val DarkOutline = Color(0xFF262A30)

val DarkError = Color(0xFFF0465F)
val DarkOnError = Color(0xFF4A0011)
val DarkErrorContainer = Color(0xFF551020)
val DarkOnErrorContainer = Color(0xFFFFD9DE)

/** Priority accent colors shared by Tasks screens. */
object PriorityColors {
    val low = Color(0xFF8A8F98)
    val medium = Color(0xFF4C8FE0)
    val high = Color(0xFFF5A623)
    val urgent = Color(0xFFF0465F)
}

/** Curated accent colors users can pick for lists, tags, and task highlights. Mint leads, matching
 *  the highlight-color picker in the mockups. */
object AccentColors {
    val palette = listOf(
        "#2EE6A8", // mint
        "#4C8FE0", // blue
        "#F5A623", // orange
        "#F0465F", // red
        "#A855F7", // purple
        "#E4574C", // coral red
        "#3FA796", // teal
        "#D2609C", // pink
        "#6FB668", // green
        "#6E7280", // graphite
    )
}
