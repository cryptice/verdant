package app.verdant.android.ui.activity

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FrequentCommentsField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    label: String = "Notes (optional)",
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = {
                            val newValue = if (value.isBlank()) suggestion
                            else "$value\n$suggestion"
                            onValueChange(newValue)
                        },
                        label = { Text(suggestion, maxLines = 1) }
                    )
                }
            }
        }
    }
}
