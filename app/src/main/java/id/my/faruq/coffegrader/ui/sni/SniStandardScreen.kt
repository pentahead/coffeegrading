package id.my.faruq.coffegrader.ui.sni

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SniStandardScreen(
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dokumen Standar", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header Dokumen
            Text(
                text = "STANDAR NASIONAL INDONESIA",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "SNI 01-2907-2008",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Biji Kopi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Batang Tubuh Dokumen
            SniArticleSection(
                number = "1.",
                title = "Ruang Lingkup",
                content = "Standar ini menetapkan istilah dan definisi, klasifikasi, syarat mutu, pengambilan contoh, cara uji, syarat penandaan dan pengemasan untuk biji kopi."
            )

            SniArticleSection(
                number = "2.",
                title = "Syarat Mutu Umum",
                content = "Untuk semua jenis mutu, biji kopi harus memenuhi kriteria berikut:\n\n" +
                        "• Serangga hidup: Tidak diperbolehkan.\n" +
                        "• Biji berbau busuk atau kapang: Tidak ada.\n" +
                        "• Kadar air: Maksimal 12,5% fraksi massa.\n" +
                        "• Kadar kotoran: Maksimal 0,5% fraksi massa."
            )

            SniArticleSection(
                number = "3.",
                title = "Klasifikasi Mutu (Sistem Nilai Cacat)",
                content = "Penentuan besaran mutu didasarkan pada jumlah nilai cacat dalam 300 gram sampel.\n\n" +
                        "Kopi Robusta:\n" +
                        "Mutu 1: Nilai cacat maksimal 11\n" +
                        "Mutu 2: Nilai cacat 12 - 25\n" +
                        "Mutu 3: Nilai cacat 26 - 44\n" +
                        "Mutu 4a: Nilai cacat 45 - 60\n" +
                        "Mutu 4b: Nilai cacat 61 - 80\n" +
                        "Mutu 5: Nilai cacat 81 - 150\n" +
                        "Mutu 6: Nilai cacat 151 - 225\n\n" +
                        "Kopi Arabika: rentang nilai sama, tetapi Mutu 4 tidak dibagi 4a/4b " +
                        "(satu kelas Mutu 4 untuk nilai cacat 45 - 80)."
            )

            SniArticleSection(
                number = "4.",
                title = "Ketentuan Nilai Cacat",
                content = "Penghitungan nilai cacat dilakukan berdasarkan kriteria:\n\n" +
                        "• 1 biji hitam: 1 nilai cacat\n" +
                        "• 1 biji pecah: 0,2 nilai cacat\n" +
                        "• 1 biji berlubang (satu): 0,1 nilai cacat\n" +
                        "• 1 biji berlubang (lebih dari satu): 0,2 nilai cacat\n" +
                        "• 1 ranting/tanah/batu ukuran besar: 5 nilai cacat"
            )

            SniArticleSection(
                number = "5.",
                title = "Metode Pengujian",
                content = "Penentuan kadar air dilakukan dengan moisture meter tester atau metode oven. Penentuan nilai cacat dilakukan secara visual dengan memisahkan biji cacat dari sampel 300 gram kemudian dihitung total nilainya."
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "© Badan Standardisasi Nasional (BSN)",
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SniArticleSection(
    number: String,
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row {
            Text(
                text = number,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(start = 28.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}