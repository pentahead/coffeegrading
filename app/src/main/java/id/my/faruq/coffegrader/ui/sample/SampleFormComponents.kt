package id.my.faruq.coffegrader.ui.sample

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val SampleAccentColor = Color(0xFF12A150)
private val SampleFieldShape = RoundedCornerShape(14.dp)
private val SampleFieldBorderWidth = 2.dp
private val SampleFieldBorderColor = Color(0xFF757575)
private val SamplePlaceholderColor = Color(0xFF9CA3AF)

@Composable
private fun sampleTransparentOutlineColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent
    )

@Composable
private fun SampleFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SampleFieldError(text: String?) {
    if (text != null) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

@Composable
fun SampleLabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = sampleFieldBorderColor(isError = isError, isActive = isFocused)

    Column(modifier = modifier.fillMaxWidth()) {
        SampleFieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(SampleFieldBorderWidth, borderColor, SampleFieldShape)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = SamplePlaceholderColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                interactionSource = interactionSource,
                isError = isError,
                shape = SampleFieldShape,
                keyboardOptions = keyboardOptions,
                colors = sampleTransparentOutlineColors()
            )
        }
        SampleFieldError(errorText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor = sampleFieldBorderColor(isError = isError, isActive = expanded)

    Column(modifier = modifier.fillMaxWidth()) {
        SampleFieldLabel(label)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(SampleFieldBorderWidth, borderColor, SampleFieldShape)
            ) {
                OutlinedTextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = SampleFieldShape,
                    colors = sampleTransparentOutlineColors()
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        SampleFieldError(errorText)
    }
}

private fun sampleFieldBorderColor(isError: Boolean, isActive: Boolean): Color =
    when {
        isError -> Color(0xFFB3261E)
        isActive -> SampleAccentColor
        else -> SampleFieldBorderColor.copy(alpha = 0.55f)
    }
