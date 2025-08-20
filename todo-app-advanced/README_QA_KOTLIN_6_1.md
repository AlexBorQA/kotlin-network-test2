# 🔹 Ветка qa-kotlin-6-1: SQLite Mock тестирование

## 📋 Описание
Данная ветка содержит реализацию **первой части домашнего задания** - тестирование SQLite с использованием mock-объектов.

## 🎯 Задача
✅ **Протестировать приложение с SQLite**, используя mock-объекты для работы со списком задач

## 📊 Статистика
- **7/7 тестов проходят** ✅
- **100% success rate**
- **Технологии:** Room DB (in-memory), MockK, Coroutines Test

## 🧪 Реализованные тесты

### Task1_SQLiteMockTest.kt
1. ✅ `test insertTodo saves entity and returns generated ID`
2. ✅ `test getAllTodos returns flow of domain models`
3. ✅ `test updateTodo modifies existing entity`
4. ✅ `test deleteTodo removes entity from database`
5. ✅ `test getTodoById returns single entity or null`
6. ✅ `test markAsCompleted changes completion status`
7. ✅ `test toggleCompletion switches between states`

## 🛠️ Архитектура тестов
- `BaseTest.kt` - базовый класс с общей настройкой
- `TestDataFactory.kt` - фабрика тестовых данных
- `MockFactory.kt` - фабрика mock объектов
- `TestExtensions.kt` - вспомогательные функции

## 🚀 Запуск тестов
```bash
./gradlew testDebugUnitTest --tests "Task1_SQLiteMockTest"
```

## 🔧 Ключевые технологии
- **Room Database** (in-memory для тестов)
- **MockK** для создания mock-объектов
- **kotlinx-coroutines-test** для тестирования корутин
- **Flow testing** для реактивных потоков
- **JUnit 4** как основной тестовый фреймворк

---

*Часть домашнего задания "ДЗ 6: Тестирование сетевого слоя"*
