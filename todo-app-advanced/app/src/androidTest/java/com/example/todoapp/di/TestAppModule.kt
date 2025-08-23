package com.example.todoapp.di

import android.content.Context
import androidx.room.Room
import com.example.todoapp.data.database.TodoDatabase
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.network.NetworkManager
import com.example.todoapp.data.repository.CachedTodoRepository
import com.example.todoapp.domain.repository.TodoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {
    
    @Provides
    @Singleton
    fun provideTodoDatabase(@ApplicationContext context: Context): TodoDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
    }
    
    @Provides
    @Singleton
    fun provideTodoDao(database: TodoDatabase): TodoDao {
        return database.todoDao()
    }
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {
    
    @Provides
    @Singleton
    fun provideTodoRepository(
        todoDao: TodoDao,
        @ApplicationContext context: Context
    ): TodoRepository {
        // Для тестов используем моки для API и NetworkManager
        val apiService: TodoApiService = mockk(relaxed = true)
        val networkManager: NetworkManager = mockk(relaxed = true)
        return CachedTodoRepository(todoDao, apiService, networkManager)
    }
}