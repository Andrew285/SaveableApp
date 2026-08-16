package com.rainyday.saveableapp.di

import android.content.Context
import com.rainyday.saveableapp.data.local.AppDatabase
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.BackupRepository
import com.rainyday.saveableapp.data.repository.InfoRepository
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.TodoRepository

class AppContainer(context: Context) {
    private val database by lazy { AppDatabase.getInstance(context) }

    val todoRepository by lazy {
        TodoRepository(database.todoListDao(), database.todoTaskDao(), database.tagDao())
    }

    val listsRepository by lazy {
        ListsRepository(database.simpleListDao(), database.simpleListItemDao())
    }

    val infoRepository by lazy {
        InfoRepository(database.infoCategoryDao(), database.infoBlockDao())
    }

    val preferencesRepository by lazy { PreferencesRepository(context.applicationContext) }

    val backupRepository by lazy { BackupRepository(database, database.backupDao()) }
}
