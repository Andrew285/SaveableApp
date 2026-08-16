package com.rainyday.saveableapp.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual fun isDynamicColorSupported(): Boolean = false

@Composable
actual fun dynamicColorSchemeOrNull(dark: Boolean): ColorScheme? = null
