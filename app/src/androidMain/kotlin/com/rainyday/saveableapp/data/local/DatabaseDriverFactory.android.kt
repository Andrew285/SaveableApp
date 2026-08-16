package com.rainyday.saveableapp.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.rainyday.saveableapp.db.AppDatabase

/**
 * Points at the same file name the old Room database used. For an existing install, this file
 * already exists with an identical schema but PRAGMA user_version = 0 (Room never touched it), so
 * SupportSQLiteOpenHelper calls onUpgrade(0, 1) rather than onCreate — we treat oldVersion == 0 as
 * "adopt the existing tables as-is" instead of running schema.migrate against a migration that
 * doesn't exist.
 */
fun createAndroidDatabaseDriver(context: Context): SqlDriver {
    val schema = AppDatabase.Schema
    return AndroidSqliteDriver(
        schema = schema,
        context = context.applicationContext,
        name = "todo_app.db",
        callback = object : AndroidSqliteDriver.Callback(schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db)
                db.setForeignKeyConstraintsEnabled(true)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion == 0) return
                super.onUpgrade(db, oldVersion, newVersion)
            }
        }
    )
}
