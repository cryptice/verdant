// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSectionHeader.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkFill04
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetInkFill04)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W600,
            fontSize = 19.sp,
            color = FaltetInk,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetSectionHeaderPreview() {
    Column {
        FaltetSectionHeader(label = "Idag")
        FaltetSectionHeader(label = "Denna vecka")
        FaltetSectionHeader(label = "Senare")
    }
}
