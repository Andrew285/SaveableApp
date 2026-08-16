package com.rainyday.saveableapp.platform

import androidx.compose.runtime.Composable

/** Blocks screenshots/recents-preview while a sensitive screen is on screen (no-op where unsupported). */
@Composable
expect fun SecureScreenEffect()
