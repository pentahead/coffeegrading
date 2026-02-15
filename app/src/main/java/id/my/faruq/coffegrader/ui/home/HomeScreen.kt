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
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import id.my.faruq.coffegrader.R
import id.my.faruq.coffegrader.util.GradeColors
// ===== Model untuk item scan di carousel =====
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
    onOpenHistoryDetail: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val recentScans by vm.recentScans.collectAsState(initial = emptyList())

    Scaffold(

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
                    .height(210.dp)
                    .background(Color(0xFFB7F23A))
                    .padding(top =  20.dp, end = 16.dp, start = 16.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Bean Grading",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Powered by real-time AI",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    IconButton(
                        onClick = onGoToAbout,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "About",
                            tint = Color.Black
                        )
                    }
                }

                Image(
                    painter = painterResource(R.drawable.bgilustrasi),
                    contentDescription = "Header Illustration",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(180.dp)
                        .padding(top = 20.dp, end = 20.dp, bottom = 0.dp),
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
                items(recentScans) { item ->
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
            // tombol “SCAN”
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
        onClick = onClick,
        modifier = Modifier.width(165.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Area gambar (putih) - ~60–70% tinggi kartu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(R.drawable.bgilustrasi),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Area info (hijau): batch, tanggal, lingkaran mutu di kanan (tidak nabrak)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(item.gradeColor)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = item.batchName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.dateTimeText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, item.gradeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = GradeColors.gradeDisplayText(item.gradeText),
                            fontWeight = FontWeight.Bold,
                            color = item.gradeColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
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