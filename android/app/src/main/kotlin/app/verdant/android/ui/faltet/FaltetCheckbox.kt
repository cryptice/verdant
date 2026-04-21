// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetCheckbox.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetInkLine40

@Composable
fun FaltetCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = FaltetClay,
            uncheckedColor = FaltetInkLine40,
            checkmarkColor = FaltetCream,
        ),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetCheckboxPreview() {
    Row {
        FaltetCheckbox(checked = false, onCheckedChange = {})
        Spacer(Modifier.width(8.dp))
        FaltetCheckbox(checked = true, onCheckedChange = {})
    }
}
