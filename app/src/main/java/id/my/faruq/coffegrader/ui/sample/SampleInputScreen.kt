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

    val beanSizeOptions =
        if (state.processingMethod == "Wet")
            listOf("Besar", "Sedang", "Kecil")
        else
            listOf("Besar", "Kecil")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Sampel SNI", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                Spacer(Modifier.width(6.dp))
                Text("Kembali")
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
                        "Input Sampel Sebelum Grading",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = state.batchId,
                        onValueChange = vm::updateBatchId,
                        label = { Text("Batch ID / Kode Sampel") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownField(
                        label = "Jenis Kopi",
                        options = listOf("Robusta", "Arabika"),
                        selected = state.coffeeType,
                        onSelected = vm::updateCoffeeType
                    )

                    DropdownField(
                        label = "Metode Pengolahan",
                        options = listOf("Dry", "Wet"),
                        selected = state.processingMethod,
                        onSelected = vm::updateProcessing
                    )

                    DropdownField(
                        label = "Ukuran Biji (SNI)",
                        options = beanSizeOptions,
                        selected = state.beanSize,
                        onSelected = vm::updateBeanSize
                    )

                    DropdownField(
                        label = "Bentuk Biji",
                        options = listOf("Normal", "Peaberry", "Polyembrio"),
                        selected = state.beanShape,
                        onSelected = vm::updateBeanShape
                    )

                    DropdownField(
                        label = "Jenis Sortasi Sampel",
                        options = listOf("Primer", "Sekunder", "Asalan", "Campuran"),
                        selected = state.sortationType,
                        onSelected = vm::updateSortation
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.Black.copy(alpha = 0.12f)
                    )

                    Text(
                        "Mutu Umum (Checklist)",
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
                        Text("Ada serangga hidup", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.hasMoldSmell,
                            onCheckedChange = vm::toggleMold
                        )
                        Text("Ada bau kapang/busuk", style = MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedTextField(
                        value = state.moisture,
                        onValueChange = vm::updateMoisture,
                        label = { Text("Kadar Air (%) max 12.5") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.dirt,
                        onValueChange = vm::updateDirt,
                        label = { Text("Kadar Kotoran (%) max 0.5") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            vm.saveSample { id ->
                                onNextToScan(id)
                            }
                        },
                        enabled = vm.isFormValid(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBorderColor)
                    ) {
                        Text("Lanjut ke Scan")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
