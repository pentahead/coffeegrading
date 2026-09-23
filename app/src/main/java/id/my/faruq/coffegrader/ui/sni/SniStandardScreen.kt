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
                title = { Text("Standard Document", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                text = "INDONESIAN NATIONAL STANDARD",
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
                text = "Coffee Beans",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Batang Tubuh Dokumen
            SniArticleSection(
                number = "1.",
                title = "Scope",
                content = "This standard specifies the terms and definitions, classification, quality requirements, sampling, test methods, and marking and packaging requirements for coffee beans."
            )

            SniArticleSection(
                number = "2.",
                title = "General Quality Requirements",
                content = "For all grades, coffee beans must meet the following criteria:\n\n" +
                        "• Live insects: Not allowed.\n" +
                        "• Beans with rotten or moldy smell: None.\n" +
                        "• Moisture content: Maximum 12.5% mass fraction.\n" +
                        "• Impurity content: Maximum 0.5% mass fraction."
            )

            SniArticleSection(
                number = "3.",
                title = "Grade Classification (Defect Value System)",
                content = "Grades are determined by the total defect value in a 300 gram sample.\n\n" +
                        "Robusta Coffee:\n" +
                        "Grade 1: Defect value maximum 11\n" +
                        "Grade 2: Defect value 12 - 25\n" +
                        "Grade 3: Defect value 26 - 44\n" +
                        "Grade 4a: Defect value 45 - 60\n" +
                        "Grade 4b: Defect value 61 - 80\n" +
                        "Grade 5: Defect value 81 - 150\n" +
                        "Grade 6: Defect value 151 - 225\n\n" +
                        "Arabica Coffee: same value ranges, but Grade 4 is not split into 4a/4b " +
                        "(a single Grade 4 class for defect values 45 - 80)."
            )

            SniArticleSection(
                number = "4.",
                title = "Defect Value Provisions",
                content = "Defect values are calculated based on the following criteria:\n\n" +
                        "• 1 black bean: 1 defect value\n" +
                        "• 1 broken bean: 0.2 defect value\n" +
                        "• 1 bean with one hole: 0.1 defect value\n" +
                        "• 1 bean with more than one hole: 0.2 defect value\n" +
                        "• 1 large twig/soil/stone: 5 defect value"
            )

            SniArticleSection(
                number = "5.",
                title = "Test Methods",
                content = "Moisture content is determined using a moisture meter tester or the oven method. Defect values are determined visually by separating defective beans from a 300 gram sample and then calculating their total value."
            )

            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "© National Standardization Agency of Indonesia (BSN)",
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