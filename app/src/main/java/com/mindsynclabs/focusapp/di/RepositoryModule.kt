package com.mindsynclabs.focusapp.di

import android.content.Context
import com.mindsynclabs.focusapp.data.repository.MockAnalyticsRepository
import com.mindsynclabs.focusapp.data.repository.UsageStatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideMockAnalyticsRepository(): MockAnalyticsRepository {
        return MockAnalyticsRepository()
    }
    
    @Provides
    @Singleton
    fun provideUsageStatsRepository(@ApplicationContext context: Context): UsageStatsRepository {
        return UsageStatsRepository(context)
    }
} 