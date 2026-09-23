package id.my.faruq.coffegrader.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateToHistory: () -> Unit,
    vm: ScanViewModel = hiltViewModel()
) {
    var batchName by remember { mutableStateOf("") }
    var totalBeansText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan & Grading") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = batchName,
                onValueChange = { batchName = it },
                label = { Text("Coffee Batch Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalBeansText,
                onValueChange = { totalBeansText = it },
                label = { Text("Total Sample Beans") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    vm.finishScan(
                        batchName = batchName,
                        totalBeans = totalBeansText.toIntOrNull() ?: 0,
                        onDone = { _ ->
                            onNavigateToHistory()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Scan Result")
            }
        }
    }
}
