package com.example.todoapp.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.todoapp.data.database.converters.Converters
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.database.entity.TodoEntity

@Database(
    entities = [TodoEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    
    abstract fun todoDao(): TodoDao
    
    companion object {
        const val DATABASE_NAME = "todo_database"
        
        @Volatile
        private var INSTANCE: TodoDatabase? = null
        
        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Callback для предзаполнения базы данных
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Можно добавить предзаполнение данных при первом создании
            }
        }
        
        // Пример миграции для будущих версий
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пример добавления нового столбца
                // database.execSQL("ALTER TABLE todos ADD COLUMN tags TEXT")
            }
        }
    }
}