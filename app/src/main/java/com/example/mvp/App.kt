package com.example.mvp

import android.app.Application
import com.example.mvp.data.models.Feedback
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.ProductFeedback
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.google.firebase.FirebaseApp
import com.parse.Parse
import com.parse.ParseObject

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable Parse Local Datastore
        Parse.enableLocalDatastore(this)

        // Register Parse subclasses
        ParseObject.registerSubclass(User::class.java)
        ParseObject.registerSubclass(ProductListing::class.java)
        ParseObject.registerSubclass(Order::class.java)
        ParseObject.registerSubclass(Media::class.java)
        ParseObject.registerSubclass(Feedback::class.java)
        ParseObject.registerSubclass(ProductFeedback::class.java)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Parse
        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .enableLocalDataStore()
                .build()
        )
    }
}