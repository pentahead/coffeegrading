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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Input Sampel Sebelum Grading",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = state.batchId,
            onValueChange = { vm.updateBatchId(it) },
            label = { Text("Batch ID / Kode Sampel") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                vm.saveSample { id ->
                    onNextToScan(id)
                }
            },
            enabled = state.batchId.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lanjut ke Scan")
        }
    }
}
