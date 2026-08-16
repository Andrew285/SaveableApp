package com.rainyday.saveableapp

import android.app.Application
import com.rainyday.saveableapp.di.AppContainer

class SaveableApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
