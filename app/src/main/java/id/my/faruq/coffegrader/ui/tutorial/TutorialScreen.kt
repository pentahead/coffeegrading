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
                text = "CoffeeGrader User Guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Follow these steps so your coffee bean scan results meet the SNI standard.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(18.dp))

            TutorialStepCard(
                step = "1",
                title = "Prepare the Coffee Sample",
                desc = "Use ±300 grams of coffee beans and make sure the beans are not stacked."
            )

            TutorialStepCard(
                step = "2",
                title = "Open the Scan Menu",
                desc = "Tap the SCAN button on the home page to open the camera."
            )

            TutorialStepCard(
                step = "3",
                title = "Position the Beans Inside the Grid",
                desc = "Place the coffee beans inside the white box area for more accurate detection."
            )

            TutorialStepCard(
                step = "4",
                title = "Take a Photo and Save",
                desc = "Tap the capture button, then select Save & View Results."
            )

            TutorialStepCard(
                step = "5",
                title = "View the Grade Based on SNI",
                desc = "The app will display the coffee grade based on SNI 01-2907-2008 defect values."
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
                Text("Start Scanning Now")
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
