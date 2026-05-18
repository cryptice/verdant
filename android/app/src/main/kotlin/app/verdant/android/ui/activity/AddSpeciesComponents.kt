package app.verdant.android.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
internal fun SuggestionRow(
    suggestion: PlantSuggestion,
    onTap: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.commonName,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = FaltetInk,
            )
            Text(
                text = suggestion.species.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        }
        Text(
            text = "${(suggestion.confidence * 100).toInt()}%",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = FaltetAccent,
        )
    }
}
