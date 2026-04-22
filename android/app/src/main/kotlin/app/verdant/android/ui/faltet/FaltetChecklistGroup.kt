// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChecklistGroup.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun <T : Any> FaltetChecklistGroup(
    label: String,
    options: List<T>,
    selected: Set<T>,
    onSelectedChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String,
    subtitleFor: ((T) -> String?)? = null,
    selectAllEnabled: Boolean = false,
    required: Boolean = false,
) {
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
        Spacer(Modifier.height(6.dp))
        if (selectAllEnabled && options.isNotEmpty()) {
            val allSelected = selected.size == options.size
            Text(
                text = if (allSelected) "AVMARKERA ALLA" else "VÄLJ ALLA",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = FaltetClay,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectedChange(if (allSelected) emptySet() else options.toSet())
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        options.forEach { option ->
            val isSelected = option in selected
            val subtitle = subtitleFor?.invoke(option)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable {
                        onSelectedChange(
                            if (isSelected) selected - option else selected + option
                        )
                    }
                    .drawBehind {
                        drawLine(
                            color = FaltetInkLine20,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                FaltetCheckbox(checked = isSelected, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = labelFor(option),
                        fontFamily = FaltetDisplay,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        color = FaltetInk,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = FaltetForest,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChecklistGroupPreview_Partial() {
    FaltetChecklistGroup(
        label = "Plantor",
        options = listOf("Cosmos #1", "Zinnia #3", "Dahlia #5"),
        selected = setOf("Cosmos #1"),
        onSelectedChange = {},
        labelFor = { it },
        subtitleFor = { "Status: Utplanterad" },
        selectAllEnabled = true,
        required = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChecklistGroupPreview_AllSelected() {
    FaltetChecklistGroup(
        label = "Plantor",
        options = listOf("Cosmos #1", "Zinnia #3"),
        selected = setOf("Cosmos #1", "Zinnia #3"),
        onSelectedChange = {},
        labelFor = { it },
        selectAllEnabled = true,
    )
}
