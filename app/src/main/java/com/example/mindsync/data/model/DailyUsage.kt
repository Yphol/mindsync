package com.example.mindsync.data.model

import java.util.*

/**
 * Represents usage data for a specific day
 */
data class DailyUsage(
    val date: Date,
    val dayOfWeek: String,
    val timeSpent: Long,
    val sessionCount: Int
) 