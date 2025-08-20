# Руководство по использованию IdlingResource в проекте

## Обзор

IdlingResource - это механизм Espresso для синхронизации тестов с асинхронными операциями. В нашем проекте IdlingResource интегрирован через Dependency Injection для автоматического отслеживания сетевых запросов и операций с базой данных.

## Архитектура

### 1. IdlingResourceProvider

Интерфейс, который абстрагирует работу с IdlingResource:

```kotlin
interface IdlingResourceProvider {
    fun increment(reason: String = "")
    fun decrement(reason: String = "")
    fun isIdleNow(): Boolean
}
```

### 2. Реализации

#### Продакшн (NoOpIdlingResourceProvider)
- Пустая реализация, которая ничего не делает
- Используется в продакшн сборке для минимального влияния на производительность

#### Тестовая (TestIdlingResourceProvider)
- Использует CountingIdlingResource от Espresso
- Отслеживает количество активных операций
- Логирует все increment/decrement для отладки

### 3. Интеграция через DI

#### Продакшн модуль
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object IdlingModule {
    @Provides
    @Singleton
    fun provideIdlingResourceProvider(): IdlingResourceProvider {
        return NoOpIdlingResourceProvider()
    }
}
```

#### Тестовый модуль
```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [IdlingModule::class]
)
object TestIdlingModule {
    @Provides
    @Singleton
    fun provideIdlingResourceProvider(): IdlingResourceProvider {
        return TestIdlingResourceProvider()
    }
}
```

## Где используется IdlingResource

### 1. Repository (CachedTodoRepository)
- `syncWithRemote()` - полная синхронизация
- `uploadPendingChanges()` - загрузка изменений на сервер
- `downloadRemoteChanges()` - загрузка изменений с сервера

### 2. ViewModel (TodoViewModel)
- `syncTodos()` - запуск синхронизации из UI

## Использование в тестах

### Настройка теста

```kotlin
@HiltAndroidTest
class MyIntegrationTest {
    
    @Inject
    lateinit var idlingResourceProvider: TestIdlingResourceProvider
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Регистрируем IdlingResource
        IdlingRegistry.getInstance().register(
            idlingResourceProvider.getIdlingResource()
        )
    }
    
    @After
    fun tearDown() {
        // Отменяем регистрацию
        IdlingRegistry.getInstance().unregister(
            idlingResourceProvider.getIdlingResource()
        )
    }
}
```

### Пример теста с автоматическим ожиданием

```kotlin
@Test
fun syncButton_triggersSync_andUpdatesUI() {
    // Given - настраиваем MockWebServer
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody(gson.toJson(mockTodos))
    )
    
    // When - нажимаем кнопку синхронизации
    composeTestRule.onNodeWithContentDescription("Sync")
        .performClick()
    
    // Then - тест автоматически ждёт завершения синхронизации
    composeTestRule.onNodeWithText("Test Todo from Server")
        .assertIsDisplayed()
}
```

## Преимущества подхода

1. **Автоматическая синхронизация** - тесты автоматически ждут завершения асинхронных операций
2. **Отсутствие Thread.sleep()** - не нужны искусственные задержки
3. **Надёжность** - тесты не падают из-за таймингов
4. **Отладка** - логирование всех increment/decrement помогает понять, какие операции выполняются
5. **Производительность** - в продакшн коде нулевые накладные расходы

## Отладка

Если тесты зависают, проверьте:

1. **Парность вызовов** - каждому increment должен соответствовать decrement
2. **Логи** - TestIdlingResourceProvider логирует все операции
3. **Finally блоки** - убедитесь, что decrement вызывается в finally блоках

### Пример правильного использования

```kotlin
suspend fun performNetworkOperation() {
    idlingResource.increment("networkOperation")
    try {
        // Выполняем операцию
        val result = apiService.getData()
        processResult(result)
    } finally {
        idlingResource.decrement("networkOperation")
    }
}
```

## Типичные ошибки

### 1. Забытый decrement
```kotlin
// ❌ Неправильно
fun badExample() {
    idlingResource.increment("operation")
    performOperation() // Если выбросит исключение, decrement не вызовется
    idlingResource.decrement("operation")
}

// ✅ Правильно
fun goodExample() {
    idlingResource.increment("operation")
    try {
        performOperation()
    } finally {
        idlingResource.decrement("operation")
    }
}
```

### 2. Множественные increment без decrement
```kotlin
// ❌ Неправильно
repeat(5) {
    idlingResource.increment("batch")
    launchOperation()
}
idlingResource.decrement("batch") // Только один decrement!

// ✅ Правильно
repeat(5) {
    idlingResource.increment("batch-$it")
    launchOperation {
        idlingResource.decrement("batch-$it")
    }
}
```

## Пример использования в проекте

Полный пример интеграционных тестов с IdlingResource можно найти в файле:
`app/src/androidTest/java/com/example/todoapp/integration/TodoSyncWithIdlingTest.kt`

Этот файл демонстрирует:
- Настройку IdlingResource для тестов
- Тестирование загрузки данных с автоматическим ожиданием
- Тестирование создания todo с синхронизацией
- Тестирование обработки сетевых ошибок
- Тестирование множественных параллельных операций

## Полезные ссылки

- [Espresso IdlingResource](https://developer.android.com/training/testing/espresso/idling-resource)
- [Testing asynchronous operations](https://developer.android.com/training/testing/espresso/idling-resource#example)
- [Hilt testing guide](https://developer.android.com/training/dependency-injection/hilt-testing)