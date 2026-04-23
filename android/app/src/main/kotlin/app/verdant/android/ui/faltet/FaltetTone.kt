// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetTone.kt
package app.verdant.android.ui.faltet

import androidx.compose.ui.graphics.Color
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky

enum class FaltetTone { Clay, Mustard, Berry, Sky, Sage, Forest }

fun FaltetTone.color(): Color = when (this) {
    FaltetTone.Clay    -> FaltetAccent
    FaltetTone.Mustard -> FaltetMustard
    FaltetTone.Berry   -> FaltetBerry
    FaltetTone.Sky     -> FaltetSky
    FaltetTone.Sage    -> FaltetSage
    FaltetTone.Forest  -> FaltetForest
}
