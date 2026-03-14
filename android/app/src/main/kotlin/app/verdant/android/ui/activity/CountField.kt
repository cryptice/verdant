package app.verdant.android.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Count",
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        FilledIconButton(
            onClick = {
                val current = value.toIntOrNull() ?: 0
                if (current > 0) onValueChange((current - 1).toString())
            }
        ) {
            Icon(Icons.Default.Remove, "Decrease")
        }
        FilledIconButton(
            onClick = {
                val current = value.toIntOrNull() ?: 0
                onValueChange((current + 1).toString())
            }
        ) {
            Icon(Icons.Default.Add, "Increase")
        }
    }
}
