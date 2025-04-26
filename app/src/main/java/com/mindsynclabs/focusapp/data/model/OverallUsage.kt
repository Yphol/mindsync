package com.mindsynclabs.focusapp.data.model

/**
 * Represents overall usage statistics
 */
data class OverallUsage(
    val totalTime: Long,
    val overLimitTime: Long,
    val dailyUsage: List<Long>
) 