// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetEmptyState.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun FaltetEmptyState(
    headline: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FaltetCream)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = FaltetInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = FaltetForest,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetEmptyStatePreview_NoAction() {
    FaltetEmptyState(
        headline = "Inga frön ännu",
        subtitle = "Börja med att lägga till ditt första frö.",
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetEmptyStatePreview_WithAction() {
    FaltetEmptyState(
        headline = "Inga uppgifter",
        subtitle = "Skapa din första uppgift för säsongen.",
        action = { Text("Lägg till") },
    )
}
