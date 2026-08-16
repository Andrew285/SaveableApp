package com.rainyday.saveableapp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.JsFun

@JsFun(
    """
    (content, mimeType, fileName) => {
        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
    }
    """
)
private external fun jsDownloadTextFile(content: String, mimeType: String, fileName: String)

@Composable
actual fun rememberBackupFileIO(): BackupFileIO = remember {
    object : BackupFileIO {
        override fun exportJson(fileName: String, content: String, onDone: (Boolean) -> Unit) {
            jsDownloadTextFile(content, "application/json", fileName)
            onDone(true)
        }

        override fun importJson(onResult: (String?) -> Unit) {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "application/json"
            input.onchange = {
                val file = input.files?.item(0)
                if (file == null) {
                    onResult(null)
                } else {
                    val reader = FileReader()
                    reader.onload = {
                        onResult(reader.result as? String)
                    }
                    reader.onerror = {
                        onResult(null)
                    }
                    reader.readAsText(file)
                }
                Unit
            }
            input.click()
        }
    }
}
