package com.mindsynclabs.focusapp.data.model

import android.graphics.drawable.Drawable

data class AppTimeLimit(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable,
    val timeLimit: Long, // in milliseconds
    val timeUsed: Long // in milliseconds
) 