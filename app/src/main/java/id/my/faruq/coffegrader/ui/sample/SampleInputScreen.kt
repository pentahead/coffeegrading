package id.my.faruq.coffegrader.ui.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val CardBorderColor = Color(0xFF12A150)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleInputScreen(
    vm: SampleInputViewModel,
    onNextToScan: (Long) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var submitAttempted by remember { mutableStateOf(false) }
    var batchTouched by remember { mutableStateOf(false) }
    var moistureTouched by remember { mutableStateOf(false) }
    var dirtTouched by remember { mutableStateOf(false) }

    val batchError = when {
        !(submitAttempted || batchTouched) -> null
        state.batchId.isBlank() -> "Batch ID is required"
        else -> null
    }
    val moistureError = when {
        !(submitAttempted || moistureTouched) -> null
        state.moisture.isBlank() -> "Moisture content is required"
        state.moisture.toFloatOrNull() == null -> "Invalid number format"
        (state.moisture.toFloatOrNull() ?: 0f) > 12.5f -> "Moisture content must be at most 12.5%"
        (state.moisture.toFloatOrNull() ?: 0f) < 0f -> "Moisture content cannot be negative"
        else -> null
    }
    val dirtError = when {
        !(submitAttempted || dirtTouched) -> null
        state.dirt.isBlank() -> "Impurity content is required"
        state.dirt.toFloatOrNull() == null -> "Invalid number format"
        (state.dirt.toFloatOrNull() ?: 0f) > 0.5f -> "Impurity content must be at most 0.5%"
        (state.dirt.toFloatOrNull() ?: 0f) < 0f -> "Impurity content cannot be negative"
        else -> null
    }

    val beanSizeOptions =
        if (state.processingMethod == "Wet")
            listOf("Besar", "Sedang", "Kecil")
        else
            listOf("Besar", "Kecil")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SNI Sample Input", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                Spacer(Modifier.width(6.dp))
                Text("Back")
            }

            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        "Sample Input Before Grading",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = state.batchId,
                        onValueChange = {
                            batchTouched = true
                            vm.updateBatchId(it)
                        },
                        label = { Text("Batch ID / Sample Code") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = batchError != null,
                        supportingText = {
                            if (batchError != null) {
                                Text(batchError)
                            }
                        }
                    )

                    DropdownField(
                        label = "Coffee Type",
                        options = listOf("Robusta", "Arabika"),
                        selected = state.coffeeType,
                        onSelected = vm::updateCoffeeType
                    )

                    DropdownField(
                        label = "Processing Method",
                        options = listOf("Dry", "Wet"),
                        selected = state.processingMethod,
                        onSelected = vm::updateProcessing
                    )

                    DropdownField(
                        label = "Bean Size (SNI)",
                        options = beanSizeOptions,
                        selected = state.beanSize,
                        onSelected = vm::updateBeanSize
                    )

                    DropdownField(
                        label = "Bean Shape",
                        options = listOf("Normal", "Peaberry", "Polyembrio"),
                        selected = state.beanShape,
                        onSelected = vm::updateBeanShape
                    )

                    DropdownField(
                        label = "Sample Sorting Type",
                        options = listOf("Primer", "Sekunder", "Asalan", "Campuran"),
                        selected = state.sortationType,
                        onSelected = vm::updateSortation
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.Black.copy(alpha = 0.12f)
                    )

                    Text(
                        "General Quality (Checklist)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.hasInsect,
                            onCheckedChange = vm::toggleInsect
                        )
                        Text("Live insects present", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.hasMoldSmell,
                            onCheckedChange = vm::toggleMold
                        )
                        Text("Moldy/rotten smell present", style = MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedTextField(
                        value = state.moisture,
                        onValueChange = {
                            moistureTouched = true
                            vm.updateMoisture(it)
                        },
                        label = { Text("Moisture Content (%) max 12.5") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = moistureError != null,
                        supportingText = {
                            if (moistureError != null) {
                                Text(moistureError)
                            }
                        }
                    )

                    OutlinedTextField(
                        value = state.dirt,
                        onValueChange = {
                            dirtTouched = true
                            vm.updateDirt(it)
                        },
                        label = { Text("Impurity Content (%) max 0.5") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = dirtError != null,
                        supportingText = {
                            if (dirtError != null) {
                                Text(dirtError)
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            submitAttempted = true
                            vm.saveSample { id ->
                                onNextToScan(id)
                            }
                        },
                        enabled = vm.isFormValid(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBorderColor)
                    ) {
                        Text("Continue to Scan")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
