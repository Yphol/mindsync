package com.example.mindsync.ui.analytics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindsync.data.model.*
import com.example.mindsync.data.repository.MockAnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: MockAnalyticsRepository
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
            val calendar = Calendar.getInstance()
            val endDate = calendar.time

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

            val startDate = calendar.time

            // Load all analytics data
            val usageData = analyticsRepository.getSocialMediaUsage(startDate, endDate)
            processUsageData(usageData, startDate, endDate)
        }
    }

    private fun processUsageData(
        usageData: List<AppUsageData>,
        startDate: Date,
        endDate: Date
    ) {
        if (usageData.isEmpty()) return

        // Process overall usage
        val totalTime = usageData.sumOf { it.timeSpent }
        val overLimitTime = usageData.sumOf { it.overLimitTime }
        val dailyUsage = calculateDailyUsage(usageData, startDate, endDate)
        _overallUsage.value = OverallUsage(totalTime, overLimitTime, dailyUsage)

        // Process app breakdown
        _appUsageBreakdown.value = usageData.groupBy { it.appName }
            .map { (appName, data) ->
                AppUsageData(
                    appName = appName,
                    packageName = data.first().packageName,
                    timeSpent = data.sumOf { it.timeSpent },
                    date = data.maxByOrNull { it.date }?.date ?: Date(),
                    overLimitTime = data.sumOf { it.overLimitTime },
                    sessionCount = data.sumOf { it.sessionCount }
                )
            }
            .sortedByDescending { it.timeSpent }

        // Process hourly usage
        _hourlyUsage.value = calculateHourlyUsage(usageData)

        // Process weekly trends
        _weeklyTrends.value = calculateWeeklyTrends(usageData, startDate, endDate)

        // Process usage spikes
        _usageSpikes.value = detectUsageSpikes(usageData)

        // Process usage patterns
        _usagePatterns.value = analyzeUsagePatterns(usageData)
    }

    private fun calculateDailyUsage(
        usageData: List<AppUsageData>,
        startDate: Date,
        endDate: Date
    ): List<Long> {
        val calendar = Calendar.getInstance()
        val dailyUsage = mutableListOf<Long>()
        var currentDate = startDate

        while (currentDate <= endDate) {
            calendar.time = currentDate
            val nextDay = calendar.apply {
                add(Calendar.DAY_OF_YEAR, 1)
                add(Calendar.MILLISECOND, -1)
            }.time

            val dayUsage = usageData.filter { it.date in currentDate..nextDay }
                .sumOf { it.timeSpent }
            dailyUsage.add(dayUsage)

            calendar.add(Calendar.MILLISECOND, 1)
            currentDate = calendar.time
        }

        return dailyUsage
    }

    private fun calculateHourlyUsage(usageData: List<AppUsageData>): List<HourlyUsage> {
        val hourlyMap = (0..23).associateWith { hour ->
            val hourData = usageData.filter {
                Calendar.getInstance().apply { time = it.date }.get(Calendar.HOUR_OF_DAY) == hour
            }
            HourlyUsage(
                hour = hour,
                timeSpent = hourData.sumOf { it.timeSpent },
                sessionCount = hourData.sumOf { it.sessionCount }
            )
        }
        return hourlyMap.values.toList()
    }

    private fun calculateWeeklyTrends(
        usageData: List<AppUsageData>,
        startDate: Date,
        endDate: Date
    ): List<DailyUsage> {
        val calendar = Calendar.getInstance()
        val weeklyTrends = mutableListOf<DailyUsage>()
        var currentDate = startDate

        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        while (currentDate <= endDate) {
            calendar.time = currentDate
            val nextDay = calendar.apply {
                add(Calendar.DAY_OF_YEAR, 1)
                add(Calendar.MILLISECOND, -1)
            }.time

            val dayData = usageData.filter { it.date in currentDate..nextDay }
            weeklyTrends.add(
                DailyUsage(
                    date = currentDate,
                    dayOfWeek = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1],
                    timeSpent = dayData.sumOf { it.timeSpent },
                    sessionCount = dayData.sumOf { it.sessionCount }
                )
            )

            calendar.add(Calendar.MILLISECOND, 1)
            currentDate = calendar.time
        }

        return weeklyTrends
    }

    private fun detectUsageSpikes(usageData: List<AppUsageData>): List<UsageSpike> {
        val spikes = mutableListOf<UsageSpike>()
        val groupedByApp = usageData.groupBy { it.appName }

        for ((appName, appData) in groupedByApp) {
            val avgTimeSpent = appData.map { it.timeSpent }.average()
            val threshold = avgTimeSpent * 1.5 // 50% above average

            appData.filter { it.timeSpent > threshold }
                .forEach { data ->
                    spikes.add(
                        UsageSpike(
                            appName = appName,
                            startTime = data.date,
                            endTime = Calendar.getInstance().apply {
                                time = data.date
                                add(Calendar.MILLISECOND, data.timeSpent.toInt())
                            }.time,
                            duration = data.timeSpent,
                            sessionCount = data.sessionCount
                        )
                    )
                }
        }

        return spikes.sortedByDescending { it.duration }
    }

    private fun analyzeUsagePatterns(usageData: List<AppUsageData>): UsagePatterns {
        // Find peak hours
        val hourlyUsage = calculateHourlyUsage(usageData)
        val peakHours = hourlyUsage
            .sortedByDescending { it.timeSpent }
            .take(3)
            .joinToString("-") { "${it.hour}:00" }

        // Calculate average session time
        val totalSessions = usageData.sumOf { it.sessionCount }
        val totalTime = usageData.sumOf { it.timeSpent }
        val avgSessionTime = if (totalSessions > 0) totalTime / totalSessions else 0

        // Find most used apps
        val mostUsedApps = usageData.groupBy { it.appName }
            .mapValues { it.value.sumOf { data -> data.timeSpent } }
            .entries.sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        // Identify common patterns
        val commonPatterns = mutableListOf<String>()
        
        // Morning usage pattern
        val morningUsage = hourlyUsage.filter { it.hour in 6..11 }
            .sumOf { it.timeSpent }
        if (morningUsage > totalTime * 0.3) {
            commonPatterns.add("Heavy morning usage")
        }

        // Evening usage pattern
        val eveningUsage = hourlyUsage.filter { it.hour in 18..23 }
            .sumOf { it.timeSpent }
        if (eveningUsage > totalTime * 0.4) {
            commonPatterns.add("Heavy evening usage")
        }

        // Frequent short sessions pattern
        val avgSessionsPerHour = totalSessions.toFloat() / 24
        if (avgSessionsPerHour > 3) {
            commonPatterns.add("Frequent short sessions")
        }

        return UsagePatterns(
            peakHours = peakHours,
            avgSessionTime = avgSessionTime,
            mostUsedApps = mostUsedApps,
            commonPatterns = commonPatterns
        )
    }
} 