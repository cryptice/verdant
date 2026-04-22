// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetHero.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetHero(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable BoxScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(FaltetCream)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        if (leading != null) {
            Box(modifier = Modifier.size(140.dp)) { leading() }
            Spacer(Modifier.width(20.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
                color = FaltetInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = FaltetForest,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetHeroPreview_WithPhotoPlaceholder() {
    FaltetHero(
        title = "Vildblommor",
        subtitle = "Lerig mylla · Sydvänd",
        leading = {
            PhotoPlaceholder(
                label = "Vildblommor",
                tone = PhotoTone.Sage,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetHeroPreview_WithEmoji() {
    FaltetHero(
        title = "Villan",
        subtitle = "Hemträdgården",
        leading = {
            Text(
                text = "🌱",
                fontSize = 64.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetHeroPreview_NoLeading() {
    FaltetHero(title = "Cosmos bipinnatus", subtitle = "84 plantor")
}
