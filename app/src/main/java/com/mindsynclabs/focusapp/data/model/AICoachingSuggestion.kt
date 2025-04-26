package com.mindsynclabs.focusapp.data.model

/**
 * Represents a coaching suggestion provided by AI
 */
data class AICoachingSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val category: SuggestionCategory,
    val priority: Int,
    val actionType: ActionType
)

/**
 * Categories for coaching suggestions
 */
enum class SuggestionCategory {
    FOCUS_IMPROVEMENT,
    SOCIAL_MEDIA_BALANCE,
    PRODUCTIVITY_BOOST,
    SCREEN_TIME_MANAGEMENT,
    HABIT_FORMATION
}

/**
 * Types of actions that can be taken for a suggestion
 */
enum class ActionType {
    SET_TIMER,
    ADJUST_LIMIT,
    TAKE_BREAK,
    ENABLE_FEATURE,
    VIEW_DETAILS
} 