package com.rainyday.saveableapp.di

import android.content.Context
import com.rainyday.saveableapp.data.ai.GroqRepository
import com.rainyday.saveableapp.data.drive.DriveBackupRepository
import com.rainyday.saveableapp.data.links.LinkPreviewRepository
import com.rainyday.saveableapp.data.local.AppDatabase
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.BackupRepository
import com.rainyday.saveableapp.data.repository.FlashCardsRepository
import com.rainyday.saveableapp.data.repository.InfoRepository
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.TodoRepository
import com.rainyday.saveableapp.data.scheduling.AutoBackupScheduler
import com.rainyday.saveableapp.data.scheduling.TaskReminderScheduler

class AppContainer(context: Context) {
    private val database by lazy { AppDatabase.getInstance(context) }

    val todoRepository by lazy {
        TodoRepository(database.todoListDao(), database.todoTaskDao(), database.tagDao())
    }

    val listsRepository by lazy {
        ListsRepository(
            database.simpleListDao(),
            database.simpleListItemDao(),
            database.fieldDefinitionDao(),
            database.fieldValueDao()
        )
    }

    val infoRepository by lazy {
        InfoRepository(database.infoCategoryDao(), database.infoBlockDao())
    }

    val flashCardsRepository by lazy {
        FlashCardsRepository(database.flashCardDeckDao(), database.flashCardDao())
    }

    val preferencesRepository by lazy { PreferencesRepository(context.applicationContext) }

    val groqRepository by lazy { GroqRepository(preferencesRepository) }

    val linkPreviewRepository by lazy { LinkPreviewRepository(database.linkPreviewDao()) }

    val backupRepository by lazy { BackupRepository(database, database.backupDao()) }

    val driveBackupRepository by lazy {
        DriveBackupRepository(context.applicationContext, backupRepository, preferencesRepository)
    }

    val taskReminderScheduler by lazy { TaskReminderScheduler(context.applicationContext) }

    val autoBackupScheduler by lazy { AutoBackupScheduler(context.applicationContext) }
}
