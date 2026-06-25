package id.my.faruq.coffegrader.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.my.faruq.coffegrader.data.repository.DefectRowUi
import id.my.faruq.coffegrader.data.repository.HistoryDetailUi
import id.my.faruq.coffegrader.util.GradeColors
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File

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

                    // ===== Waktu (jam:menit:detik) & Tanggal =====
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
                                    contentDescription = "Waktu",
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
                                    contentDescription = "Tanggal",
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {

                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.06f))
                                .clickable { if (safeDetail.imagePath != null) showImageZoom = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val imagePath = safeDetail.imagePath
                            if (imagePath != null) {
                                val file = File(imagePath)
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Foto scan",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text("Foto", color = Color.Black.copy(alpha = 0.55f))
                                }
                            } else {
                                Text("Foto", color = Color.Black.copy(alpha = 0.55f))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(safeDetail.themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Mutu", color = Color.White, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    GradeColors.gradeDisplayText(safeDetail.gradeText),
                                    color = Color.White,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== Info Sampel - jika ada =====
                    safeDetail.sampleInfo?.let { sample ->
                        Text(
                            "Info Sampel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InfoRow("Batch ID Sampel",  currentBatchName)
                                InfoRow("Jenis Kopi", sample.coffeeType)
                                InfoRow("Metode Pengolahan", sample.processingMethod)
                                InfoRow("Ukuran Biji", sample.beanSize)
                                InfoRow("Bentuk Biji", sample.beanShape)
                                InfoRow("Jenis Sortasi", sample.sortationType)
                                InfoRow("Serangga hidup", if (sample.hasInsect) "Ya" else "Tidak")
                                InfoRow("Bau kapang/busuk", if (sample.hasMoldSmell) "Ya" else "Tidak")
                                InfoRow("Kadar Air (%)", sample.moistureContent.toString())
                                InfoRow("Kadar Kotoran (%)", sample.dirtContent.toString())
                                sample.origin?.takeIf { it.isNotBlank() }?.let { InfoRow("Asal", it) }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

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
                            InfoRow("Mutu Biji", "${safeDetail.gradeText}")
                            InfoRow("Total Biji", safeDetail.totalBeans.toString())
                            InfoRow("Jumlah Biji Cacat", safeDetail.defectiveBeans.toString())
                            InfoRow("Nilai Cacat", safeDetail.defectScoreTotal.toString())
                            InfoRow("Cacat Dominan", safeDetail.dominantDefect)
                            InfoRow("Waktu Inferensi", "${safeDetail.scanDurationMs} ms")
                            InfoRow(
                                "Confidence Score",
                                "${"%.1f".format(safeDetail.confidenceScoreTotal * 100)}%"
                            )
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
                    val newName = editText.trim().ifEmpty { currentBatchName }
                    vm.updateBatchName(scanId.toLong(), newName) {
                        currentBatchName = newName
                        showEditDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Batal") }
            }
        )
    }

    // ===== Zoom Dialog (hanya jika ada gambar) =====
    val zoomImagePath = safeDetail.imagePath
    if (showImageZoom && zoomImagePath != null && File(zoomImagePath).exists()) {
        Dialog(
            onDismissRequest = { showImageZoom = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ZoomImageContent(
                imagePath = zoomImagePath,
                onDismiss = { showImageZoom = false }
            )
        }
    }
}

@Composable
private fun ZoomImageContent(
    imagePath: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformState)
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Foto scan zoom",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text("Tutup", color = Color.White)
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
    val totalTableWidth = 540.dp

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(totalTableWidth)
                        .horizontalScroll(scroll)
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

                // Fade kanan saat masih ada kolom tersembunyi di sisi kanan.
                if (scroll.maxValue > 0 && scroll.value < scroll.maxValue) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(26.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFF7F7F7)
                                    )
                                )
                            )
                    )
                }
            }

            // Scrollbar horizontal (custom) di bawah tabel.
            if (scroll.maxValue > 0) {
                val progress =
                    if (scroll.maxValue == 0) 0f else scroll.value.toFloat() / scroll.maxValue.toFloat()
                val thumbWidthFraction = 0.28f

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = 0.12f))
                ) {
                    val maxThumbOffset = maxWidth * (1f - thumbWidthFraction)
                    val thumbOffset = maxThumbOffset * progress
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(thumbWidthFraction)
                            .offset(x = thumbOffset)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.Black.copy(alpha = 0.32f))
                    )
                }
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
