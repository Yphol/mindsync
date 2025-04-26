package com.mindsynclabs.focusapp.data.model

/**
 * Represents a comparison between the user's metrics and their peers
 */
data class PeerComparison(
    val metric: String,
    val userValue: Double,
    val averageValue: Double,
    val percentile: Int,
    val category: ComparisonCategory
)

/**
 * Categories for peer comparisons
 */
enum class ComparisonCategory {
    FOCUS_TIME,
    SOCIAL_MEDIA_USAGE,
    PRODUCTIVITY,
    APP_USAGE,
    SCREEN_TIME
} 