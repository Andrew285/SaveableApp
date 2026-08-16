package com.rainyday.saveableapp.platform

import kotlinx.datetime.Clock

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
