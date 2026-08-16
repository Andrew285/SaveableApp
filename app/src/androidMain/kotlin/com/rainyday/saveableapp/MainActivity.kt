package com.rainyday.saveableapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import com.rainyday.saveableapp.ui.LocalAppContainer
import com.rainyday.saveableapp.ui.TodoApp

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SaveableApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                TodoApp()
            }
        }
    }
}
