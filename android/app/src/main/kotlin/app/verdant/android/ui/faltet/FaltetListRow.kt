// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetListRow.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetListRow(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    meta: String? = null,
    metaMaxLines: Int = 1,
    stat: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(clickable)
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
        if (leading != null) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) { leading() }
            Spacer(Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
                color = FaltetInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Text(
                    text = meta.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = FaltetForest,
                    maxLines = metaMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (stat != null || actions != null) {
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                stat?.invoke()
                actions?.invoke()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetListRowPreview_Minimal() {
    FaltetListRow(title = "Cosmos bipinnatus", meta = "Cosmos · Batch 12")
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetListRowPreview_Full() {
    FaltetListRow(
        title = "Cosmos bipinnatus",
        meta = "Cosmos · Batch 12 · Sådd vecka 14",
        leading = {
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(FaltetClay)
                    },
            )
        },
        stat = {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("84", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = FaltetInk)
                Text(" STK", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.2.sp, color = FaltetForest)
            }
        },
        onClick = {},
    )
}
