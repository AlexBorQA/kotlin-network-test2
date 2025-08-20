package com.example.todoapp.di

import com.example.todoapp.data.repository.CachedTodoRepository
import com.example.todoapp.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        cachedTodoRepository: CachedTodoRepository
    ): TodoRepository
}