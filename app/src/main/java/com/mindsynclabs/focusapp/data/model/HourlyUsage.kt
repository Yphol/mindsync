package com.mindsynclabs.focusapp.data.model

/**
 * Represents usage data for a specific hour
 */
data class HourlyUsage(
    val hour: Int,
    val timeSpent: Long,
    val sessionCount: Int
) 