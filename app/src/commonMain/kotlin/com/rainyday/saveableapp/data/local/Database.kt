package com.rainyday.saveableapp.data.local

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.rainyday.saveableapp.db.AppDatabase
import com.rainyday.saveableapp.db.Todo_tasks

private val priorityAdapter = object : ColumnAdapter<Priority, Long> {
    override fun decode(databaseValue: Long): Priority =
        Priority.entries.getOrElse(databaseValue.toInt()) { Priority.MEDIUM }

    override fun encode(value: Priority): Long = value.ordinal.toLong()
}

fun createAppDatabase(driver: SqlDriver): AppDatabase =
    AppDatabase(driver = driver, todo_tasksAdapter = Todo_tasks.Adapter(priorityAdapter = priorityAdapter))
