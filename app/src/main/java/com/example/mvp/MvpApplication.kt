package com.example.mvp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import com.parse.Parse

@HiltAndroidApp
class MvpApplication : Application() {
    companion object {
        private const val TAG = "MvpApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate: Initializing app components")
        // Initialize Parse
        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build()
        )
        Log.d(TAG, "Parse initialized")
    }
}
