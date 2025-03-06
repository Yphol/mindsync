package com.example.mindsync.di

import com.example.mindsync.data.repository.MockAnalyticsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
} 