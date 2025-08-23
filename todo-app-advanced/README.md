# ДЗ 6: Тестирование сетевого слоя - Todo App Advanced

## Описание проекта

Данный проект выполняет домашнее задание по тестированию сетевого слоя Android приложения.

## Выполненные требования

### Две ветки с тестами:
- qa-kotlin-6-1 - SQLite Mock тестирование (7/7 тестов)
- qa-kotlin-6-2 - MockWebServer API тестирование (9/9 тестов)

### Allure отчеты:
- reports/qa-kotlin-6-1/ - отчеты для SQLite тестов
- reports/qa-kotlin-6-2/ - отчеты для MockWebServer тестов

## Ветка qa-kotlin-6-1: SQLite Mock тестирование

### Задача
Протестировать приложение с SQLite, используя mock-объекты для работы со списком задач

### Статистика
- 7/7 тестов проходят
- 100% success rate
- Технологии: Room DB (in-memory), MockK, Coroutines Test, Allure

### Реализованные тесты
1. test getAllTodos returns all tasks from database
2. test insertTodo saves entity and returns generated ID
3. test updateTodo modifies existing entity
4. test deleteTodo removes entity from database
5. test getTodoById returns single entity or null
6. test markAsCompleted changes completion status
7. test toggleCompletion switches between states

### Запуск тестов
```bash
git checkout qa-kotlin-6-1
./gradlew testDebugUnitTest --tests "Task1_SQLiteMockTest"
```

### Генерация Allure отчета
```bash
./gradlew allureReport --clean
```

## Ветка qa-kotlin-6-2: MockWebServer API тестирование

### Задача
Реализовать mock-сервер с MockWebServer для тестирования API (GET, POST, DELETE)

### Статистика
- 9/9 тестов проходят
- 100% success rate
- Технологии: MockWebServer, Retrofit, Kotlinx Serialization, Allure

### Реализованные тесты
1. test get all todos returns empty list when server responds with empty array
2. test get all todos returns parsed todos when server responds with valid data
3. test get todo by ID returns parsed todo when server responds with valid data
4. test get todo by ID throws exception when server responds with 404
5. test create todo returns created todo when server responds with success
6. test create todo throws exception when server responds with error
7. test delete todo completes successfully when server responds with 204
8. test delete todo throws exception when server responds with 404
9. test sync todos handles multiple operations correctly

### Запуск тестов
```bash
git checkout qa-kotlin-6-2
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"
```

### Генерация Allure отчета
```bash
./gradlew allureReport --clean
```

## Быстрый старт

### 1. Клонирование репозитория
```bash
git clone https://github.com/AlexBorQA/kotlin-network-testing.git
cd kotlin-network-testing/todo-app-advanced
```

### 2. Запуск SQLite тестов
```bash
git checkout qa-kotlin-6-1
cd todo-app-advanced
./gradlew testDebugUnitTest --tests "Task1_SQLiteMockTest"
./gradlew allureReport --clean
```

### 3. Запуск MockWebServer тестов
```bash
git checkout qa-kotlin-6-2
cd todo-app-advanced
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"
./gradlew allureReport --clean
```

### 4. Просмотр Allure отчетов

Важно: Allure отчеты должны открываться через HTTP сервер из-за CORS политики браузеров.

```bash
# Для отчета SQLite тестов (ветка qa-kotlin-6-1)
cd todo-app-advanced
python3 -m http.server 9040 --directory app/build/reports/allure-report/allureReport &
open http://localhost:9040

# Для отчета MockWebServer тестов (ветка qa-kotlin-6-2)  
cd todo-app-advanced
python3 -m http.server 9041 --directory app/build/reports/allure-report/allureReport &
open http://localhost:9041
```

## Результаты выполнения задания

### Полностью выполнено
- 16/16 основных тестов проходят (7 SQLite + 9 MockWebServer)
- 100% success rate для обеих веток
- Allure отчеты сгенерированы и размещены в reports/
- Правильная Git структура с двумя специализированными ветками
- Современный стек технологий для Android тестирования

### Ключевые достижения
1. Полная реализация SQLite mock тестирования с Room in-memory базой
2. Комплексное MockWebServer API тестирование с различными HTTP сценариями
3. Интеграция Allure для детальной отчетности с аннотациями и описаниями
4. Правильная архитектура проекта с разделением ответственности
5. Современные подходы к Android тестированию (MockK, Coroutines Test, etc.)

### Технический стек
- Kotlin 1.9.10
- Android Gradle Plugin 8.1.2
- JUnit 4.13.2
- MockK 1.13.8
- MockWebServer 4.12.0
- Kotlinx Coroutines Test 1.7.3
- Allure Framework 2.24.0
- Room Database (in-memory для тестов)
- Retrofit 2.9.0

## Структура проекта

```
todo-app-advanced/
├── app/
│   ├── src/main/java/com/example/todoapp/
│   │   ├── data/          # Слой данных (Repository, DAO, API)
│   │   ├── domain/        # Бизнес-логика (Entity, UseCase)
│   │   └── presentation/  # UI слой (Activity, Fragment, ViewModel)
│   └── src/test/java/com/example/todoapp/
│       ├── homework/      # Основные тесты ДЗ
│       │   ├── Task1_SQLiteMockTest.kt      (ветка qa-kotlin-6-1)
│       │   └── Task2_MockWebServerTest.kt   (ветка qa-kotlin-6-2)
│       └── testing/       # Утилиты для тестирования
├── reports/               # Allure отчеты
│   ├── qa-kotlin-6-1/    # SQLite тесты
│   └── qa-kotlin-6-2/    # MockWebServer тесты
├── README.md             # Этот файл
└── QUICK_START.md        # Краткий гайд
```
