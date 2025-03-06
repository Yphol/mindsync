package com.example.mindsync.data.model

import java.util.*

enum class TimeRangePreset {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS
}

data class TimeRange(
    val start: Date,
    val end: Date
)

data class AppUsageData(
    val appName: String,
    val packageName: String,
    val timeSpent: Long,
    val date: Date,
    val overLimitTime: Long = 0,
    val sessionCount: Int = 0
)

data class DailyUsageData(
    val date: Date,
    val totalTimeSpent: Long,
    val appUsage: List<AppUsageData>,
    val focusScore: Int
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val achieved: Boolean,
    val progress: Int,
    val maxProgress: Int,
    val category: AchievementCategory
)

enum class AchievementCategory {
    FOCUS,
    PRODUCTIVITY,
    SOCIAL_MEDIA,
    STREAK,
    MILESTONE
}

data class PeerComparison(
    val metric: String,
    val userValue: Double,
    val averageValue: Double,
    val percentile: Int,
    val category: ComparisonCategory
)

enum class ComparisonCategory {
    FOCUS_TIME,
    SOCIAL_MEDIA_USAGE,
    PRODUCTIVITY,
    APP_USAGE,
    SCREEN_TIME
}

data class AICoachingSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val category: SuggestionCategory,
    val priority: Int,
    val actionType: ActionType
)

enum class SuggestionCategory {
    FOCUS_IMPROVEMENT,
    SOCIAL_MEDIA_BALANCE,
    PRODUCTIVITY_BOOST,
    SCREEN_TIME_MANAGEMENT,
    HABIT_FORMATION
}

enum class ActionType {
    SET_TIMER,
    ADJUST_LIMIT,
    TAKE_BREAK,
    ENABLE_FEATURE,
    VIEW_DETAILS
}

data class UsagePattern(
    val id: String,
    val appName: String,
    val timeOfDay: String,
    val daysOfWeek: List<String>,
    val averageDuration: Long
)

data class AnalyticsSummary(
    val dailyStats: List<DailyUsageData>,
    val achievements: List<Achievement>,
    val peerComparisons: List<PeerComparison>,
    val coachingSuggestions: List<AICoachingSuggestion>,
    val successMetrics: SuccessMetrics
)

data class SuccessMetrics(
    val limitAdherence: Double,
    val focusSessionSuccess: Double,
    val productivityScore: Double
)

data class OverallUsage(
    val totalTime: Long,
    val overLimitTime: Long,
    val dailyUsage: List<Long>
)

data class HourlyUsage(
    val hour: Int,
    val timeSpent: Long,
    val sessionCount: Int
)

data class DailyUsage(
    val date: Date,
    val dayOfWeek: String,
    val timeSpent: Long,
    val sessionCount: Int
)

data class UsageSpike(
    val appName: String,
    val startTime: Date,
    val endTime: Date,
    val duration: Long,
    val sessionCount: Int
)

data class UsagePatterns(
    val peakHours: String,
    val avgSessionTime: Long,
    val mostUsedApps: List<String>,
    val commonPatterns: List<String>
) 