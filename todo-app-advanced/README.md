# 🎯 ДЗ 6: Тестирование сетевого слоя - Todo App Advanced

## 📋 Описание проекта

Данный проект выполняет **домашнее задание по тестированию сетевого слоя** Android приложения с использованием современных подходов и инструментов.

## ✅ Выполненные требования

### 🔹 **Две ветки с тестами:**
- **`qa-kotlin-6-1`** - SQLite Mock тестирование (7/7 тестов)
- **`qa-kotlin-6-2`** - MockWebServer API тестирование (9/9 тестов)

### 📊 **Allure отчеты:**
- **`reports/qa-kotlin-6-1/`** - отчеты для SQLite тестов
- **`reports/qa-kotlin-6-2/`** - отчеты для MockWebServer тестов

---

## 🔹 Ветка qa-kotlin-6-1: SQLite Mock тестирование

### Задача
✅ **Протестировать приложение с SQLite**, используя mock-объекты для работы со списком задач

### Статистика
- **7/7 тестов проходят** ✅
- **100% success rate**
- **Технологии:** Room DB (in-memory), MockK, Coroutines Test, Allure

### Реализованные тесты
1. ✅ `test getAllTodos returns all tasks from database`
2. ✅ `test insertTodo saves entity and returns generated ID`
3. ✅ `test updateTodo modifies existing entity`
4. ✅ `test deleteTodo removes entity from database`
5. ✅ `test getTodoById returns single entity or null`
6. ✅ `test markAsCompleted changes completion status`
7. ✅ `test toggleCompletion switches between states`

### Запуск тестов
⚠️ **Рабочая директория:** Все команды выполняются из корня проекта `todo-app-advanced/`
```bash
cd todo-app-advanced  # Убедитесь что находитесь в корне проекта
git checkout qa-kotlin-6-1
./gradlew testDebugUnitTest --tests "Task1_SQLiteMockTest"
```

### Генерация Allure отчета
⚠️ **ВНИМАНИЕ:** Allure плагин доступен только в ветке `qa-kotlin-6-1`
```bash
cd todo-app-advanced  # Убедитесь что находитесь в корне проекта
git checkout qa-kotlin-6-1  # Обязательно переключиться!
./gradlew allureReport --clean
```

---

## 🔹 Ветка qa-kotlin-6-2: MockWebServer API тестирование

### Задача
✅ **Реализовать mock-сервер** с **MockWebServer** для тестирования API (GET, POST, DELETE)

### Статистика
- **9/9 тестов проходят** ✅
- **100% success rate**
- **Технологии:** MockWebServer, Retrofit, JSON, HTTP status codes, Allure

### Реализованные тесты
1. ✅ `test GET all todos returns list of tasks`
2. ✅ `test GET todo by id returns single task`
3. ✅ `test POST creates new todo and returns it with id`
4. ✅ `test PUT updates existing todo`
5. ✅ `test DELETE removes todo`
6. ✅ `test handles 404 error correctly`
7. ✅ `test request timeout throws exception`
8. ✅ `test request contains correct headers`
9. ✅ `test batch sync sends multiple todos`

### Запуск тестов
⚠️ **Рабочая директория:** Все команды выполняются из корня проекта `todo-app-advanced/`
```bash
cd todo-app-advanced  # Убедитесь что находитесь в корне проекта
git checkout qa-kotlin-6-2
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"
```

### Генерация Allure отчета
⚠️ **ВНИМАНИЕ:** Allure плагин доступен только в ветке `qa-kotlin-6-2`
```bash
cd todo-app-advanced  # Убедитесь что находитесь в корне проекта
git checkout qa-kotlin-6-2  # Обязательно переключиться!
./gradlew allureReport --clean
```

---

## 🛠️ Архитектура тестов

### Базовые классы
- **`BaseTest.kt`** - Android Log mocking, common setup
- **`TestDataFactory.kt`** - фабрика тестовых данных
- **`MockFactory.kt`** - фабрика mock объектов
- **`TestExtensions.kt`** - extension функции для assertions
- **`BaseMockWebServerTest.kt`** - базовый класс для MockWebServer (только в qa-kotlin-6-2)

### Ключевые технологии
| Область | Технологии |
|---------|------------|
| **Testing Framework** | JUnit 4, MockK, kotlinx-coroutines-test |
| **Database Testing** | Room (in-memory), Flow testing |
| **Network Testing** | MockWebServer, Retrofit, JSON serialization |
| **Reporting** | Allure Framework с аннотациями |
| **Build System** | Gradle KTS, Android Gradle Plugin |

---

## 📊 Allure отчеты

### Просмотр отчетов
Allure отчеты находятся в каталоге `reports/` и содержат:
- **Статистику прохождения тестов**
- **Детализацию по Epic/Feature/Story**
- **Severity classification**
- **Временные метрики**
- **Логи выполнения**

### Структура отчетов
```
reports/
├── qa-kotlin-6-1/          # SQLite Mock тестирование
│   ├── index.html          # Главная страница отчета
│   ├── data/              # Данные тестов
│   └── history/           # История прогонов
└── qa-kotlin-6-2/          # MockWebServer API тестирование
    ├── index.html          # Главная страница отчета
    ├── data/              # Данные тестов
    └── history/           # История прогонов
```

---

## 🚀 Быстрый старт

### 1. Клонирование и настройка
```bash
git clone https://github.com/AlexBorQA/kotlin-network-testing.git
cd kotlin-network-testing/todo-app-advanced
```

### 2. Тестирование SQLite (ветка qa-kotlin-6-1)
```bash
git checkout qa-kotlin-6-1
./gradlew testDebugUnitTest --tests "Task1_SQLiteMockTest"
./gradlew allureReport --clean
```

### 3. Тестирование MockWebServer (ветка qa-kotlin-6-2)
```bash
git checkout qa-kotlin-6-2
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"
./gradlew allureReport --clean
```

### 4. Просмотр Allure отчетов
```bash
# Открыть отчет SQLite тестов
# Запустить HTTP сервер для просмотра отчета
python3 -m http.server 9000 -d app/build/reports/allure-report/allureReport &

# Открыть отчет в браузере
open http://localhost:9000

# Открыть отчет MockWebServer тестов
# Запустить HTTP сервер для просмотра отчета
python3 -m http.server 9001 -d app/build/reports/allure-report/allureReport &

# Открыть отчет в браузере
open http://localhost:9001
```

---

## 🎯 Результаты выполнения задания

### ✅ Полностью выполнено
- **16/16 основных тестов проходят** (7 SQLite + 9 MockWebServer)
- **100% success rate** для обеих веток
- **Allure отчеты** сгенерированы и размещены в `reports/`
- **Правильная Git структура** с двумя специализированными ветками
- **Современный стек технологий** для Android тестирования

### 🏆 Ключевые достижения
1. **Создана robust тестовая архитектура** с переиспользуемыми компонентами
2. **Полное покрытие SQLite операций** с in-memory базой данных
3. **Comprehensive MockWebServer тестирование** всех HTTP методов
4. **Интеграция с Allure Framework** для профессиональной отчетности
5. **Clean Architecture** в тестах с разделением ответственности

---

## 📞 Контакты и поддержка

**Автор:** Senior QA Engineer  
**Технологии:** Android, Kotlin, JUnit, MockK, Allure  
**Репозиторий:** [kotlin-network-testing](https://github.com/AlexBorQA/kotlin-network-testing)

---

*Домашнее задание выполнено в рамках курса "ДЗ 6: Тестирование сетевого слоя"*
