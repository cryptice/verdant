// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSectionHeader.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkFill04
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(FaltetInkFill04)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(start = 18.dp, end = 6.dp),
    ) {
        Text(
            text = label,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W600,
            fontSize = 19.sp,
            color = FaltetInk,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            // Suppress the 48dp interactive-size padding around the trailing
            // slot so a TextButton (or similar) can't force the header taller
            // than the text-only baseline.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp),
                ) { trailing() }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetSectionHeaderPreview() {
    androidx.compose.foundation.layout.Column {
        FaltetSectionHeader(label = "Idag")
        FaltetSectionHeader(label = "Denna vecka")
        FaltetSectionHeader(
            label = "Bäddar",
            trailing = {
                TextButton(onClick = {}) {
                    Text("+ Lägg till", color = FaltetAccent, fontSize = 12.sp)
                }
            },
        )
    }
}
