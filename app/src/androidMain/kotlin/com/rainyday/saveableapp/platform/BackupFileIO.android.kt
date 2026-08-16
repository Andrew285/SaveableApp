package com.rainyday.saveableapp.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberBackupFileIO(): BackupFileIO {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingExport = remember { mutableStateOf<Pair<String, (Boolean) -> Unit>?>(null) }
    val pendingImport = remember { mutableStateOf<((String?) -> Unit)?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val (content, onDone) = pendingExport.value ?: return@rememberLauncherForActivityResult
        pendingExport.value = null
        if (uri == null) {
            onDone(false)
        } else {
            scope.launch(Dispatchers.IO) {
                val success = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
                withContext(Dispatchers.Main) { onDone(success) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val onResult = pendingImport.value ?: return@rememberLauncherForActivityResult
        pendingImport.value = null
        if (uri == null) {
            onResult(null)
        } else {
            scope.launch(Dispatchers.IO) {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }.getOrNull()
                withContext(Dispatchers.Main) { onResult(text) }
            }
        }
    }

    return remember {
        object : BackupFileIO {
            override fun exportJson(fileName: String, content: String, onDone: (Boolean) -> Unit) {
                pendingExport.value = content to onDone
                exportLauncher.launch(fileName)
            }

            override fun importJson(onResult: (String?) -> Unit) {
                pendingImport.value = onResult
                importLauncher.launch(arrayOf("application/json"))
            }
        }
    }
}
