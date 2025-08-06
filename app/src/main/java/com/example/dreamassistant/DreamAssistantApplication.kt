package com.example.dreamassistant

import android.app.Application
import android.util.Log

/**
 * DreamAssistantApplication - Application class for Sister's Dream Assistant
 * Handles app-wide initialization
 */
class DreamAssistantApplication : Application() {

    companion object {
        private const val TAG = "DreamAssistantApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Dream Assistant Application started")
        Log.d(TAG, "💕 Sister's personalized AI companion initializing...")
    }
}