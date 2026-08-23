package com.rainyday.saveableapp.di

import android.content.Context
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.rainyday.saveableapp.data.ai.OpenRouterRepository
import com.rainyday.saveableapp.data.auth.FirebaseAuthRepository
import com.rainyday.saveableapp.data.drive.DriveBackupRepository
import com.rainyday.saveableapp.data.links.LinkPreviewRepository
import com.rainyday.saveableapp.data.local.AppDatabase
import com.rainyday.saveableapp.data.local.BackupDao
import com.rainyday.saveableapp.data.local.FieldDefinitionDao
import com.rainyday.saveableapp.data.local.FieldValueDao
import com.rainyday.saveableapp.data.local.FlashCardDao
import com.rainyday.saveableapp.data.local.FlashCardDeckDao
import com.rainyday.saveableapp.data.local.InfoBlockDao
import com.rainyday.saveableapp.data.local.InfoCategoryDao
import com.rainyday.saveableapp.data.local.LinkPreviewDao
import com.rainyday.saveableapp.data.local.SimpleListDao
import com.rainyday.saveableapp.data.local.SimpleListItemDao
import com.rainyday.saveableapp.data.local.SyncOutboxDao
import com.rainyday.saveableapp.data.local.TagDao
import com.rainyday.saveableapp.data.local.TodoListDao
import com.rainyday.saveableapp.data.local.TodoTaskDao
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.BackupRepository
import com.rainyday.saveableapp.data.repository.FlashCardsRepository
import com.rainyday.saveableapp.data.repository.InfoRepository
import com.rainyday.saveableapp.data.repository.ListsRepository
import com.rainyday.saveableapp.data.repository.TodoRepository
import com.rainyday.saveableapp.data.scheduling.AutoBackupScheduler
import com.rainyday.saveableapp.data.scheduling.TaskReminderScheduler
import com.rainyday.saveableapp.data.sync.SyncOutbox
import com.rainyday.saveableapp.data.sync.SyncPullRepository
import com.rainyday.saveableapp.data.sync.SyncPushRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideTodoListDao(database: AppDatabase): TodoListDao = database.todoListDao()

    @Provides
    fun provideTodoTaskDao(database: AppDatabase): TodoTaskDao = database.todoTaskDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideSimpleListDao(database: AppDatabase): SimpleListDao = database.simpleListDao()

    @Provides
    fun provideSimpleListItemDao(database: AppDatabase): SimpleListItemDao = database.simpleListItemDao()

    @Provides
    fun provideFieldDefinitionDao(database: AppDatabase): FieldDefinitionDao = database.fieldDefinitionDao()

    @Provides
    fun provideFieldValueDao(database: AppDatabase): FieldValueDao = database.fieldValueDao()

    @Provides
    fun provideInfoCategoryDao(database: AppDatabase): InfoCategoryDao = database.infoCategoryDao()

    @Provides
    fun provideInfoBlockDao(database: AppDatabase): InfoBlockDao = database.infoBlockDao()

    @Provides
    fun provideFlashCardDeckDao(database: AppDatabase): FlashCardDeckDao = database.flashCardDeckDao()

    @Provides
    fun provideFlashCardDao(database: AppDatabase): FlashCardDao = database.flashCardDao()

    @Provides
    fun provideLinkPreviewDao(database: AppDatabase): LinkPreviewDao = database.linkPreviewDao()

    @Provides
    fun provideBackupDao(database: AppDatabase): BackupDao = database.backupDao()

    @Provides
    fun provideSyncOutboxDao(database: AppDatabase): SyncOutboxDao = database.syncOutboxDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideSyncOutbox(syncOutboxDao: SyncOutboxDao, workManager: WorkManager): SyncOutbox =
        SyncOutbox(syncOutboxDao, workManager)

    @Provides
    @Singleton
    fun provideSyncPushRepository(
        syncOutboxDao: SyncOutboxDao,
        firebaseAuthRepository: FirebaseAuthRepository,
        firestore: FirebaseFirestore,
        todoListDao: TodoListDao,
        todoTaskDao: TodoTaskDao,
        tagDao: TagDao,
        simpleListDao: SimpleListDao,
        simpleListItemDao: SimpleListItemDao,
        infoCategoryDao: InfoCategoryDao,
        infoBlockDao: InfoBlockDao,
        flashCardDeckDao: FlashCardDeckDao,
        flashCardDao: FlashCardDao,
        fieldDefinitionDao: FieldDefinitionDao,
        fieldValueDao: FieldValueDao
    ): SyncPushRepository = SyncPushRepository(
        syncOutboxDao, firebaseAuthRepository, firestore,
        todoListDao, todoTaskDao, tagDao,
        simpleListDao, simpleListItemDao,
        infoCategoryDao, infoBlockDao,
        flashCardDeckDao, flashCardDao,
        fieldDefinitionDao, fieldValueDao
    )

    @Provides
    @Singleton
    fun provideSyncPullRepository(
        firestore: FirebaseFirestore,
        todoListDao: TodoListDao,
        todoTaskDao: TodoTaskDao,
        tagDao: TagDao,
        simpleListDao: SimpleListDao,
        simpleListItemDao: SimpleListItemDao,
        infoCategoryDao: InfoCategoryDao,
        infoBlockDao: InfoBlockDao,
        flashCardDeckDao: FlashCardDeckDao,
        flashCardDao: FlashCardDao,
        fieldDefinitionDao: FieldDefinitionDao,
        fieldValueDao: FieldValueDao
    ): SyncPullRepository = SyncPullRepository(
        firestore,
        todoListDao, todoTaskDao, tagDao,
        simpleListDao, simpleListItemDao,
        infoCategoryDao, infoBlockDao,
        flashCardDeckDao, flashCardDao,
        fieldDefinitionDao, fieldValueDao
    )

    @Provides
    @Singleton
    fun provideTodoRepository(
        todoListDao: TodoListDao,
        todoTaskDao: TodoTaskDao,
        tagDao: TagDao,
        syncOutbox: SyncOutbox
    ): TodoRepository = TodoRepository(todoListDao, todoTaskDao, tagDao, syncOutbox)

    @Provides
    @Singleton
    fun provideListsRepository(
        simpleListDao: SimpleListDao,
        simpleListItemDao: SimpleListItemDao,
        fieldDefinitionDao: FieldDefinitionDao,
        fieldValueDao: FieldValueDao,
        syncOutbox: SyncOutbox
    ): ListsRepository = ListsRepository(simpleListDao, simpleListItemDao, fieldDefinitionDao, fieldValueDao, syncOutbox)

    @Provides
    @Singleton
    fun provideInfoRepository(
        infoCategoryDao: InfoCategoryDao,
        infoBlockDao: InfoBlockDao,
        syncOutbox: SyncOutbox
    ): InfoRepository = InfoRepository(infoCategoryDao, infoBlockDao, syncOutbox)

    @Provides
    @Singleton
    fun provideFlashCardsRepository(
        flashCardDeckDao: FlashCardDeckDao,
        flashCardDao: FlashCardDao,
        syncOutbox: SyncOutbox
    ): FlashCardsRepository = FlashCardsRepository(flashCardDeckDao, flashCardDao, syncOutbox)

    @Provides
    @Singleton
    fun providePreferencesRepository(@ApplicationContext context: Context): PreferencesRepository =
        PreferencesRepository(context)

    @Provides
    @Singleton
    fun provideFirebaseAuthRepository(): FirebaseAuthRepository = FirebaseAuthRepository()

    @Provides
    @Singleton
    fun provideOpenRouterRepository(firebaseAuthRepository: FirebaseAuthRepository): OpenRouterRepository =
        OpenRouterRepository(firebaseAuthRepository)

    @Provides
    @Singleton
    fun provideLinkPreviewRepository(linkPreviewDao: LinkPreviewDao): LinkPreviewRepository =
        LinkPreviewRepository(linkPreviewDao)

    @Provides
    @Singleton
    fun provideBackupRepository(database: AppDatabase, backupDao: BackupDao): BackupRepository =
        BackupRepository(database, backupDao)

    @Provides
    @Singleton
    fun provideDriveBackupRepository(
        @ApplicationContext context: Context,
        backupRepository: BackupRepository,
        preferencesRepository: PreferencesRepository
    ): DriveBackupRepository = DriveBackupRepository(context, backupRepository, preferencesRepository)

    @Provides
    @Singleton
    fun provideTaskReminderScheduler(@ApplicationContext context: Context): TaskReminderScheduler =
        TaskReminderScheduler(context)

    @Provides
    @Singleton
    fun provideAutoBackupScheduler(@ApplicationContext context: Context): AutoBackupScheduler =
        AutoBackupScheduler(context)
}
