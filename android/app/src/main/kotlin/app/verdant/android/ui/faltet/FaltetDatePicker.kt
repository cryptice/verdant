// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDatePicker.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltetDatePicker(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Välj datum",
    required: Boolean = false,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )

    Column(modifier) {
        Text(
            text = buildAnnotatedString {
                append(label.uppercase())
                if (required) {
                    withStyle(SpanStyle(color = FaltetClay)) { append(" *") }
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dialogOpen = true }
                .drawBehind {
                    drawLine(
                        color = FaltetInk,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = value?.let { formatDateSv(it) } ?: placeholder,
                fontFamily = FaltetDisplay,
                fontStyle = if (value == null) FontStyle.Normal else FontStyle.Italic,
                fontWeight = FontWeight.W300,
                fontSize = 20.sp,
                color = if (value == null) FaltetForest.copy(alpha = 0.4f) else FaltetInk,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = FaltetClay,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (dialogOpen) {
        DatePickerDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(picked)
                    }
                    dialogOpen = false
                }) { Text("Välj", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Avbryt") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun formatDateSv(date: LocalDate): String {
    val months = arrayOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec")
    val m = months[date.monthValue - 1]
    val currentYear = Year.now().value
    return if (date.year == currentYear) "${date.dayOfMonth} $m" else "${date.dayOfMonth} $m ${date.year}"
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDatePickerPreview_Empty() {
    FaltetDatePicker(label = "Deadline", value = null, onValueChange = {}, required = true)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDatePickerPreview_Populated() {
    FaltetDatePicker(
        label = "Deadline",
        value = LocalDate.of(2026, 5, 14),
        onValueChange = {},
    )
}
