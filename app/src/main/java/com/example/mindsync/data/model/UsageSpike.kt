package com.example.mindsync.data.model

import java.util.*

/**
 * Represents a spike in app usage
 */
data class UsageSpike(
    val appName: String,
    val startTime: Date,
    val endTime: Date,
    val duration: Long,
    val sessionCount: Int
) 