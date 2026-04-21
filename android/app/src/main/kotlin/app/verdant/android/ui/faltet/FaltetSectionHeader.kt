// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSectionHeader.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
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
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest,
        )
        Spacer(Modifier.height(4.dp))
        Spacer(
            Modifier
                .width(24.dp)
                .height(1.5.dp)
                .background(FaltetClay),
        )
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
