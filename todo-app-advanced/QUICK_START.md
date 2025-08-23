# Quick Start Guide

## Домашнее задание: ДЗ 6 - Тестирование сетевого слоя

### Статус: ВЫПОЛНЕНО (16/16 тестов, 100% success)

## Сборка проекта

```bash
# Перейти в корень проекта
cd todo-app-advanced
./gradlew clean build
```

---

## SQLite Mock тестирование (7/7 тестов)

```bash
# Переключиться на ветку
git checkout qa-kotlin-6-1

# Запустить тесты и сгенерировать отчет
cd todo-app-advanced
./gradlew clean
./gradlew testDebugUnitTest --tests "Task1_SQLiteMockTest"
./gradlew allureReport --clean

# Запустить HTTP сервер и открыть отчет
python3 -m http.server 9040 --directory app/build/reports/allure-report/allureReport &
open http://localhost:9040
```

## MockWebServer API тестирование (9/9 тестов)

```bash
# Переключиться на ветку  
git checkout qa-kotlin-6-2

# Запустить тесты и сгенерировать отчет
cd todo-app-advanced
./gradlew clean
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"
./gradlew allureReport --clean

# Запустить HTTP сервер и открыть отчет
python3 -m http.server 9041 --directory app/build/reports/allure-report/allureReport &
open http://localhost:9041
```

## Структура проекта

```
kotlin-network-testing/todo-app-advanced/
├── README.md                    # Полная документация
├── QUICK_START.md              # Этот файл
├── reports/                    # Allure отчеты
│   ├── qa-kotlin-6-1/         # SQLite тесты
│   └── qa-kotlin-6-2/         # MockWebServer тесты
└── app/src/test/              # Тестовый код
    └── homework/              # Основные тесты
        ├── Task1_SQLiteMockTest.kt      (в ветке qa-kotlin-6-1)
        └── Task2_MockWebServerTest.kt   (в ветке qa-kotlin-6-2)
```

## Результаты

| Ветка | Тесты | Статус | Технологии |
|-------|-------|--------|------------|
| qa-kotlin-6-1 | 7/7 | 100% | Room, MockK, Allure |
| qa-kotlin-6-2 | 9/9 | 100% | MockWebServer, Retrofit, Allure |
| ИТОГО | 16/16 | 100% | Full Stack Testing |