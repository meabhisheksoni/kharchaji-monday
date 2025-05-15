package com.example.monday

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.doOnAttach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monday.ui.theme.KharchajiTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import com.example.monday.parseItemText
import com.example.monday.parsePrice
import com.example.monday.EntryFormScreen
import com.example.monday.CalculationRecord
import com.example.monday.RecordItem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KharchajiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                TodoApp()
            }
        }
    }
}

    fun shareExpensesList(context: Context, itemsToShare: List<TodoItem>, sumOfItemsToShare: Double) {
        try {
            Log.d("ShareExpensesList", "Starting share process for ${itemsToShare.size} items with sum $sumOfItemsToShare")
            
            if (itemsToShare.isEmpty()) {
                Log.e("ShareExpensesList", "No items selected/passed to share")
                android.widget.Toast.makeText(
                    context,
                    "Please select items to share",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            Log.d("ShareExpensesList", "Sum to use for bitmap: $sumOfItemsToShare")
            
            // Log the items being passed to createExpensesBitmap
            Log.d("ShareExpensesList", "Items being sent to createExpensesBitmap (${itemsToShare.size} items):")
            itemsToShare.forEachIndexed { index, item ->
                Log.d("ShareExpensesList", "  Item $index: ${item.text}, isDone: ${item.isDone}")
            }
            
            // Create a handler to post to main thread
            val mainHandler = Handler(android.os.Looper.getMainLooper())
            
            // Run the bitmap creation in a background thread
            Thread {
                try {
                    val bitmap = createExpensesBitmap(context, itemsToShare, sumOfItemsToShare)
                    Log.d("ShareExpensesList", "Created bitmap: ${bitmap.width}x${bitmap.height}")
                    
                    val file = saveBitmapToFile(context, bitmap)
                    Log.d("ShareExpensesList", "Saved bitmap to file: ${file.absolutePath}")
                    Log.d("ShareExpensesList", "File exists: ${file.exists()}")
                    Log.d("ShareExpensesList", "File size: ${file.length()}")

                    val authority = "${context.packageName}.fileprovider"
                    Log.d("ShareExpensesList", "FileProvider authority: $authority")
                    
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    Log.d("ShareExpensesList", "Created URI: $uri")

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    Log.d("ShareExpensesList", "Created share intent")
                    val chooser = Intent.createChooser(shareIntent, "Share Expenses")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    // Dismiss progress dialog and start share activity on UI thread
                    mainHandler.post {
                        try {
                            Log.d("ShareExpensesList", "Starting chooser activity")
                            context.startActivity(chooser)
                            Log.d("ShareExpensesList", "Started share activity successfully")
                        } catch (e: Exception) {
                            Log.e("ShareExpensesList", "Error starting share activity", e)
                            android.widget.Toast.makeText(
                                context,
                                "Error sharing: ${e.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ShareExpensesList", "Error during bitmap/sharing process", e)
                    e.printStackTrace()
                    // Show error on UI thread
                    mainHandler.post {
                        try {
                            // progressDialog.dismiss()
                        } catch (e2: Exception) {
                            Log.e("ShareExpensesList", "Error dismissing dialog", e2)
                        }
                        android.widget.Toast.makeText(
                            context,
                            "Error sharing expenses: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.start()
        } catch (e: Exception) {
            Log.e("ShareExpensesList", "Error in shareExpensesList: ${e.message}")
            e.printStackTrace()
            // Show a toast to inform the user
            android.widget.Toast.makeText(
                context,
                "Error sharing expenses: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createExpensesBitmap(context: Context, itemsForBitmap: List<TodoItem>, totalSumForBitmap: Double): Bitmap {
        try {
            Log.d("ShareExpensesList", "Creating bitmap for ${itemsForBitmap.size} items, sum: $totalSumForBitmap")
            
            // Create a LinearLayout to hold our content
            val layout = android.widget.LinearLayout(context).apply layoutScope@{
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundColor(android.graphics.Color.WHITE)
                setPadding(32, 24, 32, 24)
            }

            // Add date in top left with light gray color
            android.widget.TextView(context).apply dateTextViewScope@{
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                setTextColor(android.graphics.Color.LTGRAY)
                textSize = 14f
                gravity = android.view.Gravity.START
                this@dateTextViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
            }.also { layout.addView(it) }

            // Add title dots
            android.widget.TextView(context).apply titleDotsViewScope@{
                text = "...."
                setTextColor(android.graphics.Color.BLACK)
                textSize = 20f
                gravity = android.view.Gravity.CENTER
                this@titleDotsViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 24)
                }
            }.also { layout.addView(it) }

            // Add items
            itemsForBitmap.forEachIndexed { index, item ->
                val (name, quantity, price) = parseItemText(item.text)
                
                // Item row container
                android.widget.LinearLayout(context).apply itemRowContainerScope@{
                    orientation = android.widget.LinearLayout.VERTICAL
                    this@itemRowContainerScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    // Item details row
                    android.widget.LinearLayout(context).apply itemDetailsRowScope@{
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        this@itemDetailsRowScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 8, 0, 8)
                        }
                        
                        // Number and name
                        android.widget.TextView(context).apply nameTextViewScope@{
                            text = "${index + 1}. $name"
                            setTextColor(android.graphics.Color.BLACK)
                            textSize = 16f
                            this@nameTextViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                1.0f
                            )
                        }.also { addView(it) }

                        // Quantity
                        if (quantity != null) {
                            android.widget.TextView(context).apply quantityTextViewScope@{
                                text = quantity
                                setTextColor(android.graphics.Color.RED)
                                textSize = 16f
                                gravity = android.view.Gravity.CENTER
                                this@quantityTextViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    marginStart = 16
                                    marginEnd = 16
                                    width = (80 * context.resources.displayMetrics.density).toInt()
                                }
                            }.also { addView(it) }
                        } else {
                            android.view.View(context).apply emptyViewScope@{
                                this@emptyViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                                    (80 * context.resources.displayMetrics.density).toInt(),
                                    1
                                )
                            }.also { addView(it) }
                        }

                        // Price
                        android.widget.TextView(context).apply priceTextViewScope@{
                            text = "Rs $price"
                            setTextColor(android.graphics.Color.BLACK)
                            textSize = 16f
                            gravity = android.view.Gravity.END
                            this@priceTextViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                width = (80 * context.resources.displayMetrics.density).toInt()
                            }
                        }.also { addView(it) }
                    }.also { addView(it) }

                    // Add dashed separator line
                    if (index < itemsForBitmap.size - 1) {
                        android.widget.LinearLayout(context).apply separatorLayoutScope@{
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            this@separatorLayoutScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 8, 0, 8)
                            }
                            
                            // Create dotted line effect
                            val dotsCount = 45
                            for (i in 0 until dotsCount) {
                                android.view.View(context).apply dotViewScope@{
                                    setBackgroundColor(android.graphics.Color.GRAY)
                                    this@dotViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                                        2,
                                        1
                                    ).apply {
                                        marginStart = 2
                                        marginEnd = 2
                                        weight = 1f
                                    }
                                }.also { addView(it) }
                            }
                        }.also { addView(it) }
                    }
                }.also { layout.addView(it) }
            }

            // Add total
            android.widget.TextView(context).apply totalTextViewScope@{
                text = "Total: Rs ${"%.1f".format(totalSumForBitmap)}"
                setTextColor(android.graphics.Color.BLACK)
                textSize = 28f
                gravity = android.view.Gravity.END
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                this@totalTextViewScope.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 32, 0, 0)
                }
            }.also { layout.addView(it) }

            // Measure and layout
            val width = (360 * context.resources.displayMetrics.density).toInt()
            val spec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            layout.measure(spec, View.MeasureSpec.UNSPECIFIED)
            
            val height = layout.measuredHeight
            layout.layout(0, 0, width, height)

            // Create bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            layout.draw(canvas)

            return bitmap
        } catch (e: Exception) {
            Log.e("ShareExpensesList", "Error creating bitmap", e)
            throw e
        }
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
        try {
            // Ensure cache directory exists
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, "expenses.png")
            Log.d("ShareExpensesList", "Saving bitmap to: ${file.absolutePath}")
            
            // Delete existing file if it exists
            if (file.exists()) {
                file.delete()
            }
            
            FileOutputStream(file).use { out ->
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                if (!success) {
                    throw Exception("Failed to compress bitmap")
                }
                out.flush()
            }
            
            if (!file.exists()) {
                throw Exception("File was not created")
            }
            
            Log.d("ShareExpensesList", "File saved successfully. Size: ${file.length()} bytes")
            return file
        } catch (e: Exception) {
            Log.e("ShareExpensesList", "Error saving bitmap to file", e)
            throw e
        }
    }
}

@Composable
fun TodoApp(todoViewModel: TodoViewModel = viewModel()) {
    var showShareScreen by remember { mutableStateOf(false) }
    var showCalculationRecordsScreen by remember { mutableStateOf(false) }
    var currentCalculationRecordId by remember { mutableStateOf<Int?>(null) }
    var showEntryForm by remember { mutableStateOf(false) }

    when {
        showEntryForm -> {
            EntryFormScreen(
                onNextClick = { showEntryForm = false },
                todoViewModel = todoViewModel
            ) 
        }
        showShareScreen -> {
            ShareScreen(
                todoItems = todoViewModel.todoItems.collectAsState(initial = emptyList()).value.filter { it.isDone },
                totalSum = todoViewModel.todoItems.collectAsState(initial = emptyList()).value.filter { it.isDone }.sumOf { parsePrice(it.text) },
                onDismiss = { showShareScreen = false }
            )
        }
        showCalculationRecordsScreen -> {
            if (currentCalculationRecordId != null) {
                val recordIdKey = currentCalculationRecordId!!
                key(recordIdKey) {
                    CalculationRecordDetailScreen(
                        recordId = recordIdKey,
                        todoViewModel = todoViewModel,
                        onNavigateBack = { currentCalculationRecordId = null },
                        onSetMemoAndReturnToExpenses = {
                            currentCalculationRecordId = null
                            showCalculationRecordsScreen = false
                        }
                    )
                }
            } else {
                CalculationRecordsScreen(
                    todoViewModel = todoViewModel,
                    onNavigateBack = { showCalculationRecordsScreen = false },
                    onRecordClick = { recordId -> currentCalculationRecordId = recordId }
                )
            }
        }
        else -> {
            ExpenseListScreen(
                todoViewModel = todoViewModel,
                onShareClick = { showShareScreen = true },
                onViewRecordsClick = { 
                    currentCalculationRecordId = null 
                    showCalculationRecordsScreen = true 
                },
                onAddItemClick = { showEntryForm = true }
            )
        }
    }
}

@Composable
fun QuantityChip(
    quantity: String,
    isSelected: Boolean,
    onSelect: () -> Unit
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
    var isSharing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Expenses") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
        ) {
            ExpensesList(todoItems = todoItems, totalSum = totalSum)

            Button(
                    onClick = {
                        isSharing = true
                        try {
                            val activity = context as? MainActivity
                            activity?.shareExpensesList(context, todoItems, totalSum)
                        } finally {
                            isSharing = false
                        }
                    },
                modifier = Modifier
                    .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    enabled = !isSharing && todoItems.any { it.isDone }
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text("Share Selected Items")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    KharchajiTheme {
        TodoApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    todoViewModel: TodoViewModel,
    onShareClick: () -> Unit,
    onViewRecordsClick: () -> Unit,
    onAddItemClick: () -> Unit
) {
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val todoItems by todoViewModel.todoItems.collectAsState(initial = emptyList())
    val totalItemsCount = todoItems.size
    val checkedItemsCount = todoItems.count { it.isDone }
    val totalSum = todoItems.sumOf { parsePrice(it.text) }
    val checkedSum = todoItems.filter { it.isDone }.sumOf { parsePrice(it.text) }
    var masterCheckboxState by remember(todoItems) { 
        mutableStateOf(todoItems.isNotEmpty() && todoItems.all { it.isDone })
    }
    val context = LocalContext.current
    val undoableItemsStack by todoViewModel.undoableDeletedItems.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Expenses") },
                actions = {
                    IconButton(onClick = onViewRecordsClick) {
                        Icon(Icons.Filled.Analytics, contentDescription = "View Calculation Records")
                    }
                    IconButton(onClick = {
                        val recordItems = todoItems.map { todoItemToRecordItem(it) }
                        if (recordItems.isNotEmpty()){
                            val newRecord = CalculationRecord(
                                items = recordItems,
                                totalSum = totalSum,
                                checkedItemsCount = checkedItemsCount,
                                checkedItemsSum = checkedSum
                            )
                            todoViewModel.insertCalculationRecord(newRecord)
                            Toast.makeText(context, "Current list saved as a record", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No items to save as record", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Current List as Record")
                    }
                    if (todoItems.any { it.isDone }) {
                        IconButton(onClick = onShareClick) {
                            Icon(Icons.Default.Share, contentDescription = "Share selected expenses")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (checkedItemsCount > 0) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Selected ($checkedItemsCount items)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${String.format("%.2f", checkedSum)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total ($totalItemsCount items)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalSum)}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = masterCheckboxState,
                        onCheckedChange = { checked ->
                            masterCheckboxState = checked
                            todoViewModel.setAllItemsChecked(checked)
                        }
                    )
                    Text(
                        text = if (masterCheckboxState && todoItems.isNotEmpty()) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { 
                            val newMasterState = !(todoItems.isNotEmpty() && todoItems.all { it.isDone })
                            masterCheckboxState = newMasterState
                            todoViewModel.setAllItemsChecked(newMasterState) 
                        }.padding(start = 4.dp, end = 8.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (undoableItemsStack.isNotEmpty()) {
                        IconButton(onClick = { todoViewModel.undoLastDelete() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Delete")
                        }
                    }
                    if (todoItems.any { it.isDone }) {
                        if (undoableItemsStack.isNotEmpty()) {
                             Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(onClick = { 
                            val itemsToActuallyDelete = todoItems.filter { it.isDone }
                            todoViewModel.deleteSelectedItemsAndEnableUndo(itemsToActuallyDelete)
                        }) {
                            Icon(
                                Icons.Filled.Delete, 
                                contentDescription = "Delete Selected Items",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (todoItems.isNotEmpty()) {
                        if (undoableItemsStack.isNotEmpty() || todoItems.any { it.isDone }) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete All Expenses")
                        }
                    }
                }
            }

            if (todoItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expenses. Tap the + button to add an item.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 56.dp), // Ensure FAB doesn't overlap last item
                    verticalArrangement = Arrangement.spacedBy(0.dp) // Remove extra spacing if Card has padding
                ) {
                    items(todoItems, key = { it.id }) { item ->
                        LocalTodoItemRow( // Changed parameters
                            item = item,
                            onCheckedChange = { isChecked ->
                                todoViewModel.updateItem(item.copy(isDone = isChecked))
                            },
                            onRemoveClick = { // For swipe-right to delete
                                todoViewModel.removeItem(item)
                            },
                            onEditClick = { // For swipe-left to edit
                                editingItem = item 
                            }
                        )
                        // Divider() // Optional: Add divider if desired, swipe backgrounds might make it redundant
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        EditItemDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onConfirm = { updatedText ->
                todoViewModel.updateItem(editingItem!!.copy(text = updatedText))
                editingItem = null
            },
            predefinedQuantities = listOf("250g", "500g", "1kg", "1.5kg", "2kg"),
            customUnits = listOf("kg", "g", "pcs", "ltr", "mtr", "dozen", "items")
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Expenses?") },
            text = { Text("Are you sure you want to delete all expenses? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        todoViewModel.deleteAllItems()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseListScreenPreview() {
    KharchajiTheme {
        // Remove preview since TodoViewModel requires Application context
    }
}

// Helper function to convert TodoItem to RecordItem
fun todoItemToRecordItem(todoItem: TodoItem): RecordItem {
    val (name, quantity, price) = parseItemText(todoItem.text)
    return RecordItem(
        description = name,
        quantity = quantity,
        price = price, // price from parseItemText is already a String
        isChecked = todoItem.isDone
    )
}

// Corrected helper function to convert RecordItem to TodoItem text representation
fun recordItemToTodoItemText(recordItem: RecordItem): String {
    val name = recordItem.description
    val quantity = recordItem.quantity
    val priceString = recordItem.price // price from RecordItem is a String

    // Attempt to parse the price string to a Double.
    // If parsing fails (e.g., malformed string), default to 0.0 or handle error appropriately.
    val priceAsDouble: Double = try {
        priceString.toDouble()
    } catch (e: NumberFormatException) {
        Log.e("RecordItemConversion", "Failed to parse price string: '$priceString' to Double. Defaulting to 0.0", e)
        0.0 // Default value in case of error
    }

    return if (quantity != null && quantity.isNotBlank()) {
        // Format the Double price value, using Locale.US for consistency
        "$name ($quantity) - ₹${String.format(Locale.US, "%.1f", priceAsDouble)}"
    } else {
        // Format the Double price value, using Locale.US for consistency
        "$name - ₹${String.format(Locale.US, "%.1f", priceAsDouble)}"
    }
}

// Renamed TodoItemRow to LocalTodoItemRow and incorporated swipe logic
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalTodoItemRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit, // Triggered by swipe right
    onEditClick: (TodoItem) -> Unit // Triggered by swipe left
) {
    val (name, quantity, price) = parseItemText(item.text)
    
    var offsetX by remember { mutableStateOf(0f) }
    // val offsetDp = with(LocalDensity.current) { offsetX.toDp() } // Not strictly needed if Card is offset
    
    val actionThreshold = 200.dp // Increased from 150.dp
    val actionThresholdPx = with(LocalDensity.current) { actionThreshold.toPx() }
    
    var actionPerformed by remember { mutableStateOf(false) }
    
    // val density = LocalDensity.current // Removed unused variable

    Box( // Outer Box to hold swipe backgrounds and the Card
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Keep some vertical spacing between items
    ) {
        // Backgrounds for swipe actions
        Row(
            modifier = Modifier
                .fillMaxSize() // Fill the space of the Box
                .clip(RoundedCornerShape(12.dp)), // Clip background to match Card shape
            horizontalArrangement = Arrangement.SpaceBetween // Important for placing backgrounds
        ) {
            // Edit action background (revealed on left swipe, so it's on the right)
            Box(
                modifier = Modifier
                    .weight(1f) // Takes available space
                    .fillMaxHeight()
                    .background(Color(0xFFD0E4FF)) // Light blue for edit
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart // Align content to the start (left)
            ) {
                 if (offsetX < -actionThresholdPx / 2) { // Show icon when swiping left
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.Blue 
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit",
                            color = Color.Blue,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            // Delete action background (revealed on right swipe, so it's on the left)
            Box(
                modifier = Modifier
                    .weight(1f) // Takes available space
                    .fillMaxHeight()
                    .background(Color(0xFFFFDDDD)) // Light red for delete
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd // Align content to the end (right)
            ) {
                if (offsetX > actionThresholdPx / 2) { // Show icon when swiping right
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Delete",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }
            }
        }

        // Card (the swipeable element)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) } // Apply horizontal offset
                .pointerInput(item.id) { // Use item.id to reset gesture state if item changes
                    detectHorizontalDragGestures(
                        onDragStart = {
                            actionPerformed = false // Reset action performed flag
                        },
                        onDragEnd = {
                            // Animate back to center after drag ends
                            // LaunchedEffect(offsetX) { // Consider if smooth animation back is needed
                            //    animate(offsetX, 0f) { value, _ -> offsetX = value }
                            // }
                            // For now, direct reset:
                            if (!actionPerformed) { // If no action was taken, snap back
                                offsetX = 0f
                            }
                            // else action was performed, it will be removed/edited, no need to snap back visually
                            // or, if we want a consistent snap-back even after action:
                            // offsetX = 0f 
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffsetX = offsetX + dragAmount
                            
                            if (abs(newOffsetX) > actionThresholdPx && !actionPerformed) {
                                if (newOffsetX < -actionThresholdPx) { // Swiping left (for edit)
                                    onEditClick(item)
                                    actionPerformed = true
                                    offsetX = 0f // Reset position after action
                                } else if (newOffsetX > actionThresholdPx) { // Swiping right (for delete)
                                    onRemoveClick()
                                    actionPerformed = true
                                    // No offsetX = 0f here, item will be removed
                                }
                            } else if (!actionPerformed) {
                                offsetX = newOffsetX.coerceIn(-actionThresholdPx * 1.5f, actionThresholdPx * 1.5f)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // Or surfaceVariant if preferred
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp), // Inner padding for content
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // Allow name/quantity to take space
                ) {
                    Checkbox(
                        checked = item.isDone,
                        onCheckedChange = onCheckedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) { // Prevent text from pushing price too far
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (quantity != null) {
                            Text(
                                text = quantity,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Price is now at the end, no explicit delete button here
                Text(
                    text = "₹$price",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp) // Add some padding before price
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    item: TodoItem,
    onDismiss: () -> Unit,
    onConfirm: (updatedText: String) -> Unit,
    predefinedQuantities: List<String>,
    customUnits: List<String>
) {
    var itemName by remember(item) { mutableStateOf(parseItemText(item.text).first) }
    var itemPrice by remember(item) { mutableStateOf(parseItemText(item.text).third) }
    
    var selectedPredefinedQuantity by remember(item) { 
        val initialQuantity = parseItemText(item.text).second
        mutableStateOf(if (initialQuantity != null && predefinedQuantities.contains(initialQuantity)) initialQuantity else "")
    }
    
    var customQuantityValue by remember(item) {
        val initialQuantity = parseItemText(item.text).second
        mutableStateOf(if (initialQuantity != null && !predefinedQuantities.contains(initialQuantity)) initialQuantity.filter { it.isDigit() || it == '.' } else "")
    }
    
    var selectedCustomUnit by remember(item) {
        val initialQuantity = parseItemText(item.text).second
        var unit = ""
        if (initialQuantity != null && !predefinedQuantities.contains(initialQuantity)) {
            customUnits.sortedByDescending { it.length }.forEach { u ->
                if (initialQuantity.endsWith(u, ignoreCase = true)) {
                    unit = u
                    return@forEach
                }
            }
        }
        mutableStateOf(if (unit.isNotEmpty()) unit else (if (customQuantityValue.isNotEmpty() && initialQuantity?.filterNot { it.isDigit() || it == '.' }?.isEmpty() == true) "items" else ""))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it },
                    label = { Text("Price (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )

                Text("Quantity (Predefined)", style = MaterialTheme.typography.bodyMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(predefinedQuantities) { quantity ->
                        FilterChip(
                            selected = quantity == selectedPredefinedQuantity,
                            onClick = { 
                                selectedPredefinedQuantity = if (selectedPredefinedQuantity == quantity) "" else quantity
                                customQuantityValue = ""
                                selectedCustomUnit = ""
                            },
                            label = { Text(quantity) }
                        )
                    }
                }

                Text("Or Custom Quantity", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customQuantityValue,
                        onValueChange = { 
                            customQuantityValue = it 
                            selectedPredefinedQuantity = "" 
                        },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true
                    )
                    LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customUnits) { unit ->
                            FilterChip(
                                selected = unit == selectedCustomUnit,
                                onClick = { 
                                    selectedCustomUnit = if (selectedCustomUnit == unit) "" else unit
                                    selectedPredefinedQuantity = "" 
                                 },
                                label = { Text(unit) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalQuantityString = when {
                        selectedPredefinedQuantity.isNotBlank() -> selectedPredefinedQuantity
                        customQuantityValue.isNotBlank() -> {
                            when {
                                selectedCustomUnit.isNotBlank() && selectedCustomUnit.lowercase() != "items" -> customQuantityValue + selectedCustomUnit.lowercase()
                                else -> customQuantityValue // If "items" or no unit, just the value
                            }
                        }
                        else -> null // No quantity specified
                    }
                    val newText = if (finalQuantityString != null) {
                        "${itemName.trim()} ($finalQuantityString) - ₹${itemPrice.trim()}"
                    } else {
                        "${itemName.trim()} - ₹${itemPrice.trim()}"
                    }
                    onConfirm(newText)
                    onDismiss()
                },
                enabled = itemName.isNotBlank() && itemPrice.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}