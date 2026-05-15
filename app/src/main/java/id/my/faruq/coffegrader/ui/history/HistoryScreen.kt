package id.my.faruq.coffegrader.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.my.faruq.coffegrader.util.GradeColors
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryItemUi(
    val id: String,
    val batchName: String,
    val dateTimeText: String,
    val totalBeans: Int,
    val defectScore: Double,
    val gradeText: String,
    val cardColor: Color,
    val thumbnailPath: String? = null
)

/** Quick filter tab: Semua, Mutu 1 .. Mutu 6 */
private val QUICK_MUTU_TABS = listOf(
    "" to "Semua",
    "1" to "Mutu 1",
    "2" to "Mutu 2",
    "3" to "Mutu 3",
    "4" to "Mutu 4 (Arabika)",
    "4a" to "Mutu 4a",
    "4b" to "Mutu 4b",
    "5" to "Mutu 5",
    "6" to "Mutu 6"
)

/** Urutkan: nama, tanggal, mutu */
private enum class SortOption(val label: String) {
    NameAsc("Nama A → Z"),
    NameDesc("Nama Z → A"),
    DateNewest("Tanggal Terbaru"),
    DateOldest("Tanggal Terlama"),
    MutuTertinggi("Mutu Tertinggi (1 dulu)"),
    MutuTerendah("Mutu Terendah (6 dulu)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: HistoryViewModel = hiltViewModel(),
    onOpenDetail: (String) -> Unit,
    onGoHome: () -> Unit,
    onGoScan: () -> Unit,
    onGoHistoryRefresh: () -> Unit,
    onGoAbout: () -> Unit
) {
    val entities by vm.historyList.collectAsState(initial = emptyList())

    // Filter state: tanggal (kalender), mutu (tabs), nama batch (search di top bar)
    var showDatePicker by remember { mutableStateOf(false) }
    var filterDate by remember { mutableStateOf("") }
    var filterMutu by remember { mutableStateOf("") }
    var filterBatchName by remember { mutableStateOf("") }

    // Sort state
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.DateNewest) }

    val uiItems = remember(entities) {
        entities.map { e ->
            HistoryItemUi(
                id = e.id.toString(),
                batchName = e.batchName,
                dateTimeText = e.dateTime,
                totalBeans = e.totalBeans,
                defectScore = e.defectScore,
                gradeText = e.gradeText,
                cardColor = Color(GradeColors.colorFor(e.gradeText)),
                thumbnailPath = e.thumbnailPath
            )
        }
    }

    fun normalizeGrade(s: String) = s.lowercase().trim().replace("mutu ", "").trim()

    fun parseDateFromDateTime(dateTimeText: String): String? {
        val t = dateTimeText.trim()
        if (t.contains(" / ")) {
            val part = t.split("/").map { it.trim() }.getOrNull(1) ?: return null
            return part.takeIf { it.length >= 8 }
        }
        if (t.contains(" ")) {
            val part = t.split(" ", limit = 2).firstOrNull() ?: return null
            if (part.length >= 8 && part.contains("-")) {
                val seg = part.split("-")
                if (seg.size == 3 && seg[0].length == 2) return "${seg[2]}-${seg[1]}-${seg[0]}"
                return part
            }
            return part
        }
        return t.takeIf { it.length >= 8 }
    }

    fun gradeOrder(gradeText: String): Int = when (normalizeGrade(gradeText)) {
        "1" -> 1
        "2" -> 2
        "3" -> 3
        "4", "4a", "4b" -> 4
        "5" -> 5
        "6" -> 6
        else -> 0
    }

    val shownItems = remember(uiItems, filterDate, filterMutu, filterBatchName, sortOption) {
        var list = uiItems
        if (filterDate.isNotBlank()) {
            val fd = filterDate.trim()
            list = list.filter { parseDateFromDateTime(it.dateTimeText)?.contains(fd) == true }
        }
        if (filterMutu.isNotBlank()) {
            val g = filterMutu.lowercase()
            list = list.filter { normalizeGrade(it.gradeText) == g }
        }
        if (filterBatchName.isNotBlank()) {
            val q = filterBatchName.trim().lowercase()
            list = list.filter { it.batchName.lowercase().contains(q) }
        }
        list = when (sortOption) {
            SortOption.NameAsc -> list.sortedBy { it.batchName.lowercase() }
            SortOption.NameDesc -> list.sortedByDescending { it.batchName.lowercase() }
            SortOption.DateNewest -> list.sortedByDescending { parseDateFromDateTime(it.dateTimeText) ?: "" }
            SortOption.DateOldest -> list.sortedBy { parseDateFromDateTime(it.dateTimeText) ?: "" }
            SortOption.MutuTertinggi -> list.sortedBy { gradeOrder(it.gradeText) }
            SortOption.MutuTerendah -> list.sortedByDescending { gradeOrder(it.gradeText) }
        }
        list
    }

    val initialDateMillis = remember(filterDate) {
        if (filterDate.isNotBlank()) {
            try {
                DATE_FORMAT.parse(filterDate)?.time
            } catch (_: Exception) { null }
        } else null
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        yearRange = (2020..2030)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            filterDate = DATE_FORMAT.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text("Riwayat", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onGoAbout) {
                            Icon(Icons.Filled.Info, contentDescription = "About")
                        }
                    }
                )
                OutlinedTextField(
                    value = filterBatchName,
                    onValueChange = { filterBatchName = it },
                    placeholder = { Text("Cari nama batch...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        bottomBar = {
            BottomNavBar(
                onHome = onGoHome,
                onScan = onGoScan,
                onHistory = onGoHistoryRefresh
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Baris Filter & Urutkan
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterDate.isNotBlank(),
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, Modifier.size(18.dp))
                        },
                        label = {
                            Text(
                                if (filterDate.isNotBlank()) filterDate else "Tanggal"
                            )
                        }
                    )
                    Box {
                        FilterChip(
                            selected = false,
                            onClick = { showSortMenu = true },
                            label = { Text(sortOption.label) },
                            trailingIcon = {
                                Icon(Icons.Filled.SwapVert, contentDescription = null, Modifier.size(18.dp))
                            }
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        sortOption = opt
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick filter tab: Semua, Mutu 1 .. Mutu 6
            ScrollableTabRow(
                selectedTabIndex = QUICK_MUTU_TABS.indexOfFirst { it.first == filterMutu }.takeIf { it >= 0 } ?: 0,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                QUICK_MUTU_TABS.forEachIndexed { index, (value, label) ->
                    Tab(
                        selected = filterMutu == value,
                        onClick = { filterMutu = value },
                        text = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (shownItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Tidak ada data",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ubah filter atau pastikan ada riwayat scan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shownItems) { item ->
                        HistoryCard(
                            item = item,
                            onClick = { onOpenDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

@Composable
private fun HistoryCard(
    item: HistoryItemUi,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = item.cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail atau placeholder (data tanpa gambar tetap ditampilkan)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.thumbnailPath != null -> {
                        val file = File(item.thumbnailPath)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Thumbnail",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("—", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    else -> Text("—", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.batchName, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(item.dateTimeText, color = Color.White, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text("Total Biji : ${item.totalBeans}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("Nilai Cacat : ${item.defectScore}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }

            // Mutu circle kanan: "Mutu" di atas, angka di bawah
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mutu", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Text(
                        GradeColors.gradeDisplayText(item.gradeText),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    onHome: () -> Unit,
    onScan: () -> Unit,
    onHistory: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item Beranda
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onHome) {
                    Icon(Icons.Filled.Home, contentDescription = "Beranda")
                }
                Text("Beranda", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.weight(1f))


            // Item Riwayat
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Filled.Description, contentDescription = "Riwayat")
                }
                Text("Riwayat", style = MaterialTheme.typography.labelSmall)
            }
        }
        Box(
            modifier = Modifier
                .offset(y = (-25).dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onScan,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB7F23A))
            ) {
                Icon(
                    imageVector = Icons.Filled.CenterFocusStrong,
                    contentDescription = "Scan",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Black
                )
            }

        }
        Text(
            text = "Scan",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
    }


