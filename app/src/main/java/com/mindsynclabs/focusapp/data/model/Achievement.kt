package com.mindsynclabs.focusapp.data.model

/**
 * Represents an achievement that can be earned by the user
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val achieved: Boolean,
    val progress: Int,
    val maxProgress: Int,
    val category: AchievementCategory
)

/**
 * Categories of achievements
 */
enum class AchievementCategory {
    FOCUS,
    PRODUCTIVITY,
    SOCIAL_MEDIA,
    STREAK,
    MILESTONE
} 