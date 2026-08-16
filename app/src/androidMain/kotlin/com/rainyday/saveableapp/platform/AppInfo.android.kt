package com.rainyday.saveableapp.platform

import com.rainyday.saveableapp.BuildConfig

actual object AppInfo {
    actual val version: String = BuildConfig.VERSION_NAME
}
