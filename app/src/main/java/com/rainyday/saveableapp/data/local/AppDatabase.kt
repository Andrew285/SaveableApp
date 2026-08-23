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
        InfoBlockEntity::class,
        FlashCardDeckEntity::class,
        FlashCardEntity::class,
        FieldDefinitionEntity::class,
        FieldValueEntity::class,
        LinkPreviewCacheEntity::class
    ],
    version = 7,
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
    abstract fun flashCardDeckDao(): FlashCardDeckDao
    abstract fun flashCardDao(): FlashCardDao
    abstract fun fieldDefinitionDao(): FieldDefinitionDao
    abstract fun fieldValueDao(): FieldValueDao
    abstract fun linkPreviewDao(): LinkPreviewDao
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE simple_list_items ADD COLUMN url TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `flashcard_decks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `icon` TEXT NOT NULL, `colorHex` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `flashcards` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`deckId` INTEGER NOT NULL, `front` TEXT NOT NULL, `back` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`deckId`) REFERENCES `flashcard_decks`(`id`) ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_deckId` ON `flashcards` (`deckId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN easeFactor REAL NOT NULL DEFAULT 2.5")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN dueAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `list_field_definitions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`listId` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` INTEGER NOT NULL, " +
                        "`colorHex` TEXT NOT NULL, `position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`listId`) REFERENCES `simple_lists`(`id`) ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_field_definitions_listId` ON `list_field_definitions` (`listId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `list_item_field_values` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`itemId` INTEGER NOT NULL, `fieldId` INTEGER NOT NULL, `value` TEXT NOT NULL, " +
                        "FOREIGN KEY(`itemId`) REFERENCES `simple_list_items`(`id`) ON DELETE CASCADE, " +
                        "FOREIGN KEY(`fieldId`) REFERENCES `list_field_definitions`(`id`) ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_item_field_values_itemId` ON `list_item_field_values` (`itemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_list_item_field_values_fieldId` ON `list_item_field_values` (`fieldId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo_tasks ADD COLUMN recurrence INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `link_previews` (" +
                        "`url` TEXT NOT NULL PRIMARY KEY, `title` TEXT, `imageUrl` TEXT, " +
                        "`fetchedAt` INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_app.db"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
                ).build().also { instance = it }
            }
    }
}
