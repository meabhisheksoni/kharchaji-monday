package com.example.monday

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monday.ui.theme.KharchajiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(todoViewModel: TodoViewModel, onShareClick: () -> Unit) {
    var showListScreen by remember { mutableStateOf(false) }

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
                                    val activity = context as? MainActivity
                                    if (activity != null) {
                                        activity.shareExpensesList(context, todoItems, checkedSum)
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
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

                // Master Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Checkbox(
                    checked = masterCheckboxState,
                    onCheckedChange = { checked ->
                        masterCheckboxState = checked
                        todoViewModel.setAllItemsChecked(checked)
                    },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Select All",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
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
                                // Update master checkbox state based on all items
                                masterCheckboxState = todoItems.all { it.isDone }
                            },
                            onRemoveClick = {
                                todoViewModel.removeItem(item)
                            }
                        )
                    }
                }
            }
        }
    } else {
        EntryFormScreen(onNextClick = { showListScreen = true }, todoViewModel = todoViewModel)
    }
}

@Composable
fun TodoItemRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit
) {
    val (name, quantity, price) = parseItemText(item.text)
    
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "₹$price",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(
                        onClick = onRemoveClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete item",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(45) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryFormScreen(onNextClick: () -> Unit, todoViewModel: TodoViewModel) {
    var newItemText by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    var customQuantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("") }
    var isQuantityFieldFocused by remember { mutableStateOf(false) }
    
    val predefinedQuantities = listOf("250g", "500g", "1kg", "1.5kg", "2kg")
    val units = listOf("kg", "g", "items")
    val itemNameFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }
    val quantityFocus = remember { FocusRequester() }

    // Effect to auto-select "items" when quantity field gets focus and contains only digits
    LaunchedEffect(isQuantityFieldFocused, customQuantity) {
        if (isQuantityFieldFocused && customQuantity.isNotEmpty() && customQuantity.all { it.isDigit() }) {
            selectedUnit = "items"
            selectedQuantity = customQuantity
        }
    }

    // Function to add expense
    val addExpense = {
        if (newItemText.isNotBlank() && itemPrice.isNotBlank()) {
            val itemText = if (selectedQuantity.isNotEmpty()) {
                "$newItemText ($selectedQuantity) - ₹$itemPrice"
            } else {
                "$newItemText - ₹$itemPrice"
            }
            todoViewModel.addItem(TodoItem(text = itemText))
            newItemText = ""
            itemPrice = ""
            selectedQuantity = ""
            customQuantity = ""
            selectedUnit = ""
            itemNameFocus.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = addExpense,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = newItemText.isNotBlank() && itemPrice.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense")
                }

                OutlinedButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View All Expenses")
        Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
        Text(
                text = "Add New Expense",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Item name field
            OutlinedTextField(
                value = newItemText,
                onValueChange = { input ->
                    // Capitalize first letter of each word
                    val capitalizedText = input.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { char -> char.uppercase() }
                    }
                    newItemText = capitalizedText
                },
                label = { Text("Item name", style = MaterialTheme.typography.titleMedium) },
                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(itemNameFocus),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        priceFocus.requestFocus()
                    }
                ),
                singleLine = true
            )

            // Price field with Next button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = itemPrice,
                    onValueChange = { itemPrice = it },
                    label = { Text("Price (₹)", style = MaterialTheme.typography.titleMedium) },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(priceFocus),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { addExpense() }
                    )
                )
                
                IconButton(
                    onClick = { quantityFocus.requestFocus() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Quantity section
            Text(
                text = "Select Quantity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Predefined quantities
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(predefinedQuantities) { quantity ->
                    FilterChip(
                        selected = quantity == selectedQuantity,
                        onClick = { 
                            selectedQuantity = if (selectedQuantity == quantity) "" else quantity
                            customQuantity = ""
                            selectedUnit = ""
                        },
                        label = { Text(quantity) },
                        leadingIcon = if (quantity == selectedQuantity) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }

            // Custom quantity input and units
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customQuantity,
                    onValueChange = { 
                        customQuantity = it
                        // Auto-select "items" unit if input is numeric only and focused
                        if (isQuantityFieldFocused && it.isNotEmpty() && it.all { char -> char.isDigit() }) {
                            selectedUnit = "items"
                            selectedQuantity = it
                        } else if (it.isEmpty()) {
                            // Clear selection if empty
                            selectedUnit = ""
                            selectedQuantity = ""
                        } else if (selectedUnit.isNotEmpty()) {
                            // Update with current unit if already selected
                            selectedQuantity = if (selectedUnit == "items") {
                                it // Just the number for items
                            } else {
                                "$it${selectedUnit.lowercase()}" // Unit suffix for kg/g
                            }
                        }
                    },
                    label = { Text("Quantity", style = MaterialTheme.typography.titleMedium) },
                    modifier = Modifier
                        .width(120.dp)
                        .focusRequester(quantityFocus)
                        .onFocusChanged { focusState ->
                            isQuantityFieldFocused = focusState.isFocused
                        },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            if (newItemText.isNotBlank() && itemPrice.isNotBlank()) {
                                addExpense()
                            }
                        }
                    ),
                    singleLine = true
                )

                // Units as chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(units) { unit ->
                        FilterChip(
                            selected = unit == selectedUnit,
                            onClick = { 
                                selectedUnit = if (selectedUnit == unit) "" else unit
                                if (customQuantity.isNotEmpty()) {
                                    selectedQuantity = if (unit == "items") {
                                        customQuantity // Just the number for items
                                    } else {
                                        "$customQuantity${unit.lowercase()}" // Unit suffix for kg/g
                                    }
                                }
                            },
                            label = { Text(unit) },
                            leadingIcon = if (unit == selectedUnit) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        itemNameFocus.requestFocus()
    }
}