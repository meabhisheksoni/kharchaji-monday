package com.example.monday

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

    suspend fun deleteAll() {
        todoDao.deleteAll()
    }

    suspend fun deleteItemById(itemId: Int) {
        todoDao.deleteItemById(itemId)
    }

    suspend fun clearAndLoadTodoItems(items: List<TodoItem>) {
        todoDao.clearAndInsertTodoItems(items)
    }

    // CalculationRecord methods
    val allCalculationRecords: Flow<List<CalculationRecord>> = todoDao.getAllCalculationRecords()

    suspend fun insertCalculationRecord(record: CalculationRecord) {
        todoDao.insertCalculationRecord(record)
    }

    fun getCalculationRecordById(id: Int): kotlinx.coroutines.flow.Flow<CalculationRecord?> {
        return todoDao.getCalculationRecordById(id)
    }

    suspend fun deleteCalculationRecord(record: CalculationRecord) {
        todoDao.deleteCalculationRecord(record)
    }

    suspend fun deleteCalculationRecordById(recordId: Int) {
        todoDao.deleteCalculationRecordById(recordId)
    }

    suspend fun deleteAllCalculationRecords() {
        todoDao.deleteAllCalculationRecords()
    }
}