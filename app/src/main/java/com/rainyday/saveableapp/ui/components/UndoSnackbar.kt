package com.rainyday.saveableapp.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Runs [delete] immediately, then offers an [actionLabel] snackbar that calls [restore]
 * with whatever [delete] returned (a snapshot of what was removed) if tapped in time.
 *
 * [actionLabel] has no default so callers resolve it from `R.string.action_undo` via
 * `stringResource` rather than hardcoding it here, where a non-@Composable function can't reach
 * string resources itself. If the message/label depends on data only known once this suspend
 * function is actually invoked (e.g. from a callback fired well after composition), resolve the
 * `stringResource` pattern/string in composable scope ahead of time and apply any runtime args
 * with `.format(...)` at the call site — avoid `context.getString(...)`, since reading resources
 * through a raw `Context` bypasses Compose's config-change recomposition (locale, night mode, etc).
 */
suspend fun <T> SnackbarHostState.showUndoableDelete(
    message: String,
    actionLabel: String,
    delete: suspend () -> T,
    restore: suspend (T) -> Unit
) {
    val snapshot = delete()
    val result = showSnackbar(message = message, actionLabel = actionLabel, duration = SnackbarDuration.Short)
    if (result == SnackbarResult.ActionPerformed) {
        restore(snapshot)
    }
}
