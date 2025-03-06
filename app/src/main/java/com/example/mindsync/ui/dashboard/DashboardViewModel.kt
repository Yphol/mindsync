package com.example.mindsync.ui.dashboard

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindsync.data.model.*
import com.example.mindsync.data.repository.MockAnalyticsRepository
import com.example.mindsync.service.TimerService
import com.example.mindsync.service.TimerUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val analyticsRepository: MockAnalyticsRepository
) : AndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager
    private val usageStatsManager: UsageStatsManager = application.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val TAG = "DashboardViewModel"

    private val _todaysUsageData = MutableLiveData<List<AppUsageData>>()
    val todaysUsageData: LiveData<List<AppUsageData>> = _todaysUsageData

    private val _activeRestrictions = MutableLiveData<List<AppRestriction>>()
    val activeRestrictions: LiveData<List<AppRestriction>> = _activeRestrictions

    private val _activeLimits = MutableLiveData<List<AppTimeLimit>>()
    val activeLimits: LiveData<List<AppTimeLimit>> = _activeLimits

    private val _restrictionTimes = MutableLiveData<Map<String, Long>>()
    val restrictionTimes: LiveData<Map<String, Long>> = _restrictionTimes

    private val _focusTimeRemaining = MutableLiveData<Long>()
    val focusTimeRemaining: LiveData<Long> = _focusTimeRemaining

    private val _restrictionExpiredEvent = MutableSharedFlow<String>()
    val restrictionExpiredEvent: SharedFlow<String> = _restrictionExpiredEvent

    private var timerService: TimerService? = null
    private var updateJob: Job? = null
    private var boundToService = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is TimerService.TimerBinder) {
                timerService = service.getService()
                boundToService = true
                syncWithService()
                observeTimerUpdates()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            boundToService = false
            updateJob?.cancel()
        }
    }

    init {
        loadTodaysUsage()
        bindToService()
    }

    private fun bindToService() {
        try {
            // Try to get existing service instance first
            timerService = TimerService.getInstance()
            if (timerService != null) {
                boundToService = true
                syncWithService()
                observeTimerUpdates()
            } else {
                // Start and bind to service if no instance exists
                val context = getApplication<Application>()
                val intent = Intent(context, TimerService::class.java)
                TimerService.startService(context) // Ensure service is started
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error binding to service", e)
        }
    }

    private fun syncWithService() {
        try {
            timerService?.let { service ->
                // Sync active restrictions
                _activeRestrictions.value = service.getCurrentRestrictions()
                
                // Sync focus timer
                service.getFocusEndTime()?.let { endTime ->
                    val remaining = endTime - System.currentTimeMillis()
                    if (remaining > 0 && service.isFocusTimerActive()) {
                        _focusTimeRemaining.value = remaining
                    }
                }

                // Initialize restriction times
                val times = mutableMapOf<String, Long>()
                service.getCurrentRestrictions().forEach { restriction ->
                    if (!restriction.isForever && restriction.endTime != null) {
                        val remaining = restriction.endTime.time - System.currentTimeMillis()
                        if (remaining > 0) {
                            times[restriction.packageName] = remaining
                        }
                    }
                }
                _restrictionTimes.value = times
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error syncing with service", e)
        }
    }

    private fun observeTimerUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            timerService?.timerUpdates?.collect { update ->
                when (update) {
                    is TimerUpdate.RestrictionTick -> {
                        // Update only the time map
                        val currentTimes = _restrictionTimes.value.orEmpty().toMutableMap()
                        currentTimes[update.packageName] = update.remainingMillis
                        _restrictionTimes.value = currentTimes
                    }
                    is TimerUpdate.RestrictionExpired -> {
                        removeRestriction(update.packageName)
                        _restrictionExpiredEvent.emit("${update.packageName} restriction has expired")
                    }
                    is TimerUpdate.FocusTimerTick -> {
                        _focusTimeRemaining.value = update.remainingMillis
                    }
                    is TimerUpdate.FocusTimerFinished -> {
                        _focusTimeRemaining.value = 0
                    }
                    is TimerUpdate.ServiceStateRestored -> {
                        // Sync with service state when restored
                        syncWithService()
                    }
                }
            }
        }
    }

    private fun loadTodaysUsage() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val startDate = calendar.time
            val data = analyticsRepository.getSocialMediaUsage(startDate, endDate)
            _todaysUsageData.value = data
        }
    }

    fun addRestriction(appInfo: AppInfo, endTime: Date?, isForever: Boolean) {
        // Implementation will be added later
    }

    fun removeRestriction(packageName: String) {
        // Implementation will be added later
    }

    fun startFocusTimer(durationMinutes: Int) {
        timerService?.startFocusTimer(durationMinutes)
    }

    fun stopFocusTimer() {
        timerService?.stopFocusTimer()
        _focusTimeRemaining.value = 0
    }

    fun setAppTimeLimit(packageName: String, limitInMillis: Long) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val newLimit = AppTimeLimit(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                appIcon = packageManager.getApplicationIcon(appInfo),
                timeLimit = limitInMillis,
                timeUsed = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
                    System.currentTimeMillis()
                ).find { stat -> stat.packageName == packageName }?.totalTimeInForeground ?: 0L
            )

            val currentLimits = _activeLimits.value?.toMutableList() ?: mutableListOf()
            val existingIndex = currentLimits.indexOfFirst { it.packageName == packageName }
            
            if (existingIndex != -1) {
                currentLimits[existingIndex] = newLimit
            } else {
                currentLimits.add(newLimit)
            }
            
            _activeLimits.value = currentLimits
        } catch (e: Exception) {
            Log.e(TAG, "Error setting app time limit", e)
        }
    }

    override fun onCleared() {
        try {
            super.onCleared()
            if (boundToService) {
                getApplication<Application>().unbindService(serviceConnection)
                boundToService = false
            }
            updateJob?.cancel()
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error in onCleared", e)
        }
    }
} 