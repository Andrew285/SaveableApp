package com.rainyday.saveableapp

import android.app.Application
import com.rainyday.saveableapp.data.local.createAndroidDatabaseDriver
import com.rainyday.saveableapp.di.AppContainer
import com.russhwolf.settings.SharedPreferencesSettings

class SaveableApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val driver = createAndroidDatabaseDriver(this)
        val settings = SharedPreferencesSettings(getSharedPreferences("app_settings", MODE_PRIVATE))
        container = AppContainer(driver, settings)
    }
}
