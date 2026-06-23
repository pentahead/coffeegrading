package id.my.faruq.coffegrader.ui.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import kotlinx.coroutines.launch

private val CardBorderColor = SampleAccentColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SampleInputScreen(
    vm: SampleInputViewModel,
    onNextToScan: (Long) -> Unit,
    onBack: () -> Unit = {}
) {
    val state by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var submitAttempted by remember { mutableStateOf(false) }
    var batchTouched by remember { mutableStateOf(false) }
    var moistureTouched by remember { mutableStateOf(false) }
    var dirtTouched by remember { mutableStateOf(false) }

    val batchBringIntoView = remember { BringIntoViewRequester() }
    val moistureBringIntoView = remember { BringIntoViewRequester() }
    val dirtBringIntoView = remember { BringIntoViewRequester() }

    val isFormValid = vm.isFormValid()

    val batchError = when {
        !(submitAttempted || batchTouched) -> null
        state.batchId.isBlank() -> "Batch ID wajib diisi"
        else -> null
    }
    val moistureError = when {
        !(submitAttempted || moistureTouched) -> null
        state.moisture.isBlank() -> "Kadar air wajib diisi"
        state.moisture.toFloatOrNull() == null -> "Format angka tidak valid"
        (state.moisture.toFloatOrNull() ?: 0f) > 12.5f -> "Kadar air maksimal 12.5%"
        (state.moisture.toFloatOrNull() ?: 0f) < 0f -> "Kadar air tidak boleh negatif"
        else -> null
    }
    val dirtError = when {
        !(submitAttempted || dirtTouched) -> null
        state.dirt.isBlank() -> "Kadar kotoran wajib diisi"
        state.dirt.toFloatOrNull() == null -> "Format angka tidak valid"
        (state.dirt.toFloatOrNull() ?: 0f) > 0.5f -> "Kadar kotoran maksimal 0.5%"
        (state.dirt.toFloatOrNull() ?: 0f) < 0f -> "Kadar kotoran tidak boleh negatif"
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
                title = { Text("Input Sampel SNI", fontWeight = FontWeight.Bold) }
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

                    SampleLabeledTextField(
                        label = "Batch ID / Kode Sampel",
                        value = state.batchId,
                        onValueChange = {
                            batchTouched = true
                            vm.updateBatchId(it)
                        },
                        placeholder = "Masukkan nama batch atau kode sample",
                        modifier = Modifier.bringIntoViewRequester(batchBringIntoView),
                        isError = batchError != null,
                        errorText = batchError
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

                    SampleLabeledTextField(
                        label = "Kadar Air (%) max 12.5",
                        value = state.moisture,
                        onValueChange = {
                            moistureTouched = true
                            vm.updateMoisture(it)
                        },
                        placeholder = "Masukkan kadar air",
                        modifier = Modifier.bringIntoViewRequester(moistureBringIntoView),
                        isError = moistureError != null,
                        errorText = moistureError
                    )

                    SampleLabeledTextField(
                        label = "Kadar Kotoran (%) max 0.5",
                        value = state.dirt,
                        onValueChange = {
                            dirtTouched = true
                            vm.updateDirt(it)
                        },
                        placeholder = "Masukkan kadar kotoran",
                        modifier = Modifier.bringIntoViewRequester(dirtBringIntoView),
                        isError = dirtError != null,
                        errorText = dirtError
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            submitAttempted = true
                            batchTouched = true
                            moistureTouched = true
                            dirtTouched = true

                            if (!isFormValid) {
                                val targetRequester = when {
                                    state.batchId.isBlank() -> batchBringIntoView
                                    state.moisture.isBlank() ||
                                            state.moisture.toFloatOrNull() == null ||
                                            (state.moisture.toFloatOrNull() ?: 0f) > 12.5f ||
                                            (state.moisture.toFloatOrNull() ?: 0f) < 0f -> moistureBringIntoView
                                    else -> dirtBringIntoView
                                }
                                coroutineScope.launch {
                                    targetRequester.bringIntoView()
                                }
                                return@Button
                            }

                            vm.saveSample { id ->
                                onNextToScan(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFormValid) {
                                CardBorderColor
                            } else {
                                CardBorderColor.copy(alpha = 0.38f)
                            },
                            contentColor = Color.White
                        )
                    ) {
                        Text("Lanjut ke Scan")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
