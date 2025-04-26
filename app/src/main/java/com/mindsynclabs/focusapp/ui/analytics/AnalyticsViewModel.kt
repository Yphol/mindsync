package com.mindsynclabs.focusapp.ui.analytics

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindsynclabs.focusapp.data.model.*
import com.mindsynclabs.focusapp.data.repository.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository
) : ViewModel() {

    private val _overallUsage = MutableLiveData<OverallUsage>()
    val overallUsage: LiveData<OverallUsage> = _overallUsage

    private val _appUsageBreakdown = MutableLiveData<List<AppUsageData>>()
    val appUsageBreakdown: LiveData<List<AppUsageData>> = _appUsageBreakdown

    private val _hourlyUsage = MutableLiveData<List<HourlyUsage>>()
    val hourlyUsage: LiveData<List<HourlyUsage>> = _hourlyUsage

    private val _weeklyTrends = MutableLiveData<List<DailyUsage>>()
    val weeklyTrends: LiveData<List<DailyUsage>> = _weeklyTrends

    private val _usageSpikes = MutableLiveData<List<UsageSpike>>()
    val usageSpikes: LiveData<List<UsageSpike>> = _usageSpikes

    private val _usagePatterns = MutableLiveData<UsagePatterns>()
    val usagePatterns: LiveData<UsagePatterns> = _usagePatterns

    private var currentTimeRange = TimeRangePreset.TODAY

    init {
        loadAnalytics(TimeRangePreset.TODAY)
    }

    fun setTimeRange(timeRange: TimeRangePreset) {
        if (currentTimeRange != timeRange) {
            currentTimeRange = timeRange
            loadAnalytics(timeRange)
        }
    }

    private fun loadAnalytics(timeRange: TimeRangePreset) {
        viewModelScope.launch {
            if (usageStatsRepository.hasUsageStatsPermission()) {
                loadRealUsageData(timeRange)
            } else {
                // Just initialize with empty data if no permission
                _appUsageBreakdown.value = emptyList()
                _hourlyUsage.value = emptyList()
                _weeklyTrends.value = emptyList()
                _usageSpikes.value = emptyList()
                _usagePatterns.value = UsagePatterns("", 0, emptyList(), emptyList())
                _overallUsage.value = OverallUsage(0, 0, emptyList())
            }
        }
    }
    
    private suspend fun loadRealUsageData(timeRange: TimeRangePreset) = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (timeRange) {
            TimeRangePreset.TODAY -> Unit // Start date is already set to today
            TimeRangePreset.LAST_7_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -6)
            TimeRangePreset.LAST_30_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -29)
            TimeRangePreset.LAST_90_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -89)
        }

        val startTime = calendar.timeInMillis
        
        Log.d("AnalyticsViewModel", "Loading real usage data for time range: $timeRange")
        Log.d("AnalyticsViewModel", "Start time: ${Date(startTime)}, End time: ${Date(endTime)}")
        
        // Get app usage data
        val appUsageData = usageStatsRepository.getAppUsageData(startTime, endTime)
        Log.d("AnalyticsViewModel", "Retrieved ${appUsageData.size} apps with usage data")
        
        // Process app usage breakdown first to ensure it's available
        processAppUsageBreakdown(appUsageData)
        
        // Process the data based on time range
        when (timeRange) {
            TimeRangePreset.TODAY -> {
                // Get hourly data for today
                val hourlyData = usageStatsRepository.getHourlyUsageData()
                Log.d("AnalyticsViewModel", "Retrieved hourly data with ${hourlyData.size} hours")
                processHourlyData(hourlyData)
            }
            TimeRangePreset.LAST_7_DAYS -> {
                // Get daily data for the week
                val dailyData = usageStatsRepository.getDailyUsageData(7)
                Log.d("AnalyticsViewModel", "Retrieved daily data with ${dailyData.size} days")
                processDailyData(dailyData)
            }
            TimeRangePreset.LAST_30_DAYS -> {
                // Get weekly data for the month
                val weeklyData = usageStatsRepository.getWeeklyUsageData(4)
                Log.d("AnalyticsViewModel", "Retrieved weekly data with ${weeklyData.size} weeks")
                processWeeklyData(weeklyData)
            }
            TimeRangePreset.LAST_90_DAYS -> {
                // Get weekly data for 3 months
                val weeklyData = usageStatsRepository.getWeeklyUsageData(12)
                Log.d("AnalyticsViewModel", "Retrieved weekly data with ${weeklyData.size} weeks")
                processWeeklyData(weeklyData)
            }
        }
    }
    
    private fun processHourlyData(hourlyData: Map<Int, Long>) {
        val hourlyUsageList = hourlyData.map { (hour, timeSpent) ->
            HourlyUsage(
                hour = hour,
                timeSpent = timeSpent,
                sessionCount = 1 // We don't have session count in this simplified approach
            )
        }.sortedBy { it.hour }
        
        _hourlyUsage.postValue(hourlyUsageList)
        
        // Calculate total time
        val totalTime = hourlyData.values.sum()
        val overallUsage = OverallUsage(
            totalTime = totalTime,
            overLimitTime = 0, // We don't have over limit time in this approach
            dailyUsage = hourlyData.values.toList()
        )
        _overallUsage.postValue(overallUsage)
    }
    
    private fun processDailyData(dailyData: Map<Long, Long>) {
        val calendar = Calendar.getInstance()
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        val dailyUsageList = dailyData.map { (timestamp, timeSpent) ->
            calendar.timeInMillis = timestamp
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-based index
            
            DailyUsage(
                date = Date(timestamp),
                dayOfWeek = dayNames[dayOfWeek],
                timeSpent = timeSpent,
                sessionCount = 1 // We don't have session count in this simplified approach
            )
        }.sortedBy { it.date }
        
        _weeklyTrends.postValue(dailyUsageList)
        
        // Calculate total time
        val totalTime = dailyData.values.sum()
        val overallUsage = OverallUsage(
            totalTime = totalTime,
            overLimitTime = 0, // We don't have over limit time in this approach
            dailyUsage = dailyData.values.toList()
        )
        _overallUsage.postValue(overallUsage)
    }
    
    private fun processWeeklyData(weeklyData: Map<Long, Long>) {
        val calendar = Calendar.getInstance()
        
        val weeklyUsageList = weeklyData.map { (timestamp, timeSpent) ->
            calendar.timeInMillis = timestamp
            val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
            
            DailyUsage( // Reusing DailyUsage for weekly data
                date = Date(timestamp),
                dayOfWeek = "Week $weekNumber",
                timeSpent = timeSpent,
                sessionCount = 1 // We don't have session count in this simplified approach
            )
        }.sortedBy { it.date }
        
        _weeklyTrends.postValue(weeklyUsageList)
        
        // Calculate total time
        val totalTime = weeklyData.values.sum()
        val overallUsage = OverallUsage(
            totalTime = totalTime,
            overLimitTime = 0, // We don't have over limit time in this approach
            dailyUsage = weeklyData.values.toList()
        )
        _overallUsage.postValue(overallUsage)
    }
    
    private fun processAppUsageBreakdown(appUsageData: List<AppUsageData>) {
        _appUsageBreakdown.postValue(appUsageData.sortedByDescending { it.timeSpent })
    }
} 