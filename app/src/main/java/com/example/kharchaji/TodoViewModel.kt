package com.example.kharchaji

//TodoViewModel.kt


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TodoItem(val text: String, var isDone: Boolean = false)

class TodoViewModel : ViewModel() {
    private val _todoItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoItems: StateFlow<List<TodoItem>> = _todoItems

    fun addItem(item: TodoItem) {
        _todoItems.value = listOf(item) + _todoItems.value  // Add new item at the beginning
    }

    fun updateItem(updatedItem: TodoItem) {
        _todoItems.value = _todoItems.value.map {
            if (it.text == updatedItem.text) updatedItem else it
        }
    }

    fun removeItem(item: TodoItem) {
        _todoItems.value = _todoItems.value - item
    }
    fun setAllItemsChecked(checked: Boolean) {
        _todoItems.value = _todoItems.value.map { it.copy(isDone = checked) }
    }
}