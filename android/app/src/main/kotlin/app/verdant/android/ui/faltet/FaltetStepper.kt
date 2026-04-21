// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetStepper.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import app.verdant.android.ui.theme.FaltetInkLine40

@Composable
fun FaltetStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
) {
    val decEnabled = value > min
    val incEnabled = value < max
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        StepperButton(
            icon = Icons.Default.Remove,
            enabled = decEnabled,
            onClick = onDecrement,
            contentDescription = "Minska",
        )
        Text(
            text = value.toString(),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = FaltetInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 24.dp),
        )
        StepperButton(
            icon = Icons.Default.Add,
            enabled = incEnabled,
            onClick = onIncrement,
            contentDescription = "Öka",
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val borderColor = if (enabled) FaltetInkLine40 else FaltetInkLine20
    val iconTint = if (enabled) FaltetInk else FaltetForest.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, borderColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetStepperPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FaltetStepper(value = 5, onDecrement = {}, onIncrement = {}, min = 0, max = 10)
        FaltetStepper(value = 0, onDecrement = {}, onIncrement = {}, min = 0, max = 10)
        FaltetStepper(value = 10, onDecrement = {}, onIncrement = {}, min = 0, max = 10)
    }
}
