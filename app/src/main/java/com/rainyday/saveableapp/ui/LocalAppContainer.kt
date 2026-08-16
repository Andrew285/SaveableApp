package com.rainyday.saveableapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.rainyday.saveableapp.SaveableApplication
import com.rainyday.saveableapp.di.AppContainer

@Composable
fun appContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as SaveableApplication).container
}
