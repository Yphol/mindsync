package com.mindsynclabs.focusapp.data.model

/**
 * Represents usage patterns detected from app usage data
 */
data class UsagePatterns(
    val peakHours: String,
    val avgSessionTime: Long,
    val mostUsedApps: List<String>,
    val commonPatterns: List<String>
) 