// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Stat.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

enum class StatSize { Large, Medium, Small }

@Composable
fun Stat(
    value: String,
    label: String,
    unit: String? = null,
    delta: String? = null,
    hue: FaltetTone = FaltetTone.Sage,
    size: StatSize = StatSize.Large,
    modifier: Modifier = Modifier,
) {
    val valueSize = when (size) { StatSize.Large -> 88.sp; StatSize.Medium -> 56.sp; StatSize.Small -> 32.sp }
    val unitSize  = when (size) { StatSize.Large -> 28.sp; StatSize.Medium -> 18.sp; StatSize.Small -> 14.sp }
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontFamily = FaltetDisplay,
                fontWeight = FontWeight.W300,
                fontSize = valueSize,
                letterSpacing = (-1.2).sp,
                color = FaltetInk,
            )
            unit?.let {
                Text(
                    text = " $it",
                    fontFamily = FaltetDisplay,
                    fontStyle = FontStyle.Italic,
                    fontSize = unitSize,
                    color = hue.color(),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(hue.color(), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
                color = FaltetForest,
            )
            delta?.let {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "▲ $it",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp,
                    color = FaltetClay,
                )
            }
        }
    }
}
