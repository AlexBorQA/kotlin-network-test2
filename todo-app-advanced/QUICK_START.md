# ⚡ Quick Start Guide

## 🎯 Домашнее задание: ДЗ 6 - Тестирование сетевого слоя

### 📊 Статус: ✅ ВЫПОЛНЕНО (16/16 тестов, 100% success)

---

## 🔹 SQLite Mock тестирование (7/7 тестов)

```bash
# Перейти в корень проекта
cd todo-app-advanced

# Переключиться на ветку (ОБЯЗАТЕЛЬНО!)
git checkout qa-kotlin-6-1

# Запустить тесты
./gradlew testDebugUnitTest --tests "Task1_SQLiteMockTest"

# Сгенерировать Allure отчет (только в ветке qa-kotlin-6-1!)
./gradlew allureReport --clean

# Запустить HTTP сервер для просмотра отчета
python3 -m http.server 9000 -d app/build/reports/allure-report/allureReport &

# Открыть отчет в браузере
open http://localhost:9000
```

---

## 🔹 MockWebServer API тестирование (9/9 тестов)

```bash
# Перейти в корень проекта
cd todo-app-advanced

# Переключиться на ветку (ОБЯЗАТЕЛЬНО!)
git checkout qa-kotlin-6-2

# Запустить тесты
./gradlew testDebugUnitTest --tests "Task2_MockWebServerTest"

# Сгенерировать Allure отчет (только в ветке qa-kotlin-6-2!)
./gradlew allureReport --clean

# Запустить HTTP сервер для просмотра отчета
python3 -m http.server 9001 -d app/build/reports/allure-report/allureReport &

# Открыть отчет в браузере
open http://localhost:9001
```

---

## 📁 Структура проекта

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

---

## 📊 Результаты

| Ветка | Тесты | Статус | Технологии |
|-------|-------|--------|------------|
| **qa-kotlin-6-1** | 7/7 ✅ | 100% | Room, MockK, Allure |
| **qa-kotlin-6-2** | 9/9 ✅ | 100% | MockWebServer, Retrofit, Allure |
| **ИТОГО** | **16/16** | **100%** | **Full Stack Testing** |

---

*🎉 Задание выполнено полностью! Все требования соблюдены.*

---

## ⚠️ Важные примечания

**Allure отчеты:** Современные браузеры блокируют JavaScript при открытии локальных файлов через `file://`. Поэтому отчеты **ОБЯЗАТЕЛЬНО** нужно открывать через HTTP сервер (команды выше используют порты 9000/9001).
