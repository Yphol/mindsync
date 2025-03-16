package com.example.mindsync.data.model

import java.util.*

/**
 * Represents usage data for a specific app
 */
data class AppUsageData(
    val appName: String,
    val packageName: String,
    val timeSpent: Long,
    val date: Date,
    val overLimitTime: Long = 0,
    val sessionCount: Int = 1
) 