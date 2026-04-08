package id.my.faruq.coffegrader.ui.home

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import id.my.faruq.coffegrader.R
import id.my.faruq.coffegrader.util.GradeColors
import java.io.File
// ===== Model untuk item scan di carousel =====
data class ScanHistoryItem(
    val id: String,
    val batchName: String,
    val dateTimeText: String,
    val gradeText: String,
    val gradeColor: Color,
    val thumbnailPath: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToCamera: () -> Unit,
    onGoToHistory: () -> Unit,
    onGoToAbout: () -> Unit,
    onGoToTutorial: () -> Unit,
    onGoToSni: () -> Unit,
    onRefreshHome: () -> Unit,
    onOpenHistoryDetail: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val recentScans by vm.recentScans.collectAsState(initial = emptyList())
    val contentScroll = rememberScrollState()

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
                .verticalScroll(contentScroll)
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

// ===== Tutorial Scan bar =====
Spacer(Modifier.height(12.dp))

TutorialScanRow(
    onClick = {
       onGoToTutorial() 
    },
    modifier = Modifier.padding(horizontal = 16.dp)
)


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
            Spacer(Modifier.height(10.dp))
            SniInfoCard(
                onClick = onGoToSni,
                modifier = Modifier.padding(horizontal = 16.dp)
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
    // 1. Setup Animasi untuk Gelombang
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f, // Seberapa besar gelombang memencar
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

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
            // 2. Lingkaran Gelombang (Digambar di belakang tombol)
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(
                    color = Color(0xFFB7F23A),
                    radius = (size.minDimension / 2) * scale,
                    alpha = alpha
                )
            }

            // 3. Tombol "SCAN" Lingkaran Hijau
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB7F23A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCAN", 
                        fontWeight = FontWeight.ExtraBold, 
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Sekarang!", 
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // 4. Klik Area (Overlay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent)
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
            // Area gambar: thumbnail scan atau placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White)
            ) {
                if (item.thumbnailPath != null) {
                    val file = File(item.thumbnailPath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.bgilustrasi),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(R.drawable.bgilustrasi),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
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
private fun TutorialScanRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Icon tutorial
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB7F23A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tutorial",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Text tutorial
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Tutorial Scan",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Pelajari cara scan biji kopi yang benar sesuai SNI",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Go",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SniInfoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.White)
            ) {
                // Sementara pakai drawable lokal. Nanti bisa diganti dengan resource/path gambar SNI yang Anda kirim.
                Image(
                    painter = painterResource(R.drawable.bgilustrasi),
                    contentDescription = "SNI Card",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SNI 01-2907-2008 Kopi",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Go",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
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