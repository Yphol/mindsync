package com.example.mindsync.data.model

import java.util.*

/**
 * Represents usage data for a specific day
 */
data class DailyUsageData(
    val date: Date,
    val totalTimeSpent: Long,
    val appUsage: List<AppUsageData>,
    val focusScore: Int
) 