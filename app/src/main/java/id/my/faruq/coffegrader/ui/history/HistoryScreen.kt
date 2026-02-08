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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HistoryItemUi(
    val id: String,
    val batchName: String,
    val dateTimeText: String,
    val totalBeans: Int,
    val defectScore: Int,
    val gradeText: String,
    val cardColor: Color
)

private enum class MutuFilter(val label: String) {
    Semua("Semua"),
    Mutu1("Mutu 1"),
    Mutu2("Mutu 2"),
    Mutu3("Mutu 3"),
    Mutu4a("Mutu 4a"),
    Mutu5("Mutu 5"),
    Mutu6("Mutu 6"),
    Mutu7("Mutu 7")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenDetail: (String) -> Unit,
    onGoHome: () -> Unit,
    onGoScan: () -> Unit,
    onGoHistoryRefresh: () -> Unit,
    onGoAbout: () -> Unit
) {
    // Dummy list (nanti ganti dari Room)
    val allItems = remember {
        listOf(
            HistoryItemUi("SCAN_001","SCAN_001","08:15:22 / 2025-11-30",132,11,"1", Color(0xFF12A150)),
            HistoryItemUi("SCAN_002","SCAN_002","08:16:15 / 2025-11-30",132,11,"4a", Color(0xFFF57C00)),
            HistoryItemUi("SCAN_003","SCAN_003","08:18:42 / 2025-11-30",128,92,"5", Color(0xFF8E1B1B)),
            HistoryItemUi("SCAN_004","SCAN_004","08:20:29 / 2025-11-30",132,33,"3", Color(0xFFB45F06)),
            HistoryItemUi("SCAN_005","SCAN_005","08:40:00 / 2025-11-30",132,33,"3", Color(0xFFB45F06)),
        )
    }

    var selectedFilter by remember { mutableStateOf(MutuFilter.Semua) }

    val shownItems = remember(selectedFilter, allItems) {
        if (selectedFilter == MutuFilter.Semua) allItems
        else {
            val grade = selectedFilter.label.removePrefix("Mutu ").lowercase()
            allItems.filter { it.gradeText.lowercase() == grade }
        }
    }

    Scaffold(
        topBar = {
            // sesuai mockup: kiri "Riwayat", kanan menu/about
            TopAppBar(
                title = { Text("Riwayat", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onGoAbout) {
                        Icon(Icons.Filled.Info, contentDescription = "About")
                    }
                }
            )
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

            // baris Filter / Urutkan (UI saja dulu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { /* nanti */ }) {
                    Icon(Icons.Filled.Tune, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Filter")
                }
                TextButton(onClick = { /* nanti */ }) {
                    Text("Urutkan")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.SwapVert, contentDescription = null)
                }
            }

            // Tab mutu (scrollable kalau kepanjangan)
            ScrollableTabRow(
                selectedTabIndex = selectedFilter.ordinal,
                edgePadding = 16.dp
            ) {
                MutuFilter.entries.forEach { f ->
                    Tab(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        text = { Text(f.label) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // List riwayat
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 100.dp // biar ga ketutup bottom nav
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
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.batchName, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(item.dateTimeText, color = Color.White, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text("Total Biji : ${item.totalBeans}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("Nilai Cacat : ${item.defectScore}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }

            // Mutu circle kanan
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mutu", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Text(item.gradeText, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
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
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(vertical = 10.dp),
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

            // Item Scan
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onScan,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB7F23A))
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Scan")
                }
            }

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
    }

}
