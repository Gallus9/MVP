package com.example.mvp

import android.app.Application
import android.util.Log
import com.example.mvp.data.models.Feedback
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.ProductFeedback
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.example.mvp.data.models.Post
import com.example.mvp.data.models.Comment
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.parse.Parse
import com.parse.ParseACL
import com.parse.ParseObject
import com.parse.ParseUser

class App : Application() {

    companion object {
        private const val TAG = "RoosterEnthusiastsApp"
    }

    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Initializing application")

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            // Enable Firebase Realtime Database persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }

        // Register Parse subclasses
        try {
            ParseObject.registerSubclass(User::class.java)
            ParseObject.registerSubclass(ProductListing::class.java)
            ParseObject.registerSubclass(Order::class.java)
            ParseObject.registerSubclass(Media::class.java)
            ParseObject.registerSubclass(Feedback::class.java)
            ParseObject.registerSubclass(ProductFeedback::class.java)
            ParseObject.registerSubclass(Post::class.java)
            ParseObject.registerSubclass(Comment::class.java)
            Log.d(TAG, "Parse models registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering Parse models", e)
        }

        // Initialize Parse with local datastore
        try {
            // Enable Parse Local Datastore first
            Parse.enableLocalDatastore(this)
            
            // Initialize Parse
            Parse.initialize(
                Parse.Configuration.Builder(this)
                    .applicationId(getString(R.string.back4app_app_id))
                    .clientKey(getString(R.string.back4app_client_key))
                    .server(getString(R.string.back4app_server_url))
                    .enableLocalDataStore()
                    .build()
            )
            
            // Set default ACLs
            val defaultACL = ParseACL()
            defaultACL.publicReadAccess = true
            ParseACL.setDefaultACL(defaultACL, true)
            
            Log.d(TAG, "Parse initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Parse", e)
        }
        
        // Check current session
        try {
            val currentUser = ParseUser.getCurrentUser()
            if (currentUser != null) {
                Log.d(TAG, "Current user session found: ${currentUser.username}")
            } else {
                Log.d(TAG, "No current user session found")
            }
            
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                Log.d(TAG, "Current Firebase user found: ${firebaseUser.email}")
            } else {
                Log.d(TAG, "No current Firebase user found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user sessions", e)
        }
    }
}