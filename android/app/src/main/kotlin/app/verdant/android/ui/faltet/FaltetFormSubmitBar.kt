// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFormSubmitBar.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetFormSubmitBar(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitting: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        FaltetSubmitButton(
            label = label,
            onClick = onClick,
            enabled = enabled,
            submitting = submitting,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetFormSubmitBarPreview() {
    FaltetFormSubmitBar(label = "Skapa", onClick = {})
}
