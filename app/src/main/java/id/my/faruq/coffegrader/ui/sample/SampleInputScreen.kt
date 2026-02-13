package id.my.faruq.coffegrader.ui.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SampleInputScreen(
    vm: SampleInputViewModel,
    onNextToScan: (Long) -> Unit
) {
    val state by vm.uiState.collectAsState()

    val beanSizeOptions =
        if (state.processingMethod == "Wet")
            listOf("Besar", "Sedang", "Kecil")
        else
            listOf("Besar", "Kecil")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            "Input Sampel Sebelum Grading",
            style = MaterialTheme.typography.titleLarge
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

        Divider()

        Text("Mutu Umum (Checklist)", style = MaterialTheme.typography.titleMedium)

        Row {
            Checkbox(
                checked = state.hasInsect,
                onCheckedChange = vm::toggleInsect
            )
            Text("Ada serangga hidup")
        }

        Row {
            Checkbox(
                checked = state.hasMoldSmell,
                onCheckedChange = vm::toggleMold
            )
            Text("Ada bau kapang/busuk")
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

        Button(
            onClick = {
                vm.saveSample { id ->
                    onNextToScan(id)
                }
            },
            enabled = vm.isFormValid(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lanjut ke Scan")
        }
    }
}
