package com.example.kharchaji

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.kharchaji.TodoItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import android.view.ViewGroup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.widget.FrameLayout
import androidx.compose.foundation.shape.CircleShape
import androidx.core.view.doOnAttach


import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kharchaji.ui.theme.KharchajiTheme
import java.io.File
import java.io.FileOutputStream

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KharchajiTheme {
                TodoApp()
            }
        }
    }
}


@Composable
fun TodoApp(todoViewModel: TodoViewModel = viewModel()) {
    var showShareScreen by remember { mutableStateOf(false) }

    if (showShareScreen) {
        ShareScreen(
            todoItems = todoViewModel.todoItems.collectAsState(initial = emptyList()).value,
            totalSum = todoViewModel.todoItems.collectAsState(initial = emptyList()).value.sumOf { parsePrice(it.text) },
            onDismiss = { showShareScreen = false }
        )
    } else {
        MainScreen(
            todoViewModel = todoViewModel,
            onShareClick = { showShareScreen = true }
        )
    }
}



@Composable
fun EntryFormScreen(onNextClick: () -> Unit, todoViewModel: TodoViewModel) {
    var newItemText by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    val predefinedQuantities = (1..20).map { "$it" } + listOf("250g", "500g", "1kg", "1.5kg", "2kg")
    val taskFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            TextField(
                value = newItemText,
                onValueChange = { text -> newItemText = text },
                placeholder = { Text("Item name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(taskFocus),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { priceFocus.requestFocus() })
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = itemPrice,
                onValueChange = { text -> itemPrice = text },
                placeholder = { Text("Rs") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(priceFocus),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { /* Handle done action */ })
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Quick Quantity")
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("250g", "500g", "1kg", "1.5Kg", "2kg")) { quantity ->
                    QuantityChip(
                        quantity = quantity,
                        isSelected = quantity == selectedQuantity,
                        onSelect = { selectedQuantity = quantity },
                        color = Color.Red
                    )
                }
            }
            // Keypad for amount (simplified for example)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items((1..20).map { "$it" }) { quantity ->
                    QuantityChip(
                        quantity = quantity,
                        isSelected = quantity == selectedQuantity,
                        onSelect = { selectedQuantity = quantity },
                        color = Color.Green
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (newItemText.isNotBlank() && itemPrice.isNotBlank()) {
                        val itemText = if (selectedQuantity.isNotEmpty()) {
                            "$newItemText ($selectedQuantity) - Rs $itemPrice"
                        } else {
                            "$newItemText - Rs $itemPrice"
                        }
                        todoViewModel.addItem(TodoItem(text = itemText))
                        newItemText = ""
                        itemPrice = ""
                        selectedQuantity = ""
                        // Request focus on the item name text field
                        taskFocus.requestFocus()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Expense")
            }
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(onClick = onNextClick) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
            }
        }
    }
}




fun parseItemText(text: String): Triple<String, String?, String> {
    val parts = text.split(" - ")
    val nameAndQuantity = parts[0]
    val price = parts[1].replace("Rs ", "")

    val quantityMatch = Regex("""\((.*?)\)""").find(nameAndQuantity)
    val quantity = quantityMatch?.groupValues?.get(1)
    val name = nameAndQuantity.replace("""\s*\(.*?\)""".toRegex(), "")

    return Triple(name, quantity, price)
}

fun parsePrice(text: String): Double {
    val priceString = text.split(" - ").last().replace("Rs ", "")
    return priceString.toDoubleOrNull() ?: 0.0
}

@Composable
fun QuantityChip(
    quantity: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    color: Color
) {
    Surface(
        modifier = Modifier.clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = quantity,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(todoItems: List<TodoItem>, totalSum: Double, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Expenses") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ExpensesList(todoItems = todoItems, totalSum = totalSum)

            Button(
                onClick = { shareExpensesList(context, todoItems, totalSum) },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("Share")
            }
        }
    }
}


fun createExpensesBitmap(context: Context, todoItems: List<TodoItem>, totalSum: Double, onBitmapReady: (Bitmap, FrameLayout) -> Unit) {
    val composeView = ComposeView(context).apply {
        setContent {
            ExpensesList(todoItems, totalSum)
        }
    }

    val parentView = FrameLayout(context).apply {
        addView(composeView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    (context as? ComponentActivity)?.addContentView(parentView, FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ))

    composeView.doOnAttach {
        Log.d("CreateExpensesBitmap", "ComposeView attached")
        generateBitmap(composeView) { bitmap ->
            onBitmapReady(bitmap, parentView)
        }
    }
}


private fun generateBitmap(composeView: ComposeView, onBitmapReady: (Bitmap) -> Unit) {
    val density = composeView.context.resources.displayMetrics.density
    val widthPx = (300 * density).toInt() // Adjust width as needed
    val maxHeightPx = (2000 * density).toInt() // Set a maximum height

    composeView.measure(
        View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(maxHeightPx, View.MeasureSpec.AT_MOST)
    )
    composeView.layout(0, 0, widthPx, composeView.measuredHeight)

    val bitmap = Bitmap.createBitmap(widthPx, composeView.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    composeView.draw(canvas)
    Log.d("GenerateBitmap", "Bitmap created: ${bitmap.width}x${bitmap.height}")
    onBitmapReady(bitmap)
}




fun shareExpensesList(context: Context, todoItems: List<TodoItem>, totalSum: Double) {
    createExpensesBitmap(context, todoItems, totalSum) { bitmap, parentView ->
        val file = saveBitmapToFile(context, bitmap)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Log.d("ShareExpensesList", "URI: $uri")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val shareIntent = Intent.createChooser(intent, "Share expenses").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(shareIntent)

        // Remove the parent view after sharing
        parentView.postDelayed({
            (parentView.parent as? ViewGroup)?.removeView(parentView)
        }, 1000)
    }
}



fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
    val file = File(context.cacheDir, "expenses.png")

    Log.d("SaveBitmapToFile", "Saving file to: ${file.absolutePath}")

    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (file.exists()) {
            Log.d("SaveBitmapToFile", "File successfully created at: ${file.absolutePath}")
        } else {
            Log.d("SaveBitmapToFile", "File creation failed")
        }
    } catch (e: Exception) {
        Log.e("SaveBitmapToFile", "Error saving file: ${e.message}")
    }

    return file
}









@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    KharchajiTheme {
        TodoApp()
    }
}