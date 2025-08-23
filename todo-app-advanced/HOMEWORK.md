да# Домашнее задание: Тестирование Android приложения Todo App

## 📌 Задание

### Что нужно сделать:
✅ **Протестировать приложение с SQLite**, используя mock-объекты для работы со списком задач  
✅ **Реализовать mock-сервер** с **MockWebServer** для тестирования API (GET, POST, DELETE)  
✅ **Написать автоматические тесты** для проверки CRUD-операций с задачами  
✅ **Сформировать отчёты в Allure** и загрузить их в каталог reports на GitHub  

## 🔧 Структура проекта

В проекте уже подготовлены шаблоны тестов:

### 1. `Task1_SQLiteMockTest.kt` - Тестирование SQLite с in-memory Room DB
- Использование настоящей in-memory базы данных Room
- Тестирование работы с реальным DAO без файловой системы
- Быстрые и изолированные тесты
- 7 тестов для основных операций с БД

### 2. `Task2_MockWebServerTest.kt` - Тестирование API с MockWebServer  
- Самостоятельная настройка MockWebServer и Retrofit
- Тестирование всех HTTP методов (GET, POST, PUT, DELETE)
- Проверка обработки ошибок и таймаутов
- 9 тестов для сетевых операций

### 3. `Task3_CRUDOperationsTest.kt` - Тестирование CRUD операций
- Полный цикл создания, чтения, обновления и удаления
- Синхронизация между локальной БД и сервером
- Разрешение конфликтов и работа в offline режиме
- 10 тестов для бизнес-логики

## 📦 Необходимые зависимости

Все зависимости уже добавлены в `build.gradle.kts`:
- MockK для создания mock-объектов
- MockWebServer для эмуляции сервера
- Coroutines Test для тестирования корутин
- WorkManager Testing для тестирования фоновых задач

Для Allure добавьте в `build.gradle.kts`:
```kotlin
dependencies {
    testImplementation("io.qameta.allure:allure-kotlin-junit4:2.4.0")
}

// Allure plugin
plugins {
    id("io.qameta.allure") version "2.11.2"
}
```

## 🎯 Требования к реализации

### Для каждого теста:
1. Удалите строку `fail("Test not implemented...")` 
2. Реализуйте тест согласно описанию в комментарии
3. Используйте предоставленные mock-объекты
4. Проверьте все условия из "Ожидаемый результат"

### Пример подхода к реализации:
```kotlin
@Test
fun `test example`() = runTest {
    // Arrange - подготовка тестовых данных
    // Создайте необходимые объекты и настройте окружение
    
    // Act - выполнение тестируемого действия
    // Вызовите метод, который хотите протестировать
    
    // Assert - проверка результата
    // Проверьте, что результат соответствует ожиданиям
}
```

## 🌳 Структура веток

Создайте две ветки в вашем репозитории:
- `qa-kotlin-6-1` – тесты для SQLite с in-memory Room DB (Task1)
- `qa-kotlin-6-2` – тесты с MockWebServer и CRUD операции (Task2 + Task3)

## 📊 Allure отчёты

### Генерация отчётов:
```bash
# Запуск тестов с Allure
./gradlew clean test

# Генерация отчёта
./gradlew allureReport

# Открытие отчёта
./gradlew allureServe
```

### Структура отчёта:
- Поместите отчёты в папку `reports/allure-report`
- Добавьте скриншоты в README.md
- Включите статистику покрытия кода

## ✅ Критерии оценки

### Минимальные требования:
- [ ] Реализовано минимум 13 тестов из 26
- [ ] Тесты проходят успешно
- [ ] Использована in-memory Room DB в Task1
- [ ] Настроен MockWebServer в Task2

### Полное выполнение:
- [ ] Реализованы все 26 тестов
- [ ] Сгенерированы Allure отчёты
- [ ] Покрытие кода > 70%
- [ ] Оформлен Pull Request с описанием

## 🚀 Запуск тестов

```bash
# Все тесты
./gradlew test

# Конкретный класс тестов
./gradlew test --tests "*.Task1_SQLiteMockTest"

# С отчётом о покрытии
./gradlew test jacocoTestReport
```

## 📚 Полезные ссылки

- [MockK Documentation](https://mockk.io/)
- [MockWebServer Guide](https://github.com/square/okhttp/tree/master/mockwebserver)
- [Coroutines Test](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
- [WorkManager Testing](https://developer.android.com/topic/libraries/architecture/workmanager/how-to/testing)
- [Allure Framework](https://docs.qameta.io/allure/)

## ❓ Вопросы

Вопросы задавайте в ТГ-канал группы.

## 📤 Сдача задания

Проект с решением ДЗ прикрепляйте в виде ссылки на merge request.