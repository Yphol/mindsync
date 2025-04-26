package com.mindsynclabs.focusapp.data.model

/**
 * Represents a summary of analytics data
 */
data class AnalyticsSummary(
    val dailyStats: List<DailyUsageData>,
    val achievements: List<Achievement>,
    val peerComparisons: List<PeerComparison>,
    val coachingSuggestions: List<AICoachingSuggestion>,
    val successMetrics: SuccessMetrics
) 