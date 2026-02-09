package id.my.faruq.coffegrader.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccessTime
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


// ===== Data model UI (nanti ganti dari Room) =====
data class DefectRowUi(
    val no: Int,
    val defectName: String,
    val defectScore: Float, // nilai cacat per biji/kejadian
    val count: Int,         // jumlah biji
    val totalScore: Float   // total nilai cacat
)

data class HistoryDetailUi(
    val id: String,
    val batchName: String,
    val timeText: String,    // "08:15:22"
    val dateText: String,    // "30-11-2025"
    val gradeText: String,   // "1" / "4a"
    val themeColor: Color,   // warna sesuai card
    val totalBeans: Int,
    val defectiveBeans: Int,
    val defectScoreTotal: Float,
    val dominantDefect: String,
    val scanDurationMs: Int,
    val officerName: String,
    val defects: List<DefectRowUi>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    scanId: String,
    onBack: () -> Unit
) {
    // ===== Dummy fetch berdasarkan scanId (nanti ganti dari DB) =====
    val detail = remember(scanId) {
        // contoh mapping warna berdasarkan grade/ID
        val color = when (scanId) {
            "SCAN_001" -> Color(0xFF12A150)
            "SCAN_002" -> Color(0xFFF57C00)
            "SCAN_003" -> Color(0xFF8E1B1B)
            else -> Color(0xFF12A150)
        }

        HistoryDetailUi(
            id = scanId,
            batchName = scanId,
            timeText = "08:15:22",
            dateText = "30-11-2025",
            gradeText = when (scanId) { "SCAN_002" -> "4a" else -> "1" },
            themeColor = color,
            totalBeans = 132,
            defectiveBeans = 13,
            defectScoreTotal = 11f,
            dominantDefect = "Biji Hitam",
            scanDurationMs = 232,
            officerName = "Mulyadi",
            defects = listOf(
                DefectRowUi(1, "Biji Hitam", 1f, 9, 9f),
                DefectRowUi(2, "Biji Hitam Sebagian", 0.5f, 2, 1f),
                DefectRowUi(3, "Kulit Kopi Ukuran Sedang", 0.5f, 2, 1f),
            )
        )
    }

    // ===== State untuk edit nama & zoom gambar =====
    var currentBatchName by remember { mutableStateOf(detail.batchName) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(currentBatchName) }

    var showImageZoom by remember { mutableStateOf(false) }

    // ===== Scroll =====
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            // Atas mirip riwayat: title kiri, menu kanan (kalau mau)
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

            // 1) Tombol kembali (ke History)
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                Spacer(Modifier.width(6.dp))
                Text("Kembali")
            }

            Spacer(Modifier.height(10.dp))

            // 2) Kotak utama dengan border warna sesuai item
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, detail.themeColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {

                    // 3) Judul batch editable + waktu & tanggal
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val chipColors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Transparent
                        )
                        // Chip untuk Jam
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            colors = chipColors,
                            label = { Text(detail.timeText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Time Icon",
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )

                        Spacer(Modifier.width(8.dp))

                        // Chip untuk Kalender
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            colors = chipColors,
                            label = { Text(detail.dateText) },
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

                    // 4) Kiri gambar (clickable zoom) & kanan mutu (lingkaran)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Image placeholder (nanti ganti thumbnail dari file/db)
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

                        Spacer(Modifier.width(14.dp))

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(detail.themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Mutu", color = Color.White)
                                Text(
                                    detail.gradeText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 5) Detail Informasi
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
                            InfoRow("Waktu", "${detail.timeText} WIB")
                            InfoRow("Tanggal", detail.dateText)
                            InfoRow("Mutu Biji", "Mutu ${detail.gradeText}")
                            InfoRow("Total Biji", detail.totalBeans.toString())
                            InfoRow("Jumlah Biji Cacat", detail.defectiveBeans.toString())
                            InfoRow("Nilai Cacat", detail.defectScoreTotal.toString())
                            InfoRow("Cacat Dominan", detail.dominantDefect)
                            InfoRow("Durasi Scan", "${detail.scanDurationMs} ms")
                            InfoRow("Petugas", detail.officerName)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 5) Detail Cacat (table)
                    Text(
                        "Detail Cacat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    DefectTable(defects = detail.defects)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    // ===== Dialog edit batch name =====
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

    // ===== Zoom image dialog (placeholder) =====
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell("No", 40.dp, isHeader = true)
                TableCell("Nama Jenis Cacat", 220.dp, isHeader = true)
                TableCell("Nilai Cacat", 90.dp, isHeader = true)
                TableCell("Jumlah Biji", 90.dp, isHeader = true)
                TableCell("Total Nilai", 100.dp, isHeader = true)
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
        style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
    )
}
