package com.mindsynclabs.focusapp.data.model

/**
 * Represents a usage spike item for display in the UI
 */
data class UsageSpikeItem(
    val appName: String,
    val date: String,
    val duration: String,
    val timeRange: String
) 