package com.example.todoapp.testing

import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.database.dao.TodoDao
import io.mockk.coVerify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*

/**
 * Extension функции для упрощения тестов
 * Убирают дублирование кода и делают тесты читаемее
 */

// ========== Flow Extensions ==========

/**
 * Тестирует Flow и проверяет ожидаемое значение
 */
suspend fun <T> Flow<T>.testAndExpect(expected: T) {
    val actual = this.first()
    assertEquals(expected, actual)
}

/**
 * Тестирует размер списка из Flow
 */
suspend fun <T> Flow<List<T>>.testSize(expectedSize: Int) {
    val actual = this.first()
    assertEquals(expectedSize, actual.size)
}

/**
 * Тестирует что Flow содержит элемент
 */
suspend fun <T> Flow<List<T>>.testContains(element: T) {
    val actual = this.first()
    assertTrue("List should contain $element", actual.contains(element))
}

/**
 * Тестирует что Flow пустой
 */
suspend fun <T> Flow<List<T>>.testEmpty() {
    val actual = this.first()
    assertTrue("List should be empty", actual.isEmpty())
}

/**
 * Тестирует что Flow не пустой
 */
suspend fun <T> Flow<List<T>>.testNotEmpty() {
    val actual = this.first()
    assertTrue("List should not be empty", actual.isNotEmpty())
}

// ========== Verification Extensions ==========

/**
 * Проверяет что insertTodo был вызван определенное количество раз
 */
fun TodoDao.verifyInsertCalled(times: Int = 1) {
    coVerify(exactly = times) { insertTodo(any()) }
}

/**
 * Проверяет что updateTodo был вызван определенное количество раз
 */
fun TodoDao.verifyUpdateCalled(times: Int = 1) {
    coVerify(exactly = times) { updateTodo(any()) }
}

/**
 * Проверяет что deleteTodo был вызван определенное количество раз
 */
fun TodoDao.verifyDeleteCalled(times: Int = 1) {
    coVerify(exactly = times) { deleteTodo(any()) }
}

/**
 * Проверяет что createTodo API был вызван определенное количество раз
 */
fun TodoApiService.verifyCreateCalled(times: Int = 1) {
    coVerify(exactly = times) { createTodo(any()) }
}

/**
 * Проверяет что updateTodo API был вызван определенное количество раз
 */
fun TodoApiService.verifyUpdateCalled(times: Int = 1) {
    coVerify(exactly = times) { updateTodo(any(), any()) }
}

/**
 * Проверяет что deleteTodo API был вызван определенное количество раз
 */
fun TodoApiService.verifyDeleteCalled(times: Int = 1) {
    coVerify(exactly = times) { deleteTodo(any()) }
}

/**
 * Проверяет что getAllTodos API был вызван определенное количество раз
 */
fun TodoApiService.verifyGetAllCalled(times: Int = 1) {
    coVerify(exactly = times) { getAllTodos() }
}

/**
 * Проверяет что getTodoById API был вызван определенное количество раз
 */
fun TodoApiService.verifyGetByIdCalled(times: Int = 1) {
    coVerify(exactly = times) { getTodoById(any()) }
}

/**
 * Проверяет что syncTodos API был вызван определенное количество раз
 */
fun TodoApiService.verifySyncCalled(times: Int = 1) {
    coVerify(exactly = times) { syncTodos(any()) }
}
