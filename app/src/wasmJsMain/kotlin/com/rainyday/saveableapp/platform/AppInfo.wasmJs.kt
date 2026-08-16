package com.rainyday.saveableapp.platform

// Kept in sync manually with defaultConfig.versionName in app/build.gradle.kts — there's no
// Android-Gradle-style BuildConfig generation available for the wasmJs target.
actual object AppInfo {
    actual val version: String = "1.0"
}
