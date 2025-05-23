package com.example.monday

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monday.ui.theme.KharchajiTheme

// Data class for expense categories
data class ExpenseCategory(
    val name: String,
    val icon: ImageVector
)

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

// Function to parse item text and extract category information
fun parseCategoryInfo(itemText: String): Pair<String, List<String>> {
    // Add debug logging
    Log.d("CategoryDebug", "Parsing categories from: $itemText")
    
    return if (itemText.contains("|CATS:")) {
        try {
            val parts = itemText.split("|CATS:")
            val displayText = parts[0]
            val categoryNames = parts[1].split(",").map { it.trim() }
            
            Log.d("CategoryDebug", "Found categories: $categoryNames")
            displayText to categoryNames
        } catch (e: Exception) {
            Log.e("CategoryDebug", "Error parsing categories: ${e.message}")
            itemText to emptyList()
        }
    } else {
        Log.d("CategoryDebug", "No categories found in item text")
        itemText to emptyList()
    }
}

@Composable
fun TodoItemRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit
) {
    // Parse the item text to extract category information
    val (baseText, categoryNames) = parseCategoryInfo(item.text)
    
    // Log what we're working with
    Log.d("CategoryDebug", "Item text: ${item.text}")
    Log.d("CategoryDebug", "Found categories: $categoryNames")
    
    // Extract name, quantity, and price
    val parts = baseText.split(" - ₹")
    val nameWithQuantity = parts[0].trim()
    val price = parts.getOrElse(1) { "0" }.trim()
    
    // Parse name and quantity
    val quantityRegex = Regex("""\((.*?)\)""")
    val quantityMatch = quantityRegex.find(nameWithQuantity)
    val name = if (quantityMatch != null) {
        nameWithQuantity.substring(0, quantityMatch.range.first).trim()
    } else {
        nameWithQuantity
    }
    val quantity = quantityMatch?.groupValues?.get(1)

    // Define all possible expense categories
    val allCategories = listOf(
        ExpenseCategory("Groceries", Icons.Outlined.ShoppingCart),
        ExpenseCategory("Food", Icons.Outlined.Restaurant),
        ExpenseCategory("Transport", Icons.Outlined.DirectionsCar),
        ExpenseCategory("Bills", Icons.Outlined.Receipt),
        ExpenseCategory("Shopping", Icons.Outlined.LocalMall),
        ExpenseCategory("Health", Icons.Outlined.Medication),
        ExpenseCategory("Education", Icons.Outlined.School),
        ExpenseCategory("Entertainment", Icons.Outlined.Movie),
        ExpenseCategory("Other", Icons.Outlined.MoreHoriz)
    )
    
    // Match category names to their icons
    val matchedCategories = allCategories.filter { category ->
        categoryNames.any { it.trim() == category.name }
    }
    
    // UI for the expense item
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Category row - always visible even if empty to help debug
            Surface(
                color = if (categoryNames.isEmpty()) 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else 
                    MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (categoryNames.isEmpty()) {
                        Text(
                            text = "No categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Show matched category icons
                        matchedCategories.take(3).forEach { category ->
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        // Show category names if no icons matched
                        if (matchedCategories.isEmpty() && categoryNames.isNotEmpty()) {
                            categoryNames.take(3).forEach { catName ->
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                        
                        // Show count for additional categories
                        if (categoryNames.size > 3) {
                            Text(
                                text = "+${categoryNames.size - 3} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Expense details row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                // Item details (name and quantity)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (!quantity.isNullOrBlank()) {
                        Text(
                            text = quantity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Price
                Text(
                    text = "₹$price",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Delete button
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // Add spacing between items
    Spacer(modifier = Modifier.height(8.dp))
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
    
    // Category selection - change to a Set to allow multiple selections
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf<Set<ExpenseCategory>>(emptySet()) }
    
    // List of predefined expense categories in a 3x3 grid
    val expenseCategories = remember {
        listOf(
            ExpenseCategory("Groceries", Icons.Outlined.ShoppingCart),
            ExpenseCategory("Food", Icons.Outlined.Restaurant),
            ExpenseCategory("Transport", Icons.Outlined.DirectionsCar),
            ExpenseCategory("Bills", Icons.Outlined.Receipt),
            ExpenseCategory("Shopping", Icons.Outlined.LocalMall),
            ExpenseCategory("Health", Icons.Outlined.Medication),
            ExpenseCategory("Education", Icons.Outlined.School),
            ExpenseCategory("Entertainment", Icons.Outlined.Movie),
            ExpenseCategory("Other", Icons.Outlined.MoreHoriz)
        )
    }
    
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
            // Store selected categories as metadata rather than text prefix
            val itemText = if (selectedQuantity.isNotEmpty()) {
                "$newItemText ($selectedQuantity) - ₹$itemPrice"
            } else {
                "$newItemText - ₹$itemPrice"
            }
            
            // Create a comma-separated list of category names for encoding (will be hidden)
            val categoryCodes = if (selectedCategories.isNotEmpty()) {
                // Add specific separator to ensure we can parse it later
                "|CATS:" + selectedCategories.joinToString(",") { it.name }
            } else {
                ""
            }
            
            // Debug log
            Log.d("CategoryDebug", "Adding item with categories: $categoryCodes")
            Log.d("CategoryDebug", "Complete item text: ${itemText + categoryCodes}")
            
            // Verify the category info parsing works correctly as a double-check
            val fullText = itemText + categoryCodes
            val (parsedBase, parsedCats) = parseCategoryInfo(fullText)
            Log.d("CategoryDebug", "Verifying parsing - Base: $parsedBase, Categories: $parsedCats")
            
            // Add the item with category metadata
            todoViewModel.addItem(TodoItem(text = fullText))
            
            newItemText = ""
            itemPrice = ""
            selectedQuantity = ""
            customQuantity = ""
            selectedUnit = ""
            // Keep the selected categories for the next entry
            itemNameFocus.requestFocus()
        }
    }
    
    // Category selection dialog
    if (showCategoryDialog) {
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Categories",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        items(expenseCategories) { category ->
                            CategoryItem(
                                category = category,
                                isSelected = selectedCategories.contains(category),
                                onClick = {
                                    selectedCategories = if (selectedCategories.contains(category)) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                }
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCategoryDialog = false }) {
                            Text("Done")
                        }
                    }
                }
            }
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
                label = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Item name", style = MaterialTheme.typography.titleMedium)
                        
                        // Display category icons in a more visible way
                        if (selectedCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Category icons in a horizontal row
                            selectedCategories.take(3).forEach { category ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = category.name,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            
                            if (selectedCategories.size > 3) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Text(
                                        text = "+${selectedCategories.size - 3}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                },
                leadingIcon = { 
                    Box(
                        modifier = Modifier
                            .clickable { showCategoryDialog = true }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Select categories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
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

@Composable
fun CategoryItem(
    category: ExpenseCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimary
                else 
                    MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
            
            // Show checkmark for selected category
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}