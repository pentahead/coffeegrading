package id.my.faruq.coffegrader.ui.home


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import id.my.faruq.coffegrader.R
// ===== Dummy model (nanti ganti dari Room) =====
data class ScanHistoryItem(
    val id: String,
    val batchName: String,
    val dateTimeText: String,
    val gradeText: String,
    val gradeColor: Color
    // nanti tambah: thumbnailUri/path
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToCamera: () -> Unit,
    onGoToHistory: () -> Unit,
    onGoToAbout: () -> Unit,
    onRefreshHome: () -> Unit,
    onOpenHistoryDetail: (String) -> Unit) {
    // Dummy data untuk carousel (siap diganti dari DB)
    val dummyHistory = remember {
        listOf(
            ScanHistoryItem("SCAN_001", "SCAN_001", "08:15:22 / 2025-11-30", "1", Color(0xFF12A150)),
            ScanHistoryItem("SCAN_002", "SCAN_002", "08:15:22 / 2025-11-30", "4a", Color(0xFFF57C00)),
            ScanHistoryItem("SCAN_003", "SCAN_003", "08:15:22 / 2025-11-30", "1", Color(0xFF12A150)),
        )
    }

    Scaffold(
        topBar = {
            // Top bar kecil + tombol menu/about (sesuai mockup: 1 tombol menu ke about)
            CenterAlignedTopAppBar(
                title = { Text("Smart Bean Grading") },
                actions = {
                    IconButton(onClick = onGoToAbout) {
                        Icon(Icons.Filled.Info, contentDescription = "About")
                    }
                }
            )
        },
        bottomBar = {
            HomeBottomBar(
                onHome = onRefreshHome,
                onScan = onGoToCamera,
                onHistory = onGoToHistory
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ===== Header area (background image placeholder) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFB7F23A))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Smart Bean Grading",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Powered by real-time AI",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Placeholder gambar di kanan (nanti ganti Image painterResource)
                Image(
                    painter = painterResource(R.drawable.bgilustrasi),
                    contentDescription = "Header Illustration",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(110.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // ===== Scan button panel =====
            Spacer(Modifier.height(12.dp))
            ScanBigButton(
                onClick = onGoToCamera,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ===== Carousel Scan Terkini =====
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Scan Terkini",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dummyHistory) { item ->
                    HistoryCarouselCard(
                        item = item,
                        onClick = {
                            onOpenHistoryDetail(item.id)
                        }
                    )
                }

            }

            // (Optional) Section lain sesuai mockup kamu
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Standarisasi SNI",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ScanBigButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // tombol lingkaran besar “SCAN”
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB7F23A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCAN", fontWeight = FontWeight.Bold)
                    Text("Sekarang!", style = MaterialTheme.typography.bodySmall)
                }
            }

            // klik area card seluruhnya
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .padding(0.dp)
            ) {
                // invisible clickable overlay
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Transparent)
                ) { }
            }
        }
    }
}


@Composable
private fun HistoryCarouselCard(
    item: ScanHistoryItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick, // ⬅️ INI KUNCINYA
        modifier = Modifier.width(165.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(Color.Black.copy(alpha = 0.08f))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(item.gradeColor)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = item.batchName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.dateTimeText,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.gradeText,
                        fontWeight = FontWeight.Bold,
                        color = item.gradeColor
                    )
                }
            }
        }
    }
}


@Composable
private fun HomeBottomBar(
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
                .padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onHome) {
                    Icon(Icons.Filled.Home, contentDescription = "Beranda")
                }
                Text("Beranda", style = MaterialTheme.typography.labelSmall)
            }

            // tombol scan
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

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Filled.Description, contentDescription = "Riwayat")
                }
                Text("Riwayat", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
