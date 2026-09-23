package id.my.faruq.coffegrader.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.faruq.coffegrader.R 
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About App", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Bagian Header Logo ---
            // Ganti R.drawable.ic_launcher_foreground dengan logo aplikasi Anda
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                    Icons.Default.Info, 
                    contentDescription = null, 
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "CoffeeGrader",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Deskripsi ---
            AboutSection(title = "Description") {
                Text(
                    text = "CoffeeGrader is an image-processing-based assistant app for grading the quality of robusta green coffee beans. It helps with scanning, defect segmentation, and quality control documentation.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify
                )
            }

            // --- Palet Warna SNI ---
            ColorPaletteSection()
            
            // --- Fitur Utama ---
            AboutSection(title = "Key Features") {
                BulletPoint("Fast & accurate coffee bean analysis")
                BulletPoint("Defect segmentation (Robusta)")
                BulletPoint("Integrated scan history")
                BulletPoint("Automated standard-compliant grade scoring")
            }

            // --- Info Teknis & Pengembang ---
            AboutSection(title = "Developer Information") {
                InfoRow(Icons.Default.Person, "Dhiyaul Faruq", "University of Jember")
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(Icons.Default.Email, "Contact", "faruq.xtkj1.simdig@gmail.com")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Note: Results are assistive. The final decision remains with the standard QC procedure.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AboutSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("• ", fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, 
            contentDescription = null, 
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
fun ColorPaletteSection() {
    AboutSection(title = "Grade Indicators (SNI 01-2907-2008)") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorRow(Color(0xFF16A34A), "Grade 1", "Very Good (Export Quality)")
            ColorRow(Color(0xFF65A30D), "Grade 2", "Good")
            ColorRow(Color(0xFFB45309), "Grade 3", "Fair")
            ColorRow(Color(0xFFEA580C), "Grade 4a", "Moderate")
            ColorRow(Color(0xFFDC2626), "Grade 4b", "Low")
            ColorRow(Color(0xFF991B1B), "Grade 5", "Very Low")
            ColorRow(Color(0xFF450A0A), "Grade 6", "Sub-standard / Reject")
        }
    }
}

@Composable
fun ColorRow(color: Color, label: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(24.dp).clip(CircleShape),
            color = color
        ) {}
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.labelSmall)
        }
    }
}