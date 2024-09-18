
package com.example.kharchaji

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository
    private val _todoItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoItems: StateFlow<List<TodoItem>> = _todoItems

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
        repository.delete(item)
    }

    fun setAllItemsChecked(checked: Boolean) = viewModelScope.launch {
        val currentItems = _todoItems.value
        val updatedItems = currentItems.map { it.copy(isDone = checked) }
        updatedItems.forEach { repository.update(it) }
    }
}