package com.example.mindsync.data.model

/**
 * Represents a pattern in app usage
 */
data class UsagePattern(
    val id: String,
    val appName: String,
    val timeOfDay: String,
    val daysOfWeek: List<String>,
    val averageDuration: Long
) 