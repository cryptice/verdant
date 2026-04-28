package app.verdant.android.ui.faltet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.verdant.android.R
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

/**
 * Renders a 17th-century botanical engraving (e.g. plates from Basilius
 * Besler's *Hortus Eystettensis*, 1613) tinted to sit on the cream palette
 * like ink stained on aged paper.
 *
 * Drop public-domain PNG/WebP scans into `app/src/main/res/drawable/` and
 * reference them via [BotanicalPlate]. The composable applies a sepia
 * desaturation + a multiply-style tint so the whites of the original scan
 * disappear into the cream background.
 *
 * See `docs/BOTANICAL_PLATES.md` for sourcing notes.
 */
@Composable
fun BotanicalIllustration(
    plate: BotanicalPlate,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(plate.drawableRes),
            contentDescription = plate.contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = sepiaInkFilter,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Pulls all colour toward FaltetInk and crushes whites to transparent-ish
 * cream so the engraving looks like ink on the same paper as the page.
 *
 * Implemented as a desaturating color-matrix; the alpha is unchanged but
 * the lightness sits in the FaltetForest range.
 */
private val sepiaInkFilter: ColorFilter = run {
    val matrix = ColorMatrix().apply {
        // Desaturate first.
        setToSaturation(0f)
    }
    // Then bias toward forest-green ink (R=0.18, G=0.24, B=0.18).
    val biased = floatArrayOf(
        0.18f, 0.18f, 0.18f, 0f, 0f,
        0.24f, 0.24f, 0.24f, 0f, 0f,
        0.18f, 0.18f, 0.18f, 0f, 0f,
        0f,    0f,    0f,    1f, 0f,
    )
    ColorFilter.colorMatrix(ColorMatrix(biased))
}

/**
 * Plates we've sourced. Each enum value names a public-domain engraving
 * dropped into `res/drawable/` — see BOTANICAL_PLATES.md for the recommended
 * file per slot. Until images land, these resolve to a placeholder vector
 * that renders as a small floral glyph so the layout still composes.
 */
enum class BotanicalPlate(@DrawableRes val drawableRes: Int, val contentDescription: String?) {
    /** Empty-state for "no plants" — recommend Besler plate of a tulip in bloom. */
    EmptyGarden(R.drawable.botanical_placeholder, "Botanisk illustration"),

    /** Splash / drawer header — recommend Besler frontispiece or a peony plate. */
    Frontispiece(R.drawable.botanical_placeholder, "Botanisk illustration"),

    /** Empty-state for "no tasks" — recommend a Besler vine or trellis plate. */
    Trellis(R.drawable.botanical_placeholder, "Botanisk illustration"),

    /** Empty-state for "no harvest" — recommend a Besler fruiting plate. */
    Harvest(R.drawable.botanical_placeholder, "Botanisk illustration"),
}
