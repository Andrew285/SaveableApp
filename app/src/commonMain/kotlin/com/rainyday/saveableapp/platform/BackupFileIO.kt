package com.rainyday.saveableapp.platform

import androidx.compose.runtime.Composable

/** Platform file I/O for the JSON backup blob (also reused as the Drive sync payload format). */
interface BackupFileIO {
    fun exportJson(fileName: String, content: String, onDone: (success: Boolean) -> Unit)
    fun importJson(onResult: (content: String?) -> Unit)
}

/** Android: SAF document picker. Web: browser download + a hidden file input. */
@Composable
expect fun rememberBackupFileIO(): BackupFileIO
