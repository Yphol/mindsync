package com.mindsynclabs.focusapp.data.manager

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.mindsynclabs.focusapp.data.model.AppRestriction
import com.mindsynclabs.focusapp.service.AppBlockerService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app access restrictions
 */
@Singleton
class AppAccessManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "AppAccessManager"
    private val restrictedApps = mutableMapOf<String, AppRestriction>()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "app_restrictions"
        private const val KEY_RESTRICTED_APPS = "restricted_apps"
    }
    
    init {
        loadRestrictionsFromStorage()
    }

    /**
     * Add an app restriction
     */
    fun addRestriction(restriction: AppRestriction) {
        Log.d(TAG, "Adding restriction for ${restriction.packageName}")
        restrictedApps[restriction.packageName] = restriction
        saveRestrictionsToStorage()
        
        // Start the app blocker service if not already running
        if (!isAccessibilityServiceEnabled()) {
            Log.d(TAG, "Accessibility service not enabled, prompting user")
            promptEnableAccessibilityService()
        }
    }

    /**
     * Remove an app restriction
     */
    fun removeRestriction(packageName: String) {
        Log.d(TAG, "Removing restriction for $packageName")
        restrictedApps.remove(packageName)
        saveRestrictionsToStorage()
    }

    /**
     * Get all active restrictions
     */
    fun getActiveRestrictions(): List<AppRestriction> {
        // Clean up expired restrictions first
        cleanupExpiredRestrictions()
        return restrictedApps.values.toList()
    }

    /**
     * Check if an app is restricted
     */
    fun isAppRestricted(packageName: String): Boolean {
        // First check if the restriction exists
        val restriction = restrictedApps[packageName] ?: return false
        
        // If it's a temporary restriction, check if it's expired
        if (!restriction.isForever && restriction.endTime != null) {
            val now = System.currentTimeMillis()
            if (now > restriction.endTime.time) {
                // Restriction has expired, remove it
                Log.d(TAG, "Restriction for $packageName has expired, removing it")
                removeRestriction(packageName)
                return false
            }
        }
        
        Log.d(TAG, "App $packageName is restricted")
        return true
    }
    
    /**
     * Clean up expired restrictions
     */
    private fun cleanupExpiredRestrictions() {
        val now = System.currentTimeMillis()
        val expiredPackages = mutableListOf<String>()
        
        restrictedApps.forEach { (packageName, restriction) ->
            if (!restriction.isForever && restriction.endTime != null && now > restriction.endTime.time) {
                Log.d(TAG, "Found expired restriction for $packageName")
                expiredPackages.add(packageName)
            }
        }
        
        if (expiredPackages.isNotEmpty()) {
            Log.d(TAG, "Removing ${expiredPackages.size} expired restrictions")
            expiredPackages.forEach { packageName ->
                restrictedApps.remove(packageName)
            }
            saveRestrictionsToStorage()
        }
    }
    
    /**
     * Save restrictions to storage
     */
    private fun saveRestrictionsToStorage() {
        try {
            // Convert restrictions to a format that can be saved
            val restrictionsToSave = restrictedApps.values.map { restriction ->
                RestrictedAppInfo(
                    packageName = restriction.packageName,
                    appName = restriction.appName,
                    startTime = restriction.startTime.time,
                    endTime = restriction.endTime?.time,
                    isForever = restriction.isForever
                )
            }
            
            val json = gson.toJson(restrictionsToSave)
            prefs.edit().putString(KEY_RESTRICTED_APPS, json).apply()
            Log.d(TAG, "Saved ${restrictionsToSave.size} restrictions to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving restrictions to storage", e)
        }
    }
    
    /**
     * Load restrictions from storage
     */
    private fun loadRestrictionsFromStorage() {
        try {
            val json = prefs.getString(KEY_RESTRICTED_APPS, null) ?: return
            val type = object : TypeToken<List<RestrictedAppInfo>>() {}.type
            val savedRestrictions: List<RestrictedAppInfo> = gson.fromJson(json, type)
            
            savedRestrictions.forEach { savedInfo ->
                try {
                    val packageManager = context.packageManager
                    val appInfo = packageManager.getApplicationInfo(savedInfo.packageName, 0)
                    val icon = packageManager.getApplicationIcon(appInfo)
                    
                    val restriction = AppRestriction(
                        packageName = savedInfo.packageName,
                        appName = savedInfo.appName,
                        icon = icon,
                        startTime = Date(savedInfo.startTime),
                        endTime = savedInfo.endTime?.let { Date(it) },
                        isForever = savedInfo.isForever,
                        remainingTime = savedInfo.endTime?.let { it - System.currentTimeMillis() } ?: 0
                    )
                    
                    // Only add if not expired
                    if (savedInfo.isForever || (savedInfo.endTime != null && savedInfo.endTime > System.currentTimeMillis())) {
                        restrictedApps[savedInfo.packageName] = restriction
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading restriction for ${savedInfo.packageName}", e)
                }
            }
            
            Log.d(TAG, "Loaded ${restrictedApps.size} restrictions from storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading restrictions from storage", e)
        }
    }

    /**
     * Check if the accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(TAG, "Error finding accessibility setting", e)
            return false
        }

        if (accessibilityEnabled == 1) {
            val serviceString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return serviceString?.contains("${context.packageName}/${AppBlockerService::class.java.name}") == true
        }
        return false
    }

    /**
     * Prompt the user to enable the accessibility service
     */
    fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Check if the app has permission to display overlays
     */
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Request permission to draw overlays
     */
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * Data class for storing restriction info
     */
    private data class RestrictedAppInfo(
        val packageName: String,
        val appName: String,
        val startTime: Long,
        val endTime: Long?,
        val isForever: Boolean
    )
} 