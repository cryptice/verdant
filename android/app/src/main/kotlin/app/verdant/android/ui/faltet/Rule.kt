// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Rule.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

enum class RuleVariant { Ink, Soft }

@Composable
fun Rule(variant: RuleVariant = RuleVariant.Ink, modifier: Modifier = Modifier) {
    val color = when (variant) {
        RuleVariant.Ink  -> FaltetInk
        RuleVariant.Soft -> FaltetInkLine20
    }
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}
