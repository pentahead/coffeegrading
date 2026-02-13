package id.my.faruq.coffegrader.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.my.faruq.coffegrader.data.repository.DefectRowUi
import id.my.faruq.coffegrader.data.repository.HistoryDetailUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    scanId: String,
    vm: HistoryDetailViewModel,
    onBack: () -> Unit
) {
    // ===== ambil data asli dari ViewModel =====
    val defects by vm.defects.collectAsState()
    val detail by vm.detail.collectAsState()

    // ===== load data sekali =====
    LaunchedEffect(scanId) {
        vm.load(scanId.toLong())
    }

    // ===== kalau detail belum ada, tampilkan loading =====
    if (detail == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // ===== sudah aman karena detail != null =====
    val safeDetail = detail!!

    // ===== State edit batch name =====
    var currentBatchName by remember { mutableStateOf(safeDetail.batchName) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(currentBatchName) }

    var showImageZoom by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
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
                border = BorderStroke(2.dp, safeDetail.themeColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {

                    // ===== Batch Name =====
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentBatchName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                editText = currentBatchName
                                showEditDialog = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Batch")
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // ===== Time & Date =====
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val chipColors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Transparent
                        )

                        AssistChip(
                            onClick = {},
                            enabled = false,
                            colors = chipColors,
                            label = { Text(safeDetail.timeText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Time Icon",
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )

                        Spacer(Modifier.width(8.dp))

                        AssistChip(
                            onClick = {},
                            enabled = false,
                            colors = chipColors,
                            label = { Text(safeDetail.dateText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Date Icon",
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== Image + Grade Circle =====
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {

                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.06f))
                                .clickable { showImageZoom = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Foto", color = Color.Black.copy(alpha = 0.55f))
                        }

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(safeDetail.themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Mutu", color = Color.White)
                                Text(
                                    safeDetail.gradeText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== Detail Informasi =====
                    Text(
                        "Detail Informasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InfoRow("Batch ID", currentBatchName)
                            InfoRow("Waktu", "${safeDetail.timeText} WIB")
                            InfoRow("Tanggal", safeDetail.dateText)
                            InfoRow("Mutu Biji", "Mutu ${safeDetail.gradeText}")
                            InfoRow("Total Biji", safeDetail.totalBeans.toString())
                            InfoRow("Jumlah Biji Cacat", safeDetail.defectiveBeans.toString())
                            InfoRow("Nilai Cacat", safeDetail.defectScoreTotal.toString())
                            InfoRow("Cacat Dominan", safeDetail.dominantDefect)
                            InfoRow("Durasi Scan", "${safeDetail.scanDurationMs} ms")
                            InfoRow("Petugas", safeDetail.officerName)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== Defect Table =====
                    Text(
                        "Detail Cacat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    DefectTable(defects = defects)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    // ===== Dialog Edit Batch =====
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Ubah Nama Batch") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true,
                    label = { Text("Nama Batch") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    currentBatchName = editText.trim().ifEmpty { currentBatchName }
                    showEditDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Batal") }
            }
        )
    }

    // ===== Zoom Dialog =====
    if (showImageZoom) {
        Dialog(onDismissRequest = { showImageZoom = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Zoom Foto (nanti isi Image)", color = Color.White)
            }
        }
    }
}

/* ===== Helper UI ===== */

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(140.dp),
            color = Color.Black.copy(alpha = 0.70f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = ":",
            modifier = Modifier.width(12.dp),
            color = Color.Black.copy(alpha = 0.70f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DefectTable(defects: List<DefectRowUi>) {
    val scroll = rememberScrollState()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(12.dp)
        ) {

            Row(modifier = Modifier.fillMaxWidth()) {
                TableCell("No", 40.dp, true)
                TableCell("Nama Jenis Cacat", 220.dp, true)
                TableCell("Nilai Cacat", 90.dp, true)
                TableCell("Jumlah Biji", 90.dp, true)
                TableCell("Total Nilai", 100.dp, true)
            }

            Spacer(Modifier.height(8.dp))

            defects.forEach { d ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableCell(d.no.toString(), 40.dp)
                    TableCell(d.defectName, 220.dp)
                    TableCell(d.defectScore.toString(), 90.dp)
                    TableCell(d.count.toString(), 90.dp)
                    TableCell(d.totalScore.toString(), 100.dp)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: Dp,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = if (isHeader)
            MaterialTheme.typography.labelMedium
        else
            MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader)
            FontWeight.SemiBold
        else
            FontWeight.Normal
    )
}
