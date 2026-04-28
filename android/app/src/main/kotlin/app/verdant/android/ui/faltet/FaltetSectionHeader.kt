// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSectionHeader.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest

@Composable
fun FaltetSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "❀",
                fontSize = 11.sp,
                color = FaltetAccent,
            )
            Text(
                text = label,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.W500,
                fontSize = 16.sp,
                color = FaltetForest,
            )
        }
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
