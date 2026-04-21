// android/app/src/main/kotlin/app/verdant/android/ui/faltet/PhotoPlaceholder.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetBlush
import app.verdant.android.ui.theme.FaltetButter
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetSage

enum class PhotoTone { Sage, Blush, Butter }

@Composable
fun PhotoPlaceholder(
    label: String,
    tone: PhotoTone = PhotoTone.Sage,
    aspectRatio: Float = 16f / 9f,
    modifier: Modifier = Modifier,
) {
    val toneColor = when (tone) {
        PhotoTone.Sage   -> FaltetSage
        PhotoTone.Blush  -> FaltetBlush
        PhotoTone.Butter -> FaltetButter
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        toneColor.copy(alpha = 0.35f),
                        toneColor.copy(alpha = 0.12f),
                        FaltetCream,
                    ),
                ),
            )
            .border(1.dp, FaltetInk),
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}
