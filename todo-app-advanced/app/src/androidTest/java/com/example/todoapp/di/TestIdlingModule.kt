package com.example.todoapp.di

import com.example.todoapp.core.idling.IdlingResourceProvider
import com.example.todoapp.testing.TestIdlingResourceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Тестовый модуль для предоставления IdlingResource в тестах
 * Заменяет продакшн IdlingModule
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [IdlingModule::class]
)
object TestIdlingModule {
    
    @Provides
    @Singleton
    fun provideIdlingResourceProvider(): IdlingResourceProvider {
        return TestIdlingResourceProvider()
    }
    
    @Provides
    @Singleton
    fun provideTestIdlingResourceProvider(
        idlingResourceProvider: IdlingResourceProvider
    ): TestIdlingResourceProvider {
        return idlingResourceProvider as TestIdlingResourceProvider
    }
}