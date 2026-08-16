package com.rainyday.saveableapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TodoListEntity::class,
        TodoTaskEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        SimpleListEntity::class,
        SimpleListItemEntity::class,
        InfoCategoryEntity::class,
        InfoBlockEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoListDao(): TodoListDao
    abstract fun todoTaskDao(): TodoTaskDao
    abstract fun tagDao(): TagDao
    abstract fun simpleListDao(): SimpleListDao
    abstract fun simpleListItemDao(): SimpleListItemDao
    abstract fun infoCategoryDao(): InfoCategoryDao
    abstract fun infoBlockDao(): InfoBlockDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_tasks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE info_blocks ADD COLUMN expiryDate INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_app.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
