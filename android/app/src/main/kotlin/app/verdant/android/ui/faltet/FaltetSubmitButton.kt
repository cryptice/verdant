// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSubmitButton.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun FaltetSubmitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitting: Boolean = false,
) {
    Button(
        onClick = { if (!submitting) onClick() },
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RectangleShape,
        enabled = enabled && !submitting,
        colors = ButtonDefaults.buttonColors(
            containerColor = FaltetInk,
            contentColor = FaltetCream,
            disabledContainerColor = FaltetInk.copy(alpha = 0.4f),
            disabledContentColor = FaltetCream,
        ),
    ) {
        if (submitting) {
            CircularProgressIndicator(
                color = FaltetCream,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = label.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetSubmitButtonPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FaltetSubmitButton(label = "Skapa", onClick = {})
        Spacer(Modifier.height(4.dp))
        FaltetSubmitButton(label = "Skapa", onClick = {}, enabled = false)
        Spacer(Modifier.height(4.dp))
        FaltetSubmitButton(label = "Skapa", onClick = {}, submitting = true)
    }
}
