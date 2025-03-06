package com.example.mindsync.data.repository

import com.example.mindsync.data.model.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockAnalyticsRepository @Inject constructor() {
    private val socialMediaApps = listOf(
        "Instagram" to "com.instagram.android",
        "Facebook" to "com.facebook.katana",
        "Twitter" to "com.twitter.android",
        "TikTok" to "com.zhiliaoapp.musically",
        "Snapchat" to "com.snapchat.android"
    )

    fun getSocialMediaUsage(startDate: Date, endDate: Date): List<AppUsageData> {
        val usageData = mutableListOf<AppUsageData>()
        val calendar = Calendar.getInstance()
        var currentDate = startDate

        while (currentDate <= endDate) {
            socialMediaApps.forEach { (appName, packageName) ->
                // Generate random usage data for each app for each day
                val timeSpent = Random.nextLong(15 * 60 * 1000, 3 * 60 * 60 * 1000) // 15min to 3h
                val overLimitTime = if (Random.nextBoolean()) Random.nextLong(0, timeSpent / 2) else 0
                val sessionCount = Random.nextInt(5, 20)

                usageData.add(
                    AppUsageData(
                        appName = appName,
                        packageName = packageName,
                        timeSpent = timeSpent,
                        date = currentDate,
                        overLimitTime = overLimitTime,
                        sessionCount = sessionCount
                    )
                )
            }

            // Move to next day
            calendar.time = currentDate
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            currentDate = calendar.time
        }

        return usageData
    }

    fun getAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "digital_detox_7",
                title = "Digital Detox Master",
                description = "Stay under app limits for 7 consecutive days",
                achieved = Random.nextBoolean(),
                progress = Random.nextInt(0, 7),
                maxProgress = 7,
                category = AchievementCategory.SOCIAL_MEDIA
            ),
            Achievement(
                id = "focus_champion",
                title = "Focus Champion",
                description = "Maintain a focus score above 80 for 5 days",
                achieved = Random.nextBoolean(),
                progress = Random.nextInt(0, 5),
                maxProgress = 5,
                category = AchievementCategory.FOCUS
            ),
            Achievement(
                id = "productive_streak",
                title = "Productivity Streak",
                description = "Use productive apps more than social media for 10 days",
                achieved = Random.nextBoolean(),
                progress = Random.nextInt(0, 10),
                maxProgress = 10,
                category = AchievementCategory.PRODUCTIVITY
            )
        )
    }

    fun getPeerComparison(): List<PeerComparison> {
        return listOf(
            PeerComparison(
                metric = "Daily Social Media Usage",
                userValue = Random.nextDouble(1.0, 4.0),
                averageValue = 2.5,
                percentile = Random.nextInt(0, 100),
                category = ComparisonCategory.SOCIAL_MEDIA_USAGE
            ),
            PeerComparison(
                metric = "Focus Score",
                userValue = Random.nextDouble(60.0, 90.0),
                averageValue = 75.0,
                percentile = Random.nextInt(0, 100),
                category = ComparisonCategory.FOCUS_TIME
            ),
            PeerComparison(
                metric = "Productive App Usage",
                userValue = Random.nextDouble(2.0, 6.0),
                averageValue = 4.0,
                percentile = Random.nextInt(0, 100),
                category = ComparisonCategory.PRODUCTIVITY
            )
        )
    }

    fun getCoachingSuggestions(): List<AICoachingSuggestion> {
        return listOf(
            AICoachingSuggestion(
                id = "morning_routine",
                title = "Optimize Your Morning Routine",
                description = "Your peak productivity is between 9-11 AM. Consider scheduling focus sessions during this time.",
                category = SuggestionCategory.PRODUCTIVITY_BOOST,
                priority = 80,
                actionType = ActionType.SET_TIMER
            ),
            AICoachingSuggestion(
                id = "social_media_balance",
                title = "Social Media Usage Pattern",
                description = "You tend to use social media most during work hours. Try setting stricter limits during these times.",
                category = SuggestionCategory.SOCIAL_MEDIA_BALANCE,
                priority = 90,
                actionType = ActionType.ADJUST_LIMIT
            ),
            AICoachingSuggestion(
                id = "evening_wind_down",
                title = "Evening Wind Down",
                description = "Reduce screen time 1 hour before bed to improve sleep quality.",
                category = SuggestionCategory.SCREEN_TIME_MANAGEMENT,
                priority = 85,
                actionType = ActionType.TAKE_BREAK
            )
        )
    }
} 