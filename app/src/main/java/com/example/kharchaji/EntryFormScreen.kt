package com.example.kharchaji

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryFormScreen(onNextClick: () -> Unit, todoViewModel: TodoViewModel) {
    var newItemText by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("") }
    var customQuantity by remember { mutableStateOf("") }
    var showUnitSelector by remember { mutableStateOf(false) }
    var selectedUnit by remember { mutableStateOf("") }
    
    val predefinedQuantities = listOf("250g", "500g", "1kg", "1.5kg", "2kg")
    val itemNameFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }
    val quantityFocus = remember { FocusRequester() }

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

    // Unit selector dialog
    if (showUnitSelector) {
        AlertDialog(
            onDismissRequest = { showUnitSelector = false },
            title = { Text("Select Unit") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("gm", "kg", "items").forEach { unit ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUnit = unit
                                    showUnitSelector = false
                                    selectedQuantity = "$customQuantity$unit"
                                },
                            color = if (unit == selectedUnit) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surface
                        ) {
        Text(
                                text = unit,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = { }
        )
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
                onValueChange = { newItemText = it },
                label = { Text("Item name") },
                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(itemNameFocus),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { priceFocus.requestFocus() })
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
                    label = { Text("Price (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(priceFocus),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
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

            // Custom quantity input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customQuantity,
                    onValueChange = { 
                        customQuantity = it
                        if (it.isNotEmpty() && selectedUnit.isNotEmpty()) {
                            selectedQuantity = "$it$selectedUnit"
                        }
                    },
                    label = { Text("Custom Quantity") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(quantityFocus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (newItemText.isNotBlank() && itemPrice.isNotBlank()) 
                            ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            if (newItemText.isNotBlank() && itemPrice.isNotBlank()) {
                                addExpense()
                            } else if (customQuantity.isNotEmpty()) {
                                showUnitSelector = true
                            }
                        }
                    ),
                    trailingIcon = if (selectedUnit.isNotEmpty()) {
                        { Text(selectedUnit, modifier = Modifier.padding(end = 12.dp)) }
                    } else null
                )
                Button(
                    onClick = { showUnitSelector = true },
                    modifier = Modifier.width(100.dp)
                ) {
                    Text(if (selectedUnit.isEmpty()) "Unit" else selectedUnit)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        itemNameFocus.requestFocus()
    }
}