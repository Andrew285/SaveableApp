package com.rainyday.saveableapp.ui.screens.todo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.RecurrenceRule

@Composable
fun RecurrenceRule.label(): String = stringResource(
    when (this) {
        RecurrenceRule.NONE -> R.string.recurrence_none
        RecurrenceRule.DAILY -> R.string.recurrence_daily
        RecurrenceRule.WEEKLY -> R.string.recurrence_weekly
        RecurrenceRule.MONTHLY -> R.string.recurrence_monthly
        RecurrenceRule.YEARLY -> R.string.recurrence_yearly
    }
)
