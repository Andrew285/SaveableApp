package com.rainyday.saveableapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rainyday.saveableapp.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors

/** Resolves [AppEntryPoint] for the handful of plain composables that need a Hilt singleton directly. */
@Composable
fun rememberAppEntryPoint(): AppEntryPoint {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, AppEntryPoint::class.java)
    }
}
