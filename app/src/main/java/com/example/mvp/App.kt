package com.example.mvp

import android.app.Application
import android.util.Log

class App : Application() {

    companion object {
        private const val TAG = "MVPApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application initialization started")
        Log.d(TAG, "Using simplified App initialization")
    }
}