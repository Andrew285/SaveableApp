package com.rainyday.saveableapp.di

import com.rainyday.saveableapp.data.links.LinkPreviewRepository
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Accessor for Hilt singletons needed inside plain `@Composable`s that aren't backed by a
 * `@HiltViewModel` (too trivial to warrant one) — see [com.rainyday.saveableapp.ui.rememberAppEntryPoint].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun preferencesRepository(): PreferencesRepository
    fun linkPreviewRepository(): LinkPreviewRepository
}
