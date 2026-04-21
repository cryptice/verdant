// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Chip.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream

@Composable
fun Chip(
    text: String,
    tone: FaltetTone = FaltetTone.Forest,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val toneColor = tone.color()
    Box(
        modifier = modifier
            .then(if (filled) Modifier.background(toneColor, CircleShape) else Modifier)
            .border(1.dp, toneColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.W400,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = if (filled) FaltetCream else toneColor,
            ),
        )
    }
}
