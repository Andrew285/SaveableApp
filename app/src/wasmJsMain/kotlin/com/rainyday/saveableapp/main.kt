package com.rainyday.saveableapp

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.rainyday.saveableapp.data.local.createWasmDatabaseDriver
import com.rainyday.saveableapp.di.AppContainer
import com.rainyday.saveableapp.ui.LocalAppContainer
import com.rainyday.saveableapp.ui.TodoApp
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MainScope().launch {
        val driver = createWasmDatabaseDriver()
        val settings = StorageSettings(localStorage)
        val container = AppContainer(driver, settings)
        ComposeViewport(document.body!!) {
            CompositionLocalProvider(LocalAppContainer provides container) {
                TodoApp()
            }
        }
    }
}
