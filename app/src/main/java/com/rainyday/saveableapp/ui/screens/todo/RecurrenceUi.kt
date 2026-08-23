package com.rainyday.saveableapp.ui.screens.todo

import com.rainyday.saveableapp.data.local.RecurrenceRule

fun RecurrenceRule.label(): String = when (this) {
    RecurrenceRule.NONE -> "None"
    RecurrenceRule.DAILY -> "Daily"
    RecurrenceRule.WEEKLY -> "Weekly"
    RecurrenceRule.MONTHLY -> "Monthly"
    RecurrenceRule.YEARLY -> "Yearly"
}
