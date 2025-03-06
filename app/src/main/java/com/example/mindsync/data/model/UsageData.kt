package com.example.mindsync.data.model

data class UsageData(
    val packageName: String,
    val appName: String,
    val usageTimeInMillis: Long,
    val lastTimeUsed: Long
) 