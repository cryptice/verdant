// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipSelector.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInkLine40

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> FaltetChipSelector(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T?) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
) {
    Column(modifier) {
        Text(
            text = buildAnnotatedString {
                append(label.uppercase())
                if (required) {
                    withStyle(SpanStyle(color = FaltetAccent)) { append(" *") }
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectedChange(if (isSelected) null else option) },
                    label = { Text(labelFor(option), fontSize = 12.sp) },
                    shape = RectangleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = FaltetCream,
                        labelColor = FaltetForest,
                        selectedContainerColor = FaltetAccent,
                        selectedLabelColor = FaltetCream,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = FaltetInkLine40,
                        selectedBorderColor = FaltetAccent,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 0.dp,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChipSelectorPreview_Unselected() {
    FaltetChipSelector(
        label = "Jordtyp",
        options = listOf("Lera", "Sand", "Mylla"),
        selected = null,
        onSelectedChange = {},
        labelFor = { it },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChipSelectorPreview_Selected() {
    FaltetChipSelector(
        label = "Jordtyp",
        options = listOf("Lera", "Sand", "Mylla"),
        selected = "Mylla",
        onSelectedChange = {},
        labelFor = { it },
        required = true,
    )
}
