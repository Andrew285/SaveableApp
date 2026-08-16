package com.rainyday.saveableapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.rainyday.saveableapp.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided")
}

@Composable
fun appContainer(): AppContainer = LocalAppContainer.current
