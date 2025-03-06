package com.example.mindsync.data.model

import android.graphics.drawable.Drawable
import java.util.Date

data class AppRestriction(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val startTime: Date,
    val endTime: Date?,  // null if restricted forever
    val isForever: Boolean,
    val remainingTime: Long = 0
) 