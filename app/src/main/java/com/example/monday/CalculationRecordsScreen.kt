package com.example.monday

import androidx.compose.foundation.layout.* 
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationRecordsScreen(
    todoViewModel: TodoViewModel,
    onNavigateBack: () -> Unit,
    onRecordClick: (Int) -> Unit // Callback for when a record is clicked, passing its ID
) {
    val records by todoViewModel.allCalculationRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculation Records") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Potentially add a "Delete All Records" button here later
                }
            )
        }
    ) { paddingValues ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No calculation records found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    CalculationRecordItem(record = record, onClick = { onRecordClick(record.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationRecordItem(
    record: CalculationRecord,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Record ID: ${record.id} - Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Total Sum: ₹${String.format("%.2f", record.totalSum)}")
            Text("Checked Items: ${record.checkedItemsCount} (Sum: ₹${String.format("%.2f", record.checkedItemsSum)})")
            // Further details like the list of items can be shown on a dedicated detail screen
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationRecordDetailScreen(
    recordId: Int,
    todoViewModel: TodoViewModel,
    onNavigateBack: () -> Unit,
    onSetMemoAndReturnToExpenses: () -> Unit // New callback
) {
    val recordFromState by todoViewModel.getCalculationRecordById(recordId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            // Capture recordFromState for use in this scope to enable smart casting
            val currentRecord = recordFromState
            TopAppBar(
                title = { Text(if (currentRecord != null) "Record Details (ID: ${currentRecord.id})" else "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Capture recordFromState again for this content scope to enable smart casting
        val currentRecord = recordFromState
        if (currentRecord == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                item {
                    Text("Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(currentRecord.timestamp))}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total Sum: ₹${String.format("%.2f", currentRecord.totalSum)}", style = MaterialTheme.typography.titleMedium)
                    Text("Checked Items: ${currentRecord.checkedItemsCount} (Sum: ₹${String.format("%.2f", currentRecord.checkedItemsSum)})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Items:", style = MaterialTheme.typography.headlineSmall)
                }
                items(currentRecord.items, key = { item -> "${currentRecord.id}_${item.description}_${item.price}" }) { item ->
                    RecordDetailListItem(item = item)
                }
                 item {
                    Spacer(modifier = Modifier.height(16.dp)) // Add some space before buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly // Space out buttons
                    ) {
                        Button(
                            onClick = { 
                                todoViewModel.loadRecordItemsAsCurrentExpenses(currentRecord.items)
                                onSetMemoAndReturnToExpenses() // Call the new callback
                            },
                            modifier = Modifier.weight(1f).padding(end = 4.dp) // Make buttons take equal space
                        ) {
                            Text("Set This Memo")
                        }
                        Button(
                            onClick = { 
                                todoViewModel.deleteCalculationRecordById(currentRecord.id)
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).padding(start = 4.dp) // Make buttons take equal space
                        ) {
                            Text("Delete This Record")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordDetailListItem(item: RecordItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        item.quantity?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "₹${item.price}",
            style = MaterialTheme.typography.bodyLarge
        )
        if(item.isChecked){
            Icon(Icons.Filled.Check, contentDescription = "Checked", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 8.dp))
        }
    }
    Divider()
}
