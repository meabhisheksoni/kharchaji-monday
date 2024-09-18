package com.example.kharchaji

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    fun getTodoItems(): Flow<List<TodoItem>> = todoDao.getTodoItems()

    suspend fun insert(todoItem: TodoItem) {
        todoDao.insert(todoItem)
    }

    suspend fun update(todoItem: TodoItem) {
        todoDao.update(todoItem)
    }

    suspend fun delete(todoItem: TodoItem) {
        todoDao.delete(todoItem)
    }
}