package com.example.monday

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_table ORDER BY id DESC")
    fun getTodoItems(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(todoItem: TodoItem)

    @Update
    suspend fun update(todoItem: TodoItem)

    @Delete
    suspend fun delete(todoItem: TodoItem)

    @Query("DELETE FROM todo_table")
    suspend fun deleteAll()

    @Query("DELETE FROM todo_table WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Int)

    @Transaction
    suspend fun clearAndInsertTodoItems(items: List<TodoItem>) {
        deleteAll() // Clear existing todo items
        items.forEach { insert(it) } // Insert new items
    }

    // CalculationRecord methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculationRecord(record: CalculationRecord)

    @Query("SELECT * FROM calculation_records ORDER BY timestamp DESC")
    fun getAllCalculationRecords(): kotlinx.coroutines.flow.Flow<List<CalculationRecord>>

    @Query("SELECT * FROM calculation_records WHERE id = :id")
    fun getCalculationRecordById(id: Int): kotlinx.coroutines.flow.Flow<CalculationRecord?>

    @Delete
    suspend fun deleteCalculationRecord(record: CalculationRecord)

    @Query("DELETE FROM calculation_records WHERE id = :recordId")
    suspend fun deleteCalculationRecordById(recordId: Int)

    @Query("DELETE FROM calculation_records")
    suspend fun deleteAllCalculationRecords()
}