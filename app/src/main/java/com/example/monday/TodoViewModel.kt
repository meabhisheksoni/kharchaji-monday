package com.example.monday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository
    private val _todoItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoItems: StateFlow<List<TodoItem>> = _todoItems

    private val _undoableDeletedItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val undoableDeletedItems: StateFlow<List<TodoItem>> = _undoableDeletedItems

    init {
        val todoDao = AppDatabase.getDatabase(application).todoDao()
        repository = TodoRepository(todoDao)
        viewModelScope.launch {
            repository.getTodoItems().collectLatest { items ->
                _todoItems.value = items
            }
        }
    }

    fun addItem(item: TodoItem) = viewModelScope.launch {
        repository.insert(item)
    }

    fun updateItem(updatedItem: TodoItem) = viewModelScope.launch {
        repository.update(updatedItem)
    }

    fun removeItem(item: TodoItem) = viewModelScope.launch {
        _undoableDeletedItems.value = listOf(item) + _undoableDeletedItems.value
        repository.delete(item)
    }

    fun deleteSelectedItemsAndEnableUndo(itemsToDelete: List<TodoItem>) = viewModelScope.launch {
        if (itemsToDelete.isEmpty()) return@launch
        _undoableDeletedItems.value = itemsToDelete.reversed() + _undoableDeletedItems.value
        itemsToDelete.forEach { repository.delete(it) }
    }

    fun setAllItemsChecked(checked: Boolean) = viewModelScope.launch {
        val currentItems = _todoItems.value
        val updatedItems = currentItems.map { it.copy(isDone = checked) }
        updatedItems.forEach { repository.update(it) }
    }

    fun deleteAllItems() = viewModelScope.launch {
        repository.deleteAll()
        _undoableDeletedItems.value = emptyList()
    }

    fun deleteItemById(itemId: Int) = viewModelScope.launch {
        repository.deleteItemById(itemId)
        _undoableDeletedItems.value = emptyList()
    }

    fun loadRecordItemsAsCurrentExpenses(recordItems: List<RecordItem>) = viewModelScope.launch {
        val newTodoItems = recordItems.map {
            TodoItem(
                text = recordItemToTodoItemText(it),
                isDone = it.isChecked
            )
        }
        repository.clearAndLoadTodoItems(newTodoItems)
        _undoableDeletedItems.value = emptyList()
    }

    fun undoLastDelete() = viewModelScope.launch {
        if (_undoableDeletedItems.value.isNotEmpty()) {
            val itemToRestore = _undoableDeletedItems.value.first()
            repository.insert(itemToRestore)
            _undoableDeletedItems.value = _undoableDeletedItems.value.drop(1)
        }
    }

    fun clearLastDeletedItem() = viewModelScope.launch {
        _undoableDeletedItems.value = emptyList()
    }

    // CalculationRecord methods
    val allCalculationRecords: kotlinx.coroutines.flow.StateFlow<List<CalculationRecord>> = repository.allCalculationRecords
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertCalculationRecord(record: CalculationRecord) = viewModelScope.launch {
        repository.insertCalculationRecord(record)
    }

    fun getCalculationRecordById(id: Int): kotlinx.coroutines.flow.StateFlow<CalculationRecord?> {
        return repository.getCalculationRecordById(id)
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)
    }

    fun deleteCalculationRecord(record: CalculationRecord) = viewModelScope.launch {
        repository.deleteCalculationRecord(record)
    }

    fun deleteCalculationRecordById(recordId: Int) = viewModelScope.launch {
        repository.deleteCalculationRecordById(recordId)
    }

    fun deleteAllCalculationRecords() = viewModelScope.launch {
        repository.deleteAllCalculationRecords()
    }
}