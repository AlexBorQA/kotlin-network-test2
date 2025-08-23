git checkout qa-kotlin-6-1# Ветка qa-kotlin-6-2: MockWebServer API тестирование

## Описание
Данная ветка содержит реализацию второй части домашнего задания - тестирование API с использованием MockWebServer.

## Задача
Реализовать mock-сервер с MockWebServer для тестирования API (GET, POST, DELETE)

## Статистика
- 9/9 тестов проходят
- 100% success rate
- Технологии: MockWebServer, Retrofit, JSON, HTTP status codes, Allure

## Реализованные тесты

### Task2_MockWebServerTest.kt
1. test GET all todos returns list of tasks
2. test GET todo by id returns single task
3. test POST creates new todo and returns it with id
4. test PUT updates existing todo
5. test DELETE removes todo from server
6. test error handling for invalid API responses
7. test batch operations with multiple todos
8. test network timeout scenarios
9. test API authentication headers

## Команды для запуска

### Переключение на ветку
```bash
git checkout qa-kotlin-6-2
```

### Запуск тестов
```bash
cd todo-app-advanced
./gradlew clean
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"
```

### Генерация Allure отчета
```bash
./gradlew allureReport --clean
```

### Просмотр отчета
```bash
python3 -m http.server 9041 --directory app/build/reports/allure-report/allureReport &
open http://localhost:9041
```

## Ключевые технологии

### MockWebServer
- Эмуляция HTTP сервера для тестирования
- Настройка различных HTTP ответов
- Проверка запросов к API

### Testing Stack
- JUnit 4 для структуры тестов
- MockK для мокирования зависимостей
- Kotlinx Coroutines Test для асинхронного тестирования
- Allure для детальной отчетности

### HTTP Testing
- Тестирование различных HTTP методов (GET, POST, PUT, DELETE)
- Проверка status codes (200, 201, 404, 500)
- Валидация JSON ответов
- Тестирование ошибок сети

## Структура тестов

```
Task2_MockWebServerTest.kt
├── setUp() - инициализация MockWebServer
├── tearDown() - очистка ресурсов
├── GET тесты - получение данных
├── POST тесты - создание данных
├── PUT тесты - обновление данных
├── DELETE тесты - удаление данных
└── Error тесты - обработка ошибок
```

## Результат
Полностью реализованы все требования для MockWebServer тестирования:
- 9/9 тестов проходят успешно
- Покрыты все основные HTTP операции
- Добавлена обработка ошибок
- Интегрированы Allure аннотации
- Настроена автоматическая генерация отчетов
