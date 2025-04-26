package com.mindsynclabs.focusapp.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.mindsynclabs.focusapp.data.model.AppUsageData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "UsageStatsRepository"
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    // Custom data class to store event information
    private data class UsageEventData(
        val packageName: String,
        val className: String?,
        val eventType: Int,
        val timeStamp: Long
    )

    /**
     * Get app usage data for the specified time range
     */
    fun getAppUsageData(startTime: Long, endTime: Long): List<AppUsageData> {
        if (!hasUsageStatsPermission()) {
            Log.e(TAG, "No usage stats permission")
            return emptyList()
        }

        val appUsageMap = mutableMapOf<String, MutableList<UsageEventData>>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ (Android 10+) - Use queryEvents with time range
            Log.d(TAG, "Using API 29+ method to collect events")
            collectEventsApi29Plus(appUsageMap, startTime, endTime)
        } else {
            // API 26-28 (Android 8.0-9.0) - Use queryUsageStats
            Log.d(TAG, "Using API 26-28 method to collect events")
            collectEventsCompat(appUsageMap, startTime, endTime)
        }

        // Process events to calculate usage time
        val result = processUsageEvents(appUsageMap, startTime, endTime)
        Log.d(TAG, "Retrieved ${result.size} apps with usage data")
        
        // Log the top 5 apps by usage time
        result.take(5).forEach { app ->
            Log.d(TAG, "App: ${app.appName}, Usage: ${app.timeSpent / (60 * 1000)} minutes")
        }
        
        return result
    }

    /**
     * Collect events for API level 29+ (Android 10+)
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun collectEventsApi29Plus(
        appUsageMap: MutableMap<String, MutableList<UsageEventData>>,
        startTime: Long,
        endTime: Long
    ) {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        
        // Collect all events
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            
            // We're only interested in MOVE_TO_FOREGROUND and MOVE_TO_BACKGROUND events
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || 
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                
                val packageName = event.packageName
                if (!appUsageMap.containsKey(packageName)) {
                    appUsageMap[packageName] = mutableListOf()
                }
                
                // Create a copy of the event data using our custom data class
                val eventData = UsageEventData(
                    packageName = event.packageName,
                    className = event.className,
                    eventType = event.eventType,
                    timeStamp = event.timeStamp
                )
                appUsageMap[packageName]?.add(eventData)
            }
        }
    }

    /**
     * Collect events for API levels below 29 (Android 8.0-9.0)
     * This is a simplified approach that uses queryUsageStats instead of queryEvents
     */
    private fun collectEventsCompat(
        appUsageMap: MutableMap<String, MutableList<UsageEventData>>,
        startTime: Long,
        endTime: Long
    ) {
        // For older Android versions, we'll use queryUsageStats
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        // Process each app's usage stats
        for (stat in stats) {
            val packageName = stat.packageName
            
            // Skip if no usage
            if (stat.totalTimeInForeground <= 0) continue
            
            if (!appUsageMap.containsKey(packageName)) {
                appUsageMap[packageName] = mutableListOf()
            }
            
            // Create synthetic MOVE_TO_FOREGROUND event at first usage time
            val firstEventData = UsageEventData(
                packageName = packageName,
                className = null,
                eventType = UsageEvents.Event.MOVE_TO_FOREGROUND,
                timeStamp = stat.firstTimeStamp
            )
            appUsageMap[packageName]?.add(firstEventData)
            
            // Create synthetic MOVE_TO_BACKGROUND event at last usage time
            val lastEventData = UsageEventData(
                packageName = packageName,
                className = null,
                eventType = UsageEvents.Event.MOVE_TO_BACKGROUND,
                timeStamp = stat.lastTimeStamp
            )
            appUsageMap[packageName]?.add(lastEventData)
        }
    }

    /**
     * Process usage events to calculate app usage time
     */
    private fun processUsageEvents(
        appUsageMap: Map<String, List<UsageEventData>>,
        startTime: Long,
        endTime: Long
    ): List<AppUsageData> {
        val appUsageList = mutableListOf<AppUsageData>()
        
        for ((packageName, events) in appUsageMap) {
            if (events.size < 2) continue
            
            // Sort events by timestamp
            val sortedEvents = events.sortedBy { it.timeStamp }
            
            var totalTimeInForeground = 0L
            var lastForegroundEvent: UsageEventData? = null
            var sessionCount = 0
            
            for (event in sortedEvents) {
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        lastForegroundEvent = event
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (lastForegroundEvent != null) {
                            val usageTime = event.timeStamp - lastForegroundEvent.timeStamp
                            if (usageTime > 0) {
                                totalTimeInForeground += usageTime
                                sessionCount++
                            }
                            lastForegroundEvent = null
                        }
                    }
                }
            }
            
            // Handle case where app was in foreground at the end of the period
            if (lastForegroundEvent != null) {
                val usageTime = endTime - lastForegroundEvent.timeStamp
                if (usageTime > 0) {
                    totalTimeInForeground += usageTime
                    sessionCount++
                }
            }
            
            // Only include apps with actual usage
            if (totalTimeInForeground > 0) {
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    
                    appUsageList.add(
                        AppUsageData(
                            appName = appName,
                            packageName = packageName,
                            timeSpent = totalTimeInForeground,
                            date = Date(startTime),
                            overLimitTime = 0, // This would need to be calculated based on your app's limits
                            sessionCount = sessionCount
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e(TAG, "Package not found: $packageName", e)
                }
            }
        }
        
        return appUsageList.sortedByDescending { it.timeSpent }
    }

    /**
     * Check if the app has usage stats permission
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /**
     * Get hourly usage data for today
     */
    fun getHourlyUsageData(): Map<Int, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        // Set start time to beginning of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        val hourlyUsage = mutableMapOf<Int, Long>()
        for (hour in 0..23) {
            hourlyUsage[hour] = 0L
        }
        
        val appUsageData = getAppUsageData(startTime, endTime)
        
        // Process each app's usage to get hourly breakdown
        // This is a simplified approach - for more accurate hourly data,
        // you would need to process individual events by hour
        for (appUsage in appUsageData) {
            val appStartTime = appUsage.date.time
            val calendar = Calendar.getInstance().apply { timeInMillis = appStartTime }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyUsage[hour] = (hourlyUsage[hour] ?: 0L) + appUsage.timeSpent
        }
        
        return hourlyUsage
    }

    /**
     * Get daily usage data for the last week
     */
    fun getDailyUsageData(days: Int): Map<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        // Set start time to beginning of period
        calendar.add(Calendar.DAY_OF_YEAR, -days + 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        val dailyUsage = mutableMapOf<Long, Long>()
        
        // Initialize map with all days
        var currentDay = startTime
        while (currentDay < endTime) {
            dailyUsage[currentDay] = 0L
            calendar.timeInMillis = currentDay
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            currentDay = calendar.timeInMillis
        }
        
        // Get all app usage for the period
        val appUsageData = getAppUsageData(startTime, endTime)
        
        // Group by day
        for (appUsage in appUsageData) {
            val appStartTime = appUsage.date.time
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = appStartTime
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            dailyUsage[dayStart] = (dailyUsage[dayStart] ?: 0L) + appUsage.timeSpent
        }
        
        return dailyUsage
    }

    /**
     * Get weekly usage data for the last month
     */
    fun getWeeklyUsageData(weeks: Int): Map<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        // Set start time to beginning of period
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks + 1)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        val weeklyUsage = mutableMapOf<Long, Long>()
        
        // Initialize map with all weeks
        var currentWeekStart = startTime
        while (currentWeekStart < endTime) {
            weeklyUsage[currentWeekStart] = 0L
            calendar.timeInMillis = currentWeekStart
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            currentWeekStart = calendar.timeInMillis
        }
        
        // Get all app usage for the period
        val appUsageData = getAppUsageData(startTime, endTime)
        
        // Group by week
        for (appUsage in appUsageData) {
            val appStartTime = appUsage.date.time
            val weekStart = Calendar.getInstance().apply {
                timeInMillis = appStartTime
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            weeklyUsage[weekStart] = (weeklyUsage[weekStart] ?: 0L) + appUsage.timeSpent
        }
        
        return weeklyUsage
    }
} 