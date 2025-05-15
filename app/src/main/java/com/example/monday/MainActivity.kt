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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
            
            // Show progress dialog
            val progressDialog = android.app.ProgressDialog(context).apply {
                setMessage("Preparing expenses list...")
                setCancelable(false)
                show()
            }
            
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
                            progressDialog.dismiss()
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
                            progressDialog.dismiss()
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
            val layout = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundColor(android.graphics.Color.WHITE)
                setPadding(32, 24, 32, 24)
            }

            // Add date in top left with light gray color
            android.widget.TextView(context).apply {
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                setTextColor(android.graphics.Color.LTGRAY)
                textSize = 14f
                gravity = android.view.Gravity.START
                this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
            }.also { layout.addView(it) }

            // Add title dots
            android.widget.TextView(context).apply {
                text = "...."
                setTextColor(android.graphics.Color.BLACK)
                textSize = 20f
                gravity = android.view.Gravity.CENTER
                this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
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
                android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    // Item details row
                    android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 8, 0, 8)
                        }
                        
                        // Number and name
                        android.widget.TextView(context).apply {
                            text = "${index + 1}. $name"
                            setTextColor(android.graphics.Color.BLACK)
                            textSize = 16f
                            this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                1.0f
                            )
                        }.also { addView(it) }

                        // Quantity
                        if (quantity != null) {
                            android.widget.TextView(context).apply {
                                text = quantity
                                setTextColor(android.graphics.Color.RED)
                                textSize = 16f
                                gravity = android.view.Gravity.CENTER
                                this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    marginStart = 16
                                    marginEnd = 16
                                    width = (80 * context.resources.displayMetrics.density).toInt()
                                }
                            }.also { addView(it) }
                        } else {
                            android.view.View(context).apply {
                                this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
                                    (80 * context.resources.displayMetrics.density).toInt(),
                                    1
                                )
                            }.also { addView(it) }
                        }

                        // Price
                        android.widget.TextView(context).apply {
                            text = "Rs $price"
                            setTextColor(android.graphics.Color.BLACK)
                            textSize = 16f
                            gravity = android.view.Gravity.END
                            this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                width = (80 * context.resources.displayMetrics.density).toInt()
                            }
                        }.also { addView(it) }
                    }.also { addView(it) }

                    // Add dashed separator line
                    if (index < itemsForBitmap.size - 1) {
                        android.widget.LinearLayout(context).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 8, 0, 8)
                            }
                            
                            // Create dotted line effect
                            val dotsCount = 45
                            for (i in 0 until dotsCount) {
                                android.view.View(context).apply {
                                    setBackgroundColor(android.graphics.Color.GRAY)
                                    this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
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
            android.widget.TextView(context).apply {
                text = "Total: Rs ${"%.1f".format(totalSumForBitmap)}"
                setTextColor(android.graphics.Color.BLACK)
                textSize = 28f
                gravity = android.view.Gravity.END
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                this@apply.layoutParams = android.widget.LinearLayout.LayoutParams(
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

    if (showShareScreen) {
        ShareScreen(
            todoItems = todoViewModel.todoItems.collectAsState(initial = emptyList()).value,
            totalSum = todoViewModel.todoItems.collectAsState(initial = emptyList()).value.sumOf { parsePrice(it.text) },
            onDismiss = { showShareScreen = false }
        )
    } else {
        ExpenseListScreen(
            todoViewModel = todoViewModel,
            onShareClick = { showShareScreen = true }
        )
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

@Composable
fun TodoItemRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit,
    onEditClick: (TodoItem) -> Unit
) {
    val (name, quantity, price) = parseItemText(item.text)
    
    // Swipe state
    var offsetX by remember { mutableStateOf(0f) }
    val offsetDp = with(LocalDensity.current) { offsetX.toDp() }
    
    // Action threshold - when to trigger the action
    val actionThreshold = 200.dp // Increased from 100.dp to require more swiping
    val actionThresholdPx = with(LocalDensity.current) { actionThreshold.toPx() }
    
    // Track if an action was performed to prevent multiple triggers
    var actionPerformed by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Background (revealed when swiping)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp) // Fixed height to match the card
                .clip(RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Delete action background (right swipe reveals)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFFFDDDD)), // Light red for delete
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White // White icon as requested
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Edit action background (left swipe reveals)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFD0E4FF)), // Light blue for edit
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White // White icon for consistency
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            color = Color.Blue,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        
        // Card (the swipeable element)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetDp)
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // Reset action state at drag start
                            actionPerformed = false
                        },
                        onDragEnd = {
                            // Only trigger action at the end of the drag if threshold is crossed
                            if (!actionPerformed) {
                                if (offsetX < -actionThresholdPx) {
                                    onEditClick(item)
                                    actionPerformed = true
                                } else if (offsetX > actionThresholdPx) {
                                    onRemoveClick()
                                    actionPerformed = true
                                }
                            }
                            
                            // Animate back to center
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            
                            // Only update offset if we haven't performed an action yet
                            if (!actionPerformed) {
                                offsetX += dragAmount
                                
                                // Allow dragging until the action threshold with some extra room
                                offsetX = offsetX.coerceIn(-actionThresholdPx * 1.5f, actionThresholdPx * 1.5f)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
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
                    
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (quantity != null) {
                            Text(
                                text = quantity,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "₹$price",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    item: TodoItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    
    val predefinedQuantities = listOf("250g", "500g", "1kg", "1.5kg", "2kg")
    var selectedPredefinedQuantity by remember { mutableStateOf("") }

    var customQuantityValue by remember { mutableStateOf("") }
    val customUnits = listOf("kg", "g", "items")
    var selectedCustomUnit by remember { mutableStateOf("") }

    LaunchedEffect(item) {
        val (parsedName, initialQuantityStringNullable, parsedPrice) = parseItemText(item.text)
        itemName = parsedName
        itemPrice = parsedPrice

        selectedPredefinedQuantity = ""
        customQuantityValue = ""
        selectedCustomUnit = ""

        initialQuantityStringNullable?.let { initialNonNullQuantityString ->
            if (predefinedQuantities.contains(initialNonNullQuantityString)) {
                selectedPredefinedQuantity = initialNonNullQuantityString
            } else {
                var numPart = initialNonNullQuantityString 
                var unitPart = ""
                for (unit in customUnits.sortedByDescending { it.length }) {
                    if (initialNonNullQuantityString.endsWith(unit, ignoreCase = true)) {
                        val potentialNumPart = initialNonNullQuantityString.substring(0, initialNonNullQuantityString.length - unit.length).trim()
                        if (potentialNumPart.all { it.isDigit() || it == '.' } && potentialNumPart.isNotEmpty()) {
                            numPart = potentialNumPart
                            unitPart = unit.lowercase()
                            break
                        }
                    }
                }

                if (numPart.all { it.isDigit() || it == '.' } && numPart.isNotEmpty()) {
                    customQuantityValue = numPart
                    selectedCustomUnit = unitPart.ifEmpty { "items" }
                } else {
                    customQuantityValue = initialNonNullQuantityString 
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Adjusted spacing
            ) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it },
                    label = { Text("Price (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("Quantity (Predefined)", style = MaterialTheme.typography.bodyMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(predefinedQuantities) { quantity ->
                        FilterChip(
                            selected = quantity == selectedPredefinedQuantity,
                            onClick = { 
                                selectedPredefinedQuantity = if (selectedPredefinedQuantity == quantity) "" else quantity
                                customQuantityValue = ""
                                selectedCustomUnit = ""
                            },
                            label = { Text(quantity) },
                            leadingIcon = if (quantity == selectedPredefinedQuantity) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                Text("Or Custom Quantity", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customQuantityValue,
                        onValueChange = { 
                            customQuantityValue = it 
                            selectedPredefinedQuantity = "" // Clear predefined if custom is typed
                        },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Box(modifier = Modifier.weight(1f)) { // Box to allow LazyRow to take space
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(customUnits) { unit ->
                                FilterChip(
                                    selected = unit == selectedCustomUnit,
                                    onClick = { 
                                        selectedCustomUnit = if (selectedCustomUnit == unit) "" else unit
                                        selectedPredefinedQuantity = "" // Clear predefined if custom unit selected
                                     },
                                    label = { Text(unit) },
                                    leadingIcon = if (unit == selectedCustomUnit) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalQuantityString: String? = if (selectedPredefinedQuantity.isNotBlank()) {
                        selectedPredefinedQuantity
                    } else if (customQuantityValue.isNotBlank()) {
                        when {
                            selectedCustomUnit.isNotBlank() && selectedCustomUnit.lowercase() != "items" -> customQuantityValue + selectedCustomUnit.lowercase() // e.g., "1.5kg"
                            selectedCustomUnit.lowercase() == "items" -> customQuantityValue + "items" // e.g., "2items"
                            else -> customQuantityValue // Just the number e.g. "2", implies items for parsing
                        }
                    } else {
                        null
                    }
                    onConfirm(itemName, itemPrice, finalQuantityString)
                    onDismiss()
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(todoViewModel: TodoViewModel, onShareClick: () -> Unit) {
    var showListScreen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val todoItems by todoViewModel.todoItems.collectAsState(initial = emptyList())
    val totalItems = todoItems.size
    val checkedItems = todoItems.count { it.isDone }
    val totalSum = todoItems.sumOf { parsePrice(it.text) }
    val checkedSum = todoItems.filter { it.isDone }.sumOf { parsePrice(it.text) }
    var masterCheckboxState by remember(todoItems) { 
        mutableStateOf(todoItems.isNotEmpty() && todoItems.all { it.isDone })
    }
    val context = LocalContext.current

    if (showListScreen) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Expenses List") },
                    navigationIcon = {
                        IconButton(onClick = { showListScreen = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { 
                                try {
                                    val currentCheckedItems = todoItems.filter { it.isDone }
                                    val currentCheckedSum = currentCheckedItems.sumOf { parsePrice(it.text) }

                                    if (currentCheckedItems.isEmpty()) {
                                        Toast.makeText(context, "Please select items to share", Toast.LENGTH_SHORT).show()
                                    }

                                    val activity = context as? MainActivity
                                    if (activity != null) {
                                        activity.shareExpensesList(context, currentCheckedItems, currentCheckedSum)
                                    } else {
                                        Log.e("ShareExpensesList", "Context is not MainActivity")
                                        Toast.makeText(
                                            context,
                                            "Unable to share: Context is not MainActivity",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("ShareExpensesList", "Error in share button click", e)
                                    Toast.makeText(
                                        context,
                                        "Error sharing: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = todoItems.any { it.isDone }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share expenses")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                                .format(Date()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$totalItems",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Amount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹$totalSum",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        if (checkedItems > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Checked Items: $checkedItems",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹$checkedSum",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Master Checkbox and Delete All Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            text = "Select All",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (todoItems.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete All Expenses")
                        }
                    }
                }

                // Confirmation Dialog for Delete All
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

                // Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(todoItems) { item ->
                        TodoItemRow(
                            item = item,
                            onCheckedChange = { checked ->
                                todoViewModel.updateItem(item.copy(isDone = checked))
                                masterCheckboxState = todoItems.all { it.isDone }
                            },
                            onRemoveClick = {
                                todoViewModel.removeItem(item)
                            },
                            onEditClick = {
                                editingItem = it
                            }
                        )
                    }
                }
            }

            // Edit Dialog
            editingItem?.let { item ->
                EditExpenseDialog(
                    item = item,
                    onDismiss = { editingItem = null },
                    onConfirm = { name, price, quantity ->
                        val newText = if (quantity != null) {
                            "$name ($quantity) - ₹$price"
                        } else {
                            "$name - ₹$price"
                        }
                        todoViewModel.updateItem(item.copy(text = newText))
                    }
                )
            }
        }
        } else {
        EntryFormScreen(onNextClick = { showListScreen = true }, todoViewModel = todoViewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseListScreenPreview() {
    KharchajiTheme {
        // Remove preview since TodoViewModel requires Application context
    }
}