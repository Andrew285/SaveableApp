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

    @TypeConverter
    fun fromRecurrenceRule(rule: RecurrenceRule): Int = rule.ordinal

    @TypeConverter
    fun toRecurrenceRule(value: Int): RecurrenceRule = RecurrenceRule.entries.getOrElse(value) { RecurrenceRule.NONE }

    @TypeConverter
    fun fromSyncOperation(operation: SyncOperation): Int = operation.ordinal

    @TypeConverter
    fun toSyncOperation(value: Int): SyncOperation = SyncOperation.entries.getOrElse(value) { SyncOperation.UPSERT }

    @TypeConverter
    fun fromSyncEntityType(type: SyncEntityType): Int = type.ordinal

    @TypeConverter
    fun toSyncEntityType(value: Int): SyncEntityType = SyncEntityType.entries.getOrElse(value) { SyncEntityType.TODO_TASK }
    // Note: SyncEntityType ordinals are persisted (via this converter). Only ever append new entries.
}
