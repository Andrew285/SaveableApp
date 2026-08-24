package com.rainyday.saveableapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The display/mono faces in AppFonts.kt are downloadable Google Fonts, which need Play Services
 * to resolve and never load inside Android Studio's @Preview renderer (no Play Services there) —
 * without this fallback every @Preview using MaterialTheme.typography renders blank/broken text.
 */
@Composable
private fun previewSafe(remote: FontFamily): FontFamily =
    if (LocalInspectionMode.current) FontFamily.Default else remote

// Headlines/titles use Space Grotesk (bold geometric sans, matches the mockups' big screen
// titles like "Active Tasks"). Eyebrows, meta text, tags, buttons and nav labels use JetBrains
// Mono. Body copy (descriptions, row titles) stays the platform default sans for readability.
val AppTypography: Typography
    @Composable
    get() {
        val display = previewSafe(SpaceGrotesk)
        val mono = previewSafe(JetBrainsMono)
        return Typography(
            displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.5).sp),
            displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp),
            displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),

            headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.25).sp),
            headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
            headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),

            titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
            titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),

            bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
            bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),

            labelLarge = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
            labelMedium = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
            labelSmall = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp)
        )
    }

/** Small mint mono eyebrow label style, e.g. "// TASKS" above a screen title. */
val EyebrowTextStyle: TextStyle
    @Composable
    get() = TextStyle(
        fontFamily = previewSafe(JetBrainsMono),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )
