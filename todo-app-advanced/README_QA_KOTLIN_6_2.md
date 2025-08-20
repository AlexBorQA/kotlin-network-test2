# 🔹 Ветка qa-kotlin-6-2: MockWebServer API тестирование

## 📋 Описание
Данная ветка содержит реализацию **второй части домашнего задания** - тестирование API с использованием MockWebServer.

## 🎯 Задача
✅ **Реализовать mock-сервер** с **MockWebServer** для тестирования API (GET, POST, DELETE)

## 📊 Статистика
- **9/9 тестов проходят** ✅
- **100% success rate**
- **Технологии:** MockWebServer, Retrofit, JSON, HTTP status codes

## 🧪 Реализованные тесты

### Task2_MockWebServerTest.kt
1. ✅ `test GET all todos returns list of tasks`
2. ✅ `test GET todo by id returns single task`
3. ✅ `test POST creates new todo and returns it with id`
4. ✅ `test PUT updates existing todo`
5. ✅ `test DELETE removes todo`
6. ✅ `test handles 404 error correctly`
7. ✅ `test request timeout throws exception`
8. ✅ `test request contains correct headers`
9. ✅ `test batch sync sends multiple todos`

## 🛠️ Архитектура тестов
- `BaseTest.kt` - базовый класс с общей настройкой
- `BaseMockWebServerTest.kt` - базовый класс для MockWebServer
- `TestDataFactory.kt` - фабрика тестовых данных
- `MockFactory.kt` - фабрика mock объектов  
- `TestExtensions.kt` - вспомогательные функции

## 🚀 Запуск тестов
```bash
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"
```

## 🔧 Ключевые технологии
- **MockWebServer** для эмуляции HTTP сервера
- **Retrofit** для REST API клиента
- **JSON serialization/deserialization**
- **HTTP status codes** и error handling
- **Request/Response validation**
- **Timeouts и async operations**

## 🌐 Покрытие HTTP методов
- ✅ **GET** - получение списка и единичных объектов
- ✅ **POST** - создание новых ресурсов
- ✅ **PUT** - обновление существующих ресурсов
- ✅ **DELETE** - удаление ресурсов
- ✅ **Error handling** - 404, timeouts, invalid responses

---

*Часть домашнего задания "ДЗ 6: Тестирование сетевого слоя"*
