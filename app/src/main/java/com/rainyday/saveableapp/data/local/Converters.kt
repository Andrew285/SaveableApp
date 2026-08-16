package com.rainyday.saveableapp.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): Int = priority.ordinal

    @TypeConverter
    fun toPriority(value: Int): Priority = Priority.entries.getOrElse(value) { Priority.MEDIUM }

    @TypeConverter
    fun fromFieldType(type: FieldType): Int = type.ordinal

    @TypeConverter
    fun toFieldType(value: Int): FieldType = FieldType.entries.getOrElse(value) { FieldType.TEXT }
}
