package com.rainyday.saveableapp.di

import app.cash.sqldelight.db.SqlDriver
import com.russhwolf.settings.Settings
import com.rainyday.saveableapp.data.local.createAppDatabase
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.BackupRepository
import com.rainyday.saveableapp.data.repository.InfoRepository
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.TodoRepository

class AppContainer(driver: SqlDriver, settings: Settings) {
    private val database = createAppDatabase(driver)

    val todoRepository by lazy { TodoRepository(database) }
    val listsRepository by lazy { ListsRepository(database) }
    val infoRepository by lazy { InfoRepository(database) }
    val preferencesRepository by lazy { PreferencesRepository(settings) }
    val backupRepository by lazy { BackupRepository(database) }
}
