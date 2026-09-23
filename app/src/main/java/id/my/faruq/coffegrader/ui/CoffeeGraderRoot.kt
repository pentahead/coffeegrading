package id.my.faruq.coffegrader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import id.my.faruq.coffegrader.ui.navigation.AppNavHost

@Composable
fun CoffeeGraderRoot() {
    MaterialTheme {
        Surface {
            AppNavHost()
        }
    }
}