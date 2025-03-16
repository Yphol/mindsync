package com.example.mindsync.di

import android.content.Context
import com.example.mindsync.data.repository.MockAnalyticsRepository
import com.example.mindsync.data.repository.UsageStatsRepository
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