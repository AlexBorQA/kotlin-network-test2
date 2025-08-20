package com.example.todoapp.di

import com.example.todoapp.core.idling.IdlingResourceProvider
import com.example.todoapp.core.idling.NoOpIdlingResourceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IdlingModule {
    
    @Provides
    @Singleton
    fun provideIdlingResourceProvider(): IdlingResourceProvider {
        return NoOpIdlingResourceProvider()
    }
}