package com.rainyday.saveableapp.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Runs [delete] immediately, then offers an "Undo" snackbar that calls [restore]
 * with whatever [delete] returned (a snapshot of what was removed) if tapped in time.
 */
suspend fun <T> SnackbarHostState.showUndoableDelete(
    message: String,
    delete: suspend () -> T,
    restore: suspend (T) -> Unit
) {
    val snapshot = delete()
    val result = showSnackbar(message = message, actionLabel = "Undo", duration = SnackbarDuration.Short)
    if (result == SnackbarResult.ActionPerformed) {
        restore(snapshot)
    }
}
