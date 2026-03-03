package id.my.faruq.coffegrader.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onBack: () -> Unit,
    onStartScan: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tutorial Scan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            Text(
                text = "Panduan Penggunaan CoffeeGrader",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Ikuti langkah berikut agar hasil scan biji kopi sesuai standar SNI.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(18.dp))

            TutorialStepCard(
                step = "1",
                title = "Siapkan Sampel Kopi",
                desc = "Gunakan biji kopi ±300 gram dan pastikan biji tidak bertumpuk."
            )

            TutorialStepCard(
                step = "2",
                title = "Masuk Menu Scan",
                desc = "Tekan tombol SCAN di halaman utama untuk membuka kamera."
            )

            TutorialStepCard(
                step = "3",
                title = "Posisikan Biji di Dalam Grid",
                desc = "Letakkan biji kopi di area kotak putih agar deteksi lebih akurat."
            )

            TutorialStepCard(
                step = "4",
                title = "Ambil Foto dan Simpan",
                desc = "Tekan tombol capture, lalu pilih Simpan & Lihat Hasil."
            )

            TutorialStepCard(
                step = "5",
                title = "Lihat Mutu Berdasarkan SNI",
                desc = "Aplikasi akan menampilkan mutu kopi berdasarkan nilai cacat SNI 01-2907-2008."
            )

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Mulai Scan Sekarang")
            }
        }
    }
}

@Composable
private fun TutorialStepCard(
    step: String,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Step circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFB7F23A), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
