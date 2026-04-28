package app.verdant.android.ui.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditEventDateDialog(
    initialDate: java.time.LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val picked = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                    onConfirm(picked)
                } else {
                    onDismiss()
                }
            }) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
internal fun EventChooserDialog(
    eventLabel: String,
    date: String,
    count: Int,
    onDismiss: () -> Unit,
    onEditDate: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$eventLabel · $date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$count plantor",
                    fontSize = 12.sp,
                    color = FaltetForest,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onEditDate) {
                    Text("Ändra datum", color = FaltetAccent)
                }
                TextButton(onClick = onDelete) {
                    Text("Ta bort händelse…", color = FaltetAccent)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

@Composable
internal fun DeleteEventDialog(
    eventLabel: String,
    date: String,
    maxCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (count: Int) -> Unit,
) {
    var countText by remember { mutableStateOf(maxCount.toString()) }
    val parsed = countText.toIntOrNull()
    val valid = parsed != null && parsed in 1..maxCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ta bort händelse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "$eventLabel · $date",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = FaltetForest,
                )
                Text(
                    text = "Antal plantor (max $maxCount):",
                    fontSize = 13.sp,
                    color = FaltetInk,
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    text = "Plantor som blir kvar utan händelser markeras som borttagna.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = FaltetForest,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onConfirm(parsed!!) },
                enabled = valid,
            ) { Text("Ta bort", color = if (valid) FaltetAccent else FaltetForest) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
