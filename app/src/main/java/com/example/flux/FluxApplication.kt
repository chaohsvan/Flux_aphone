package com.example.flux

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FluxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
