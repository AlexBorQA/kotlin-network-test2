---
<!-- _class: page_route -->
# Маршрут занятия

- Тестирование Room Database
- Тестирование Background Services  
- Тестирование Content Providers
- Тестирование Broadcast Receivers
- Практические находки и решения

---
<!-- _class: page_twocolumn -->
<div class="table-container">
<div class="column-headers">
<div class="column1">
<h1>Цель вебинара</h1>
<h2>К концу занятия вы сможете</h2>
</div>
<div class="column2">
<h1>Смысл</h1>
<h2>Для чего вам это уметь</h2>
</div>
</div>
<div class="table-wrapper">
<table>
<tbody>
<tr><td>Тестировать Room базы данных с in-memory подходом</td><td>Обеспечение корректности работы с локальными данными</td></tr>
<tr><td>Создавать тесты для фоновых сервисов и WorkManager</td><td>Проверка синхронизации и фоновых операций</td></tr>
<tr><td>Тестировать Content Providers и Broadcast Receivers</td><td>Валидация межкомпонентного взаимодействия</td></tr>
</tbody>
</table>
</div>
</div>

---
<!-- _class: page_section -->

<hr>

# Тестирование Room Database

---
<!-- _class: page -->
# Архитектура тестирования Room

```
TestApp Architecture:
├── data/
│   ├── database/
│   │   ├── entity/TodoEntity     # Room Entity
│   │   ├── dao/TodoDao           # DAO интерфейс
│   │   └── TodoDatabase          # Room Database
│   ├── repository/               # Repository слой
│   └── mapper/TodoMapper         # Entity ↔ Domain
├── domain/
│   └── model/Todo               # Domain модель
└── tests/
    ├── TodoDaoTest              # DAO тестирование
    └── TodoRepositoryTest       # Repository тестирование
```

---
<!-- _class: page -->
# Room Entity с полной функциональностью

```kotlin
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date()
)

enum class SyncStatus { LOCAL, SYNCED, PENDING, CONFLICT }
```

---
<!-- _class: page -->
# DAO с продвинутыми запросами

```kotlin
@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY priority DESC, created_at DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE sync_status = :syncStatus")
    suspend fun getTodosBySyncStatus(syncStatus: SyncStatus): List<TodoEntity>
    
    @Query("SELECT * FROM todos WHERE title LIKE '%' || :query || '%'")
    fun searchTodos(query: String): Flow<List<TodoEntity>>
    
    @Transaction
    suspend fun markAsCompleted(id: Long) {
        updateCompletionStatus(id, true, System.currentTimeMillis())
    }
    
    @Query("UPDATE todos SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean, updatedAt: Long)
}
```

---
<!-- _class: page -->
# Настройка тестовой базы данных

```kotlin
@RunWith(AndroidJUnit4::class)
class TodoDaoTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: TodoDatabase
    private lateinit var todoDao: TodoDao
    
    @Before
    fun setup() {
        // In-memory база данных для тестов
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        
        todoDao = database.todoDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
}
```

---
<!-- _class: page -->
# Тестирование CRUD операций

```kotlin
@Test
fun insertTodo_returnsTodoId() = runBlocking {
    // Given
    val todo = TodoEntity(
        title = "Test Todo",
        description = "Test Description",
        priority = Priority.HIGH
    )
    
    // When
    val todoId = todoDao.insertTodo(todo)
    
    // Then
    Assert.assertTrue("Todo ID should be greater than 0", todoId > 0)
}

@Test
fun getTodoById_returnsCorrectTodo() = runBlocking {
    // Given
    val todo = TodoEntity(title = "Test Todo")
    val todoId = todoDao.insertTodo(todo)
    
    // When
    val retrievedTodo = todoDao.getTodoById(todoId)
    
    // Then
    Assert.assertEquals(todo.title, retrievedTodo?.title)
}
```

---
<!-- _class: page -->
# Тестирование сложных запросов

```kotlin
@Test
fun searchTodos_findsMatchingTodos() = runBlocking {
    // Given
    todoDao.insertTodo(TodoEntity(title = "Meeting with client"))
    todoDao.insertTodo(TodoEntity(title = "Buy groceries"))
    todoDao.insertTodo(TodoEntity(title = "Client presentation"))
    
    // When
    val searchResults = todoDao.searchTodos("client").first()
    
    // Then
    Assert.assertEquals("Should find 2 todos with 'client'", 2, searchResults.size)
}

@Test
fun getTodosByPriority_returnsCorrectTodos() = runBlocking {
    // Given
    todoDao.insertTodo(TodoEntity(title = "High Priority", priority = Priority.HIGH))
    todoDao.insertTodo(TodoEntity(title = "Normal Priority", priority = Priority.NORMAL))
    
    // When
    val highPriorityTodos = todoDao.getTodosByPriority(Priority.HIGH).first()
    
    // Then
    Assert.assertEquals(1, highPriorityTodos.size)
    Assert.assertEquals(Priority.HIGH, highPriorityTodos[0].priority)
}
```

---
<!-- _class: page -->
# Тестирование транзакций

```kotlin
@Test
fun updateCompletionStatus_changesCompletionCorrectly() = runBlocking {
    // Given
    val todo = TodoEntity(title = "Test Todo", isCompleted = false)
    val todoId = todoDao.insertTodo(todo)
    val currentTime = System.currentTimeMillis()
    
    // When
    todoDao.updateCompletionStatus(todoId, true, currentTime)
    
    // Then
    val updatedTodo = todoDao.getTodoById(todoId)
    Assert.assertTrue("Todo should be completed", updatedTodo?.isCompleted == true)
}

@Test
fun deleteCompletedTodos_removesOnlyCompletedTodos() = runBlocking {
    // Given
    todoDao.insertTodo(TodoEntity(title = "Active Todo", isCompleted = false))
    todoDao.insertTodo(TodoEntity(title = "Completed Todo", isCompleted = true))
    
    // When
    todoDao.deleteCompletedTodos()
    
    // Then
    val remainingTodos = todoDao.getAllTodos().first()
    Assert.assertEquals(1, remainingTodos.size)
    Assert.assertFalse(remainingTodos[0].isCompleted)
}
```

---
<!-- _class: page_section -->

<hr>

# Тестирование Background Services

---
<!-- _class: page -->
# WorkManager архитектура

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val todoRepository: TodoRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())
            
            val syncResult = todoRepository.syncWithRemote()
            
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
```

---
<!-- _class: page -->
# SyncManager для управления работами

```kotlin
@Singleton
class SyncManager @Inject constructor(
    private val workManager: WorkManager
) {
    fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
            
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
            
        workManager.enqueueUniquePeriodicWork(
            "PeriodicSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}
```

---
<!-- _class: page -->
# Тестирование WorkManager с TestListenableWorkerBuilder

```kotlin
@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    
    private lateinit var context: Context
    private val mockRepository = mockk<TodoRepository>()
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun `syncWorker returns success when sync succeeds`() = runBlocking {
        // Given
        coEvery { mockRepository.syncWithRemote() } returns Result.success(Unit)
        
        // When
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()
        val result = worker.doWork()
        
        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
```

---
<!-- _class: page -->
# Тестирование SyncManager

```kotlin
class SyncManagerTest {
    
    private val mockWorkManager = mockk<WorkManager>(relaxed = true)
    private lateinit var syncManager: SyncManager
    
    @Before
    fun setup() {
        syncManager = SyncManager(mockWorkManager)
    }
    
    @Test
    fun `startPeriodicSync enqueues unique periodic work`() {
        // When
        syncManager.startPeriodicSync()
        
        // Then
        verify {
            mockWorkManager.enqueueUniquePeriodicWork(
                "PeriodicSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                any<PeriodicWorkRequest>()
            )
        }
    }
}
```

---
<!-- _class: page -->
# WorkManager Integration Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class SyncWorkManagerIntegrationTest {
    
    private lateinit var workManager: WorkManager
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
            
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }
    
    @Test
    fun `periodic sync work is scheduled correctly`() {
        // Given
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .build()
            
        // When
        workManager.enqueue(request)
        
        // Then
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
    }
}
```

---
<!-- _class: page_section -->

<hr>

# Тестирование Content Providers

---
<!-- _class: page -->
# TodoContentProvider архитектура

```kotlin
class TodoContentProvider : ContentProvider() {
    
    companion object {
        const val AUTHORITY = "com.example.todoapp.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/todos")
    }
    
    override fun query(uri: Uri, projection: Array<String>?, 
                      selection: String?, selectionArgs: Array<String>?, 
                      sortOrder: String?): Cursor? {
        return when (uriMatcher.match(uri)) {
            TODOS -> {
                // Возвращаем курсор с данными всех задач
                createCursorFromTodos(getAllTodosFromDatabase())
            }
            TODO_ID -> {
                // Возвращаем конкретную задачу по ID
                val todoId = uri.lastPathSegment?.toLongOrNull()
                createCursorFromTodos(listOfNotNull(getTodoById(todoId)))
            }
            else -> null
        }
    }
}
```

---
<!-- _class: page -->
# Настройка тестов для Content Provider

```kotlin
@RunWith(AndroidJUnit4::class)
class TodoContentProviderTest {
    
    @get:Rule
    val providerRule = ProviderTestRule.Builder(
        TodoContentProvider::class.java,
        TodoContentProvider.AUTHORITY
    ).build()
    
    private lateinit var contentUri: Uri
    
    @Before
    fun setup() {
        contentUri = Uri.parse("content://${TodoContentProvider.AUTHORITY}/todos")
    }
    
    @Test
    fun testProviderCreation() {
        val provider = providerRule.provider
        assertNotNull("Provider should be created", provider)
        assertTrue("Provider should be TodoContentProvider", 
                  provider is TodoContentProvider)
    }
}
```

---
<!-- _class: page -->
# Тестирование CRUD операций через ContentResolver

```kotlin
@Test
fun testInsertTodo() {
    // Given
    val values = ContentValues().apply {
        put("title", "Test Todo")
        put("description", "Test Description")
        put("is_completed", false)
        put("category", "Work")
    }
    
    // When
    val resultUri = providerRule.resolver.insert(contentUri, values)
    
    // Then
    assertNotNull("Insert should return URI", resultUri)
    assertTrue("URI should contain todo ID", resultUri.toString().contains("todos/"))
}

@Test
fun testQuerySpecificTodo() {
    // Given
    val insertedUri = insertTestTodo("Specific Todo", "Specific Description")
    
    // When
    val cursor = providerRule.resolver.query(insertedUri!!, null, null, null, null)
    
    // Then
    cursor?.use {
        if (it.moveToFirst()) {
            val title = it.getString(it.getColumnIndex("title"))
            assertEquals("Specific Todo", title)
        }
    }
}
```

---
<!-- _class: page -->
# Тестирование MIME типов и URI матчинга

```kotlin
@Test
fun testGetType() {
    // When
    val todoListType = providerRule.provider.getType(contentUri)
    val todoItemUri = Uri.withAppendedPath(contentUri, "1")
    val todoItemType = providerRule.provider.getType(todoItemUri)
    
    // Then
    assertEquals(
        "vnd.android.cursor.dir/vnd.${TodoContentProvider.AUTHORITY}.todos",
        todoListType
    )
    assertEquals(
        "vnd.android.cursor.item/vnd.${TodoContentProvider.AUTHORITY}.todos",
        todoItemType
    )
}

@Test
fun testUpdateTodo() {
    // Given
    val insertedUri = insertTestTodo("Original Title", "Original Description")
    val updateValues = ContentValues().apply {
        put("title", "Updated Title")
        put("is_completed", true)
    }
    
    // When
    val updatedRows = providerRule.resolver.update(insertedUri!!, updateValues, null, null)
    
    // Then
    assertEquals("Should update 1 row", 1, updatedRows)
}
```

---
<!-- _class: page_section -->

<hr>

# Тестирование Broadcast Receivers

---
<!-- _class: page -->
# BootReceiver для автозапуска

```kotlin
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var syncManager: SyncManager
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Запускаем периодическую синхронизацию после загрузки устройства
            syncManager.startPeriodicSync()
        }
    }
}

// В манифесте:
<receiver android:name=".receiver.BootReceiver" android:enabled="true" android:exported="true">
    <intent-filter android:priority="1000">
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---
<!-- _class: page -->
# NetworkReceiver для реакции на сеть

```kotlin
@AndroidEntryPoint
class NetworkReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var syncManager: SyncManager
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            if (isNetworkAvailable(context)) {
                syncManager.startOneTimeSync()
            }
        }
    }
    
    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }
}
```

---
<!-- _class: page -->
# Тестирование BootReceiver

```kotlin
@RunWith(AndroidJUnit4::class)
class BootReceiverTest {
    
    private lateinit var receiver: BootReceiver
    private lateinit var context: Context
    private val mockSyncManager = mockk<SyncManager>(relaxed = true)
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        receiver = BootReceiver()
        // В реальном тесте использовали бы Hilt testing
        receiver.syncManager = mockSyncManager
    }
    
    @Test
    fun `onReceive with BOOT_COMPLETED starts periodic sync`() {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify { mockSyncManager.startPeriodicSync() }
    }
}
```

---
<!-- _class: page -->
# Тестирование NetworkReceiver

```kotlin
class NetworkReceiverTest {
    
    private lateinit var receiver: NetworkReceiver
    private lateinit var context: Context
    private val mockSyncManager = mockk<SyncManager>(relaxed = true)
    
    @Test
    fun `onReceive with network available starts one time sync`() {
        // Given
        val intent = Intent(ConnectivityManager.CONNECTIVITY_ACTION)
        // Мокаем доступность сети
        every { receiver.isNetworkAvailable(any()) } returns true
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify { mockSyncManager.startOneTimeSync() }
    }
    
    @Test
    fun `onReceive with network unavailable does not start sync`() {
        // Given
        val intent = Intent(ConnectivityManager.CONNECTIVITY_ACTION)
        every { receiver.isNetworkAvailable(any()) } returns false
        
        // When
        receiver.onReceive(context, intent)
        
        // Then
        verify(exactly = 0) { mockSyncManager.startOneTimeSync() }
    }
}
```

---
<!-- _class: page -->
# Интеграционное тестирование Broadcast Receivers

```kotlin
@RunWith(AndroidJUnit4::class)
class BroadcastReceiverIntegrationTest {
    
    @Test
    fun `boot receiver is registered and receives broadcasts`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        
        // When
        val receiverInfo = context.packageManager.queryBroadcastReceivers(
            intent, 
            PackageManager.GET_RESOLVED_FILTER
        )
        
        // Then
        assertTrue("Boot receiver should be registered", 
                  receiverInfo.any { it.activityInfo.name.contains("BootReceiver") })
    }
    
    @Test
    fun `network receiver responds to connectivity changes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = NetworkReceiver()
        val intent = Intent(ConnectivityManager.CONNECTIVITY_ACTION)
        
        // Тестируем что receiver не падает при получении intent
        assertDoesNotThrow {
            receiver.onReceive(context, intent)
        }
    }
}
```

---
<!-- _class: page_section -->

<hr>

# Интеграционное тестирование

---
<!-- _class: page -->
# Repository интеграционные тесты

```kotlin
class TodoRepositoryImplTest {
    
    private val mockTodoDao = mockk<TodoDao>(relaxed = true)
    private lateinit var repository: TodoRepositoryImpl
    
    @Test
    fun `markAsCompleted updates completion and sync status`() = runBlocking {
        // Given
        val todoId = 1L
        coEvery { mockTodoDao.markAsCompleted(any()) } just Runs
        coEvery { mockTodoDao.updateSyncStatus(any(), any()) } just Runs
        
        // When
        repository.markAsCompleted(todoId)
        
        // Then
        coVerify { mockTodoDao.markAsCompleted(todoId) }
        coVerify { mockTodoDao.updateSyncStatus(todoId, SyncStatus.PENDING) }
    }
    
    @Test
    fun `syncWithRemote marks pending todos as synced on success`() = runBlocking {
        // Given
        val pendingTodos = listOf(
            TodoEntity(id = 1, title = "Todo 1", syncStatus = SyncStatus.PENDING)
        )
        coEvery { mockTodoDao.getTodosBySyncStatus(SyncStatus.PENDING) } returns pendingTodos
        
        // When
        val result = repository.syncWithRemote()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockTodoDao.updateSyncStatus(1, SyncStatus.SYNCED) }
    }
}
```

---
<!-- _class: page -->
# End-to-End тестирование синхронизации

```kotlin
@RunWith(AndroidJUnit4::class)
class SyncIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var todoRepository: TodoRepository
    
    @Inject 
    lateinit var syncManager: SyncManager
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun `full sync workflow works correctly`() = runBlocking {
        // Given - создаем задачу
        val todoId = todoRepository.insertTodo(
            Todo(title = "Test Todo", description = "Test Description")
        )
        
        // When - помечаем как завершенную (должна стать PENDING)
        todoRepository.markAsCompleted(todoId)
        
        // And - запускаем синхронизацию
        val syncResult = todoRepository.syncWithRemote()
        
        // Then - проверяем что задача синхронизирована
        assertTrue("Sync should succeed", syncResult.isSuccess)
        
        val todo = todoRepository.getTodoById(todoId)
        assertEquals("Todo should be synced", TodoSyncStatus.SYNCED, todo?.syncStatus)
    }
}
```

---
<!-- _class: page -->
# Тестирование жизненного цикла приложения

```kotlin
@RunWith(AndroidJUnit4::class)
class AppLifecycleIntegrationTest {
    
    @Test
    fun `app starts sync on boot completed`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockSyncManager = mockk<SyncManager>(relaxed = true)
        val receiver = BootReceiver().apply {
            syncManager = mockSyncManager
        }
        
        // When - симулируем загрузку устройства
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, bootIntent)
        
        // Then
        verify { mockSyncManager.startPeriodicSync() }
    }
    
    @Test
    fun `network change triggers sync when connected`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockSyncManager = mockk<SyncManager>(relaxed = true)
        val receiver = NetworkReceiver().apply {
            syncManager = mockSyncManager
        }
        
        // When - симулируем появление сети
        val networkIntent = Intent(ConnectivityManager.CONNECTIVITY_ACTION)
        receiver.onReceive(context, networkIntent)
        
        // Then - проверяем запуск синхронизации (если сеть доступна)
        // В реальном тесте нужно мокать ConnectivityManager
    }
}
```

---
<!-- _class: page -->
# Производительность и стресс-тестирование

```kotlin
class PerformanceTest {
    
    @Test
    fun `database handles large number of todos efficiently`() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        // When - вставляем 1000 задач
        val todos = (1..1000).map { 
            TodoEntity(title = "Todo $it", description = "Description $it")
        }
        todoDao.insertTodos(todos)
        
        // Then - проверяем время выполнения
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue("Batch insert should complete in under 5 seconds", duration < 5000)
        
        // And - проверяем корректность данных
        val allTodos = todoDao.getAllTodos().first()
        assertEquals("All todos should be inserted", 1000, allTodos.size)
    }
    
    @Test
    fun `search performance with large dataset`() = runBlocking {
        // Given - большой набор данных
        repeat(10000) { i ->
            todoDao.insertTodo(TodoEntity(title = "Task $i", description = "Description $i"))
        }
        
        val startTime = System.currentTimeMillis()
        
        // When - выполняем поиск
        val results = todoDao.searchTodos("Task 5").first()
        
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Search should complete quickly", duration < 1000)
        assertTrue("Should find multiple results", results.isNotEmpty())
    }
}
```

---
<!-- _class: page_section -->

<hr>

# Практические находки и решения

---
<!-- _class: page -->
# Проблемы с Turbine тестированием Flow

**Проблема**: `TurbineTimeoutCancellationException` в тестах Flow
```kotlin
viewModel.uiEvent.test {
    viewModel.createTodo("New Todo")
    // TimeoutCancellationException: Timed out waiting for 1000 ms
    val event = awaitItem()
}
```

**Решение**: Добавить timeout и правильную настройку диспатчеров
```kotlin
viewModel.uiEvent.test(timeout = 3.seconds) {
    viewModel.createTodo("New Todo")
    testDispatcher.scheduler.advanceUntilIdle()
    
    val event = awaitItem()
    assertThat(event).isInstanceOf(UiEvent.ShowSnackbar::class.java)
    
    cancelAndIgnoreRemainingEvents()
}
```

---
<!-- _class: page -->
# Правильная настройка TestDispatcher для Turbine

**StandardTestDispatcher** (рекомендуется):
```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()

private val testDispatcher = StandardTestDispatcher()

@Before
fun setup() {
    Dispatchers.setMain(testDispatcher)
    viewModel = TodoViewModel(repository, testDispatcher)
}
```

**Важно**: Всегда используйте `testDispatcher.scheduler.advanceUntilIdle()` для продвижения времени

**UnconfinedTestDispatcher** - избегайте для ViewModel тестов с Turbine

---
<!-- _class: page -->
# WorkManager: проблема "рассинхронизации коробки и мотора"

**Проблема**: SyncManager и тесты используют разные WorkManager instances
```kotlin
// SyncManager получает продакшн WorkManager
class SyncManager @Inject constructor(
    private val workManager: WorkManager // ← продакшн instance
)

// Тест создаёт свой тестовый WorkManager
WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
val testWorkManager = WorkManager.getInstance(context) // ← тестовый instance
```

**Результат**: Тесты не видят работы, созданные через SyncManager

---
<!-- _class: page -->
# Решение: правильный TestWorkManagerModule

**Создайте TestWorkManagerModule**:
```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [WorkManagerModule::class]
)
object TestWorkManagerModule {
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
            
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        return WorkManager.getInstance(context)
    }
}
```

---
<!-- _class: page -->
# Упрощённые инструментальные тесты WorkManager

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SyncManagerInstrumentedTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var syncManager: SyncManager
    
    @Inject
    lateinit var workManager: WorkManager
    
    private lateinit var driver: TestDriver
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        hiltRule.inject()
        driver = WorkManagerTestInitHelper.getTestDriver(context)!!
    }
    
    @Test
    fun testStartPeriodicSync() {
        syncManager.startPeriodicSync()
        
        val workInfos = workManager.getWorkInfosForUniqueWork("PeriodicSyncWork").get()
        assertTrue(workInfos.isNotEmpty())
    }
}
```

---
<!-- _class: page -->
# ForegroundService в тестах: избегаем InvalidForegroundServiceTypeException

**Проблема**: Worker падает с `InvalidForegroundServiceTypeException` в тестах

**Решение**: Детектор тестового режима
```kotlin
override suspend fun doWork(): Result {
    return try {
        if (!isInTestMode()) {
            setForeground(createForegroundInfo())
        }
        
        // Основная логика работы
        syncRepository.syncWithRemote()
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}

private fun isInTestMode(): Boolean {
    return try {
        Class.forName("androidx.work.testing.WorkManagerTestInitHelper")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}
```

---
<!-- _class: page -->
# Важные разрешения для ForegroundService

В `AndroidManifest.xml` для работы `setForeground()`:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:directBootAware="false"
    android:enabled="@bool/enable_system_foreground_service_default"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

**Без этих разрешений** WorkManager не сможет установить foreground service

---
<!-- _class: page -->
# Отладочные советы для Turbine

1. **Логирование состояний Flow**:
```kotlin
viewModel.uiState
    .onEach { println("State: $it") }
    .test(timeout = 3.seconds) {
        // ваши тесты
    }
```

2. **Проверка начального состояния**:
```kotlin
viewModel.uiState.test(timeout = 3.seconds) {
    skipItems(1) // Пропускаем начальное состояние если нужно
    
    viewModel.loadTodos()
    testDispatcher.scheduler.advanceUntilIdle()
    
    val loadingState = awaitItem()
    // assertions
}
```

3. **Всегда используйте `cancelAndIgnoreRemainingEvents()`**

---
<!-- _class: page_question -->
# Вопросы?

<hr>

![](https://raw.githubusercontent.com/dzolotov/otus/refs/heads/main/theme/questions.png)

---
<!-- _class: page -->
# Полезные материалы

- **Room Testing Guide**: https://developer.android.com/training/data-storage/room/testing-db
- **WorkManager Testing**: https://developer.android.com/guide/background/testing/persistent/integration-testing
- **Content Provider Testing**: https://developer.android.com/guide/topics/providers/content-provider-testing
- **Broadcast Receiver Testing**: https://developer.android.com/guide/components/broadcasts#testing
- **Android Testing Fundamentals**: https://developer.android.com/training/testing/fundamentals
- **Hilt Testing Guide**: https://dagger.dev/hilt/testing
- **Turbine Testing Library**: https://github.com/cashapp/turbine
- **Coroutines Testing**: https://developer.android.com/kotlin/coroutines/test

**Практические примеры:**
- `todo-app-advanced/` - Room, WorkManager, Broadcast Receivers
- `todo-app-retrofit/` - Turbine Flow testing, ViewModel testing

---
<!-- _class: page_twocolumn -->
<div class="fullcolumn">
<h1>Итоговые цели</h1>
<h2>Что вы теперь умеете</h2>

<div class="table-wrapper">
<table>
<tbody>
<tr><td>Тестировать Room базы данных с in-memory подходом</td><td>Обеспечение корректности работы с локальными данными</td></tr>
<tr><td>Создавать тесты для фоновых сервисов и WorkManager</td><td>Проверка синхронизации и фоновых операций</td></tr>
<tr><td>Тестировать Content Providers и Broadcast Receivers</td><td>Валидация межкомпонентного взаимодействия</td></tr>
</tbody>
</table>
</div>
</div>