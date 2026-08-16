package com.rainyday.saveableapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.JsFun

@JsFun(
    """
    (text) => {
        if (navigator.share) {
            navigator.share({ text: text }).catch(() => {});
            return true;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text);
            return false;
        }
        return false;
    }
    """
)
private external fun jsShareOrCopy(text: String): Boolean

@Composable
actual fun rememberShareText(): ShareText = remember {
    ShareText { text -> jsShareOrCopy(text) }
}
