package com.rainyday.saveableapp.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Whether the platform can derive a Material You color scheme from the user's environment. */
expect fun isDynamicColorSupported(): Boolean

/** Android S+: dynamicLightColorScheme/dynamicDarkColorScheme. Everywhere else: null. */
@Composable
expect fun dynamicColorSchemeOrNull(dark: Boolean): ColorScheme?
