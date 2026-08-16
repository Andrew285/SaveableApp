package com.rainyday.saveableapp.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): Int = priority.ordinal

    @TypeConverter
    fun toPriority(value: Int): Priority = Priority.entries.getOrElse(value) { Priority.MEDIUM }
}
