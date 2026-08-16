package com.rainyday.saveableapp.platform

import androidx.compose.runtime.Composable

fun interface ShareText {
    fun share(text: String)
}

/** Android: ACTION_SEND chooser. Web: navigator.share() with a clipboard-copy fallback. */
@Composable
expect fun rememberShareText(): ShareText
