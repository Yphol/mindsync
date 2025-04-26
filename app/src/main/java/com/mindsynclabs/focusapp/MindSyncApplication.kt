package com.mindsynclabs.focusapp

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MindSyncApplication : Application() {
    private val TAG = "MindSyncApplication"

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d(TAG, "Initializing MindSync application")
            setupLogging()
            // Add any other initialization code here
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }

    private fun setupLogging() {
        try {
            Log.d(TAG, "Setting up logging")
            // Additional logging setup if needed
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up logging", e)
        }
    }
} 