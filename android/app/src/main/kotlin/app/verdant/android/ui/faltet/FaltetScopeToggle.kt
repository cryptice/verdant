// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScopeToggle.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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

@Composable
fun <T : Any> FaltetScopeToggle(
    label: String,
    options: List<T>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
) {
    require(options.size == 2) { "FaltetScopeToggle requires exactly 2 options" }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(1.dp, FaltetInkLine40),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (index == 1) Modifier.drawBehind {
                                drawLine(
                                    color = FaltetInkLine40,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            } else Modifier,
                        )
                        .background(if (isSelected) FaltetAccent else FaltetCream)
                        .clickable(enabled = !isSelected) { onSelectedChange(option) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelFor(option).uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = if (isSelected) FaltetCream else FaltetForest,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetScopeTogglePreview_SelectedLeft() {
    FaltetScopeToggle(
        label = "Omfattning",
        options = listOf("Hela bädden", "Enskilda plantor"),
        selected = "Hela bädden",
        onSelectedChange = {},
        labelFor = { it },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetScopeTogglePreview_SelectedRight() {
    FaltetScopeToggle(
        label = "Omfattning",
        options = listOf("Hela bädden", "Enskilda plantor"),
        selected = "Enskilda plantor",
        onSelectedChange = {},
        labelFor = { it },
        required = true,
    )
}
