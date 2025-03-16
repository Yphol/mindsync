package com.example.mindsync.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import com.example.mindsync.R
import com.example.mindsync.data.manager.AppAccessManager
import com.example.mindsync.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Accessibility service that detects and blocks restricted apps
 */
@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {
    private val TAG = "AppBlockerService"
    
    @Inject
    lateinit var appAccessManager: AppAccessManager
    
    private var blockingOverlay: View? = null
    private var windowManager: WindowManager? = null
    private var currentPackage: String? = null
    private var isOverlayShown = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockedTime = 0L
    private var lastBlockedPackage: String? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var monitoringRunnable: Runnable? = null
    
    companion object {
        private var INSTANCE: AppBlockerService? = null
        private const val OVERLAY_DELAY_MS = 50L
        private const val MONITOR_INTERVAL_MS = 500L
        private const val DEBOUNCE_TIME_MS = 1000L
        
        fun getInstance(): AppBlockerService? = INSTANCE
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        INSTANCE = this
        Log.d(TAG, "AppBlockerService connected")
        
        // Configure accessibility service with enhanced settings
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                          AccessibilityEvent.TYPE_WINDOWS_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL or
                           AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 50
        info.packageNames = null // Listen to all packages
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                     AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                     AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                     AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
        serviceInfo = info
        
        // Start continuous monitoring for restricted apps
        startMonitoring()
    }
    
    private fun startMonitoring() {
        monitoringRunnable = Runnable {
            try {
                // Check if current package is restricted and ensure overlay is shown
                val packageName = currentPackage
                if (packageName != null && appAccessManager.isAppRestricted(packageName) && !isOverlayShown) {
                    Log.d(TAG, "Monitor detected restricted app without overlay: $packageName")
                    showBlockingOverlay(packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in monitoring runnable", e)
            } finally {
                // Schedule next check
                monitoringRunnable?.let {
                    handler.postDelayed(it, MONITOR_INTERVAL_MS)
                }
            }
        }
        
        handler.postDelayed(monitoringRunnable!!, MONITOR_INTERVAL_MS)
    }
    
    private fun stopMonitoring() {
        monitoringRunnable?.let {
            handler.removeCallbacks(it)
            monitoringRunnable = null
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = event.packageName?.toString()
                if (packageName != null && packageName != "com.example.mindsync") {
                    Log.d(TAG, "App switched to: $packageName")
                    
                    // Check if this app is restricted
                    if (appAccessManager.isAppRestricted(packageName)) {
                        Log.d(TAG, "Blocking restricted app: $packageName")
                        
                        val currentTime = System.currentTimeMillis()
                        
                        // Prevent too frequent blocking of the same app (debounce)
                        if (packageName == lastBlockedPackage && 
                            currentTime - lastBlockedTime < DEBOUNCE_TIME_MS) {
                            Log.d(TAG, "Skipping duplicate block within debounce time")
                            return
                        }
                        
                        lastBlockedTime = currentTime
                        lastBlockedPackage = packageName
                        currentPackage = packageName
                        
                        // Show overlay immediately
                        showBlockingOverlay(packageName)
                        
                        // Perform a back action to try to exit the app
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        
                        // Schedule additional checks to ensure overlay remains visible
                        scheduleOverlayChecks(packageName)
                    } else if (packageName != currentPackage) {
                        // Only update current package and hide overlay if it's a different package
                        // and not a restricted app
                        currentPackage = packageName
                        hideBlockingOverlay()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }
    
    private fun scheduleOverlayChecks(packageName: String) {
        // Check after short delay
        handler.postDelayed({
            if (currentPackage == packageName && !isOverlayShown) {
                Log.d(TAG, "First check: Overlay not visible, reshowing")
                showBlockingOverlay(packageName)
            }
        }, 300)
        
        // Check again after longer delay
        handler.postDelayed({
            if (currentPackage == packageName && !isOverlayShown) {
                Log.d(TAG, "Second check: Overlay not visible, reshowing and going home")
                showBlockingOverlay(packageName)
                goHome()
            }
        }, 800)
        
        // Final check with home action
        handler.postDelayed({
            if (currentPackage == packageName) {
                Log.d(TAG, "Final check: Forcing home screen")
                goHome()
            }
        }, 1500)
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "AppBlockerService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null
        stopMonitoring()
        hideBlockingOverlay()
        Log.d(TAG, "AppBlockerService destroyed")
    }
    
    private fun goHome() {
        try {
            Log.d(TAG, "Going to home screen")
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            
            // Show the overlay again after going home to ensure it's visible
            handler.postDelayed({
                if (currentPackage != null && appAccessManager.isAppRestricted(currentPackage!!)) {
                    showBlockingOverlay(currentPackage!!)
                }
            }, 300)
        } catch (e: Exception) {
            Log.e(TAG, "Error going home", e)
        }
    }
    
    private fun showBlockingOverlay(packageName: String) {
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Cannot show overlay - permission not granted")
            return
        }
        
        try {
            // If we already have an overlay for this package, don't recreate it
            if (isOverlayShown && currentPackage == packageName) {
                Log.d(TAG, "Overlay already shown for $packageName")
                return
            }
            
            // Remove any existing overlay
            hideBlockingOverlay()
            
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            blockingOverlay = inflater.inflate(R.layout.overlay_app_blocked, null)
            
            // Set app name in the message
            val appName = try {
                packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
            } catch (e: Exception) {
                packageName
            }
            
            blockingOverlay?.findViewById<TextView>(R.id.tvBlockedMessage)?.text = 
                getString(R.string.app_blocked_message, appName)
            
            // Set up the "Go Home" button
            blockingOverlay?.findViewById<Button>(R.id.btnGoHome)?.setOnClickListener {
                goHome()
            }
            
            // Set up the "Open Dashboard" button
            blockingOverlay?.findViewById<Button>(R.id.btnOpenDashboard)?.setOnClickListener {
                val dashboardIntent = Intent(this, DashboardActivity::class.java)
                dashboardIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(dashboardIntent)
            }
            
            // Create improved window parameters - use highest priority overlay
            overlayParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            )
            
            overlayParams?.gravity = Gravity.CENTER
            windowManager?.addView(blockingOverlay, overlayParams)
            isOverlayShown = true
            
            // Update the parameters to make it touchable immediately
            handler.postDelayed({
                try {
                    if (blockingOverlay != null && windowManager != null && overlayParams != null) {
                        overlayParams?.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        
                        windowManager?.updateViewLayout(blockingOverlay, overlayParams)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating overlay parameters", e)
                }
            }, 50)
            
            Log.d(TAG, "Blocking overlay shown for $appName")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing blocking overlay", e)
        }
    }
    
    private fun hideBlockingOverlay() {
        try {
            if (blockingOverlay != null && windowManager != null) {
                windowManager?.removeView(blockingOverlay)
                blockingOverlay = null
                isOverlayShown = false
                Log.d(TAG, "Blocking overlay hidden")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding blocking overlay", e)
            // Reset state even if there was an error
            blockingOverlay = null
            isOverlayShown = false
        }
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "AppBlockerService unbound")
        stopMonitoring()
        hideBlockingOverlay()
        return super.onUnbind(intent)
    }
} 