package com.example.kharchaji

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun MainScreen(todoViewModel: TodoViewModel, onShareClick: () -> Unit) {
    var showListScreen by remember { mutableStateOf(false) }

    val todoItems by todoViewModel.todoItems.collectAsState(initial = emptyList())
    val totalItems = todoItems.size
    val checkedItems = todoItems.count { it.isDone }
    val totalSum = todoItems.sumOf { parsePrice(it.text) }
    val checkedSum = todoItems.filter { it.isDone }.sumOf { parsePrice(it.text) }
    var masterCheckboxState by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showListScreen) {
        // List Screen
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)

            ) {
                IconButton(onClick = { showListScreen = false }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = "Monday, April 24",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE6E0F8), shape = RoundedCornerShape(100.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "      ($totalItems) Total sum = Rs $totalSum\n\t                                                     ($checkedItems) Checked sum = Rs $checkedSum",
                        color = Color.Black,
                        style = TextStyle(fontSize = 13.5.sp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Checkbox(
                    checked = masterCheckboxState,
                    onCheckedChange = { checked ->
                        masterCheckboxState = checked
                        todoViewModel.setAllItemsChecked(checked)
                    },
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(
                    onClick = {
                        shareExpensesList(context, todoItems, checkedSum)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share expenses")
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(todoItems) { item ->
                        TodoItemRow(
                            item = item,
                            onCheckedChange = { checked ->
                                todoViewModel.updateItem(item.copy(isDone = checked))
                            },
                            onRemoveClick = {
                                todoViewModel.removeItem(item)
                            },
                            masterCheckboxState = masterCheckboxState
                        )
                    }
                }
            }
        }
    } else {
        // Entry Form Screen
        EntryFormScreen(onNextClick = { showListScreen = true }, todoViewModel = todoViewModel)
    }
}


@Composable
fun TodoItemRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit,
    masterCheckboxState: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = masterCheckboxState || item.isDone,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))

        val (name, quantity, price) = parseItemText(item.text)

        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            if (quantity != null) {
                Text(
                    text = quantity,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Rs $price",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 8.dp)
        )

        IconButton(onClick = onRemoveClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}