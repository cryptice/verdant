// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Masthead.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun Masthead(
    left: String,
    center: String,
    right: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            .drawBehind {
                drawLine(
                    color = FaltetInk,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text = left.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            color = FaltetForest,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = center,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            color = FaltetForest,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(2f),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            right?.invoke()
        }
    }
}
