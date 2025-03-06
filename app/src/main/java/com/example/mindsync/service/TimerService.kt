package com.example.mindsync.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mindsync.R
import com.example.mindsync.data.model.AppRestriction
import com.example.mindsync.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AndroidEntryPoint
class TimerService : Service() {
    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var alarmManager: AlarmManager
    private lateinit var prefs: SharedPreferences
    private var timerJob: Job? = null

    private val _timerUpdates = MutableSharedFlow<TimerUpdate>(replay = 1, extraBufferCapacity = 1)
    val timerUpdates: SharedFlow<TimerUpdate> = _timerUpdates

    private val activeRestrictions = mutableMapOf<String, AppRestriction>()
    private var focusEndTime: Long? = null
    private var focusTimerActive = false

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "timer_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val PREFS_NAME = "timer_service_prefs"
        private const val FOCUS_TIMER_KEY = "focus_timer"
        private const val FOCUS_TIMER_REQUEST_CODE = 1000
        private const val RESTRICTION_REQUEST_CODE_BASE = 2000

        const val ACTION_FOCUS_TIMER_FINISHED = "com.example.mindsync.FOCUS_TIMER_FINISHED"
        const val ACTION_RESTRICTION_EXPIRED = "com.example.mindsync.RESTRICTION_EXPIRED"
        const val EXTRA_PACKAGE_NAME = "package_name"

        @Volatile
        private var INSTANCE: TimerService? = null

        fun getInstance(): TimerService? = INSTANCE

        fun startService(context: Context) {
            val startIntent = Intent(context, TimerService::class.java)
            context.startForegroundService(startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, TimerService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Timer Service Running"))
        restoreTimerStates()
        startTimerUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure we emit the current state when the service starts
        serviceScope.launch {
            focusEndTime?.let { endTime ->
                val remaining = endTime - System.currentTimeMillis()
                if (remaining > 0) {
                    _timerUpdates.emit(TimerUpdate.FocusTimerTick(remaining))
                }
            }
            // Also emit current restrictions state
            activeRestrictions.forEach { (packageName, restriction) ->
                if (!restriction.isForever && restriction.endTime != null) {
                    val remaining = restriction.endTime.time - System.currentTimeMillis()
                    if (remaining > 0) {
                        _timerUpdates.emit(TimerUpdate.RestrictionTick(packageName, remaining))
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun getCurrentRestrictions(): List<AppRestriction> = activeRestrictions.values.toList()
    
    fun getFocusEndTime(): Long? = focusEndTime

    fun isFocusTimerActive(): Boolean = focusTimerActive

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Timer Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MindSync Timer")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startTimerUpdates() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                try {
                    val currentTime = System.currentTimeMillis()
                    
                    // Update focus timer
                    focusEndTime?.let { endTime ->
                        if (currentTime >= endTime) {
                            focusEndTime = null
                            focusTimerActive = false
                            _timerUpdates.emit(TimerUpdate.FocusTimerFinished)
                            updateNotification()
                        } else {
                            val remaining = endTime - currentTime
                            _timerUpdates.emit(TimerUpdate.FocusTimerTick(remaining))
                        }
                    }

                    // Update app restrictions
                    val expiredRestrictions = mutableListOf<String>()
                    activeRestrictions.forEach { (packageName, restriction) ->
                        if (!restriction.isForever && restriction.endTime != null) {
                            val remaining = restriction.endTime.time - currentTime
                            if (remaining <= 0) {
                                expiredRestrictions.add(packageName)
                                _timerUpdates.emit(TimerUpdate.RestrictionExpired(packageName))
                            } else {
                                _timerUpdates.emit(TimerUpdate.RestrictionTick(packageName, remaining))
                            }
                        }
                    }

                    if (expiredRestrictions.isNotEmpty()) {
                        expiredRestrictions.forEach { activeRestrictions.remove(it) }
                        updateNotification()
                    }
                } catch (e: Exception) {
                    Log.e("TimerService", "Error in timer update loop", e)
                }

                delay(1000L)
            }
        }
    }

    private fun restoreTimerStates() {
        // Restore focus timer state
        val savedFocusEndTime = prefs.getLong(FOCUS_TIMER_KEY, -1)
        if (savedFocusEndTime > System.currentTimeMillis()) {
            focusEndTime = savedFocusEndTime
            focusTimerActive = true
            scheduleFocusTimerAlarm(savedFocusEndTime)
        }

        // Restore restrictions (you'll need to implement persistence for restrictions)
        serviceScope.launch {
            _timerUpdates.emit(TimerUpdate.ServiceStateRestored)
        }
    }

    private fun scheduleFocusTimerAlarm(endTime: Long) {
        val intent = Intent(this, TimerBroadcastReceiver::class.java).apply {
            action = ACTION_FOCUS_TIMER_FINISHED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            FOCUS_TIMER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            endTime,
            pendingIntent
        )
    }

    private fun scheduleRestrictionAlarm(packageName: String, endTime: Long) {
        val intent = Intent(this, TimerBroadcastReceiver::class.java).apply {
            action = ACTION_RESTRICTION_EXPIRED
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        val requestCode = RESTRICTION_REQUEST_CODE_BASE + packageName.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            endTime,
            pendingIntent
        )
    }

    fun startFocusTimer(durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + durationMinutes * 60 * 1000
        focusEndTime = endTime
        focusTimerActive = true
        
        // Save state
        prefs.edit().putLong(FOCUS_TIMER_KEY, endTime).apply()
        
        // Schedule alarm
        scheduleFocusTimerAlarm(endTime)
        
        // Emit initial state
        serviceScope.launch {
            _timerUpdates.emit(TimerUpdate.FocusTimerTick(durationMinutes * 60 * 1000L))
        }
        updateNotification()
    }

    fun stopFocusTimer() {
        focusEndTime = null
        focusTimerActive = false
        
        // Clear saved state
        prefs.edit().remove(FOCUS_TIMER_KEY).apply()
        
        // Cancel alarm
        val intent = Intent(this, TimerBroadcastReceiver::class.java).apply {
            action = ACTION_FOCUS_TIMER_FINISHED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            FOCUS_TIMER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        serviceScope.launch {
            _timerUpdates.emit(TimerUpdate.FocusTimerFinished)
        }
        updateNotification()
    }

    fun addRestriction(restriction: AppRestriction) {
        try {
            activeRestrictions[restriction.packageName] = restriction
            
            if (!restriction.isForever && restriction.endTime != null) {
                scheduleRestrictionAlarm(restriction.packageName, restriction.endTime.time)
                // Emit initial state immediately
                serviceScope.launch {
                    val remaining = restriction.endTime.time - System.currentTimeMillis()
                    if (remaining > 0) {
                        _timerUpdates.emit(TimerUpdate.RestrictionTick(restriction.packageName, remaining))
                    }
                }
            }
            
            updateNotification()
        } catch (e: Exception) {
            Log.e("TimerService", "Error adding restriction", e)
        }
    }

    fun removeRestriction(packageName: String) {
        activeRestrictions.remove(packageName)
        
        // Cancel alarm if exists
        val intent = Intent(this, TimerBroadcastReceiver::class.java).apply {
            action = ACTION_RESTRICTION_EXPIRED
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        val requestCode = RESTRICTION_REQUEST_CODE_BASE + packageName.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        updateNotification()
    }

    private fun updateNotification() {
        val content = buildString {
            if (focusTimerActive) {
                append("Focus Mode Active")
            }
            if (activeRestrictions.isNotEmpty()) {
                if (isNotEmpty()) append(" | ")
                append("${activeRestrictions.size} Active Restrictions")
            }
        }
        val notification = createNotification(content.ifEmpty { "Timer Service Running" })
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null
        timerJob?.cancel()
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Ensure service keeps running even if app is removed from recent tasks
        val restartServiceIntent = Intent(applicationContext, TimerService::class.java)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
    }
}

class TimerBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = TimerService.getInstance()
        
        when (intent.action) {
            TimerService.ACTION_FOCUS_TIMER_FINISHED -> {
                service?.stopFocusTimer()
            }
            TimerService.ACTION_RESTRICTION_EXPIRED -> {
                val packageName = intent.getStringExtra(TimerService.EXTRA_PACKAGE_NAME)
                if (packageName != null) {
                    service?.removeRestriction(packageName)
                }
            }
        }
    }
}

sealed class TimerUpdate {
    data class FocusTimerTick(val remainingMillis: Long) : TimerUpdate()
    object FocusTimerFinished : TimerUpdate()
    data class RestrictionTick(val packageName: String, val remainingMillis: Long) : TimerUpdate()
    data class RestrictionExpired(val packageName: String) : TimerUpdate()
    object ServiceStateRestored : TimerUpdate()
} 