package id.my.faruq.coffegrader.ui.about


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar


@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tentang Aplikasi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
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
            Text("Tentang CoffeeGrader\n" +
                    "CoffeeGrader adalah aplikasi bantu penilaian mutu green beans kopi robusta berbasis pengolahan citra. Aplikasi ini membantu pengguna melakukan pemindaian, menampilkan hasil deteksi/segmentasi cacat, serta menyimpan riwayat pemeriksaan untuk dokumentasi quality control.\n" +
                    "\n" +
                    "Fitur Utama\n" +
                    "\n" +
                    "Pemindaian dan analisis biji kopi secara cepat\n" +
                    "\n" +
                    "Ringkasan hasil: jumlah/proporsi cacat dan skor mutu (jika diaktifkan)\n" +
                    "\n" +
                    "Riwayat scan: detail hasil per ID pemeriksaan\n" +
                    "\n" +
                    "Berjalan secara offline pada perangkat yang didukung (tergantung konfigurasi)\n" +
                    "\n" +
                    "Catatan Penting\n" +
                    "Hasil dari aplikasi bersifat assistive (pendukung). Keputusan akhir penilaian mutu tetap mengikuti prosedur QC dan standar yang berlaku.\n" +
                    "\n" +
                    "Informasi Aplikasi\n" +
                    "\n" +
                    "Nama: CoffeeGrader\n" +
                    "\n" +
                    "Platform: Android\n" +
                    "\n" +
                    "Minimum: Android 10 (API 29)\n" +
                    "\n" +
                    "Pengembang: (Isi nama/kampusmu)\n" +
                    "\n" +
                    "Kontak: (Isi email/WA)")
        }
    }
}