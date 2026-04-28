package app.verdant.android.ui.faltet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.verdant.android.R

/**
 * Renders a hand-coloured 17th-century botanical illustration (e.g. plates
 * from Basilius Besler's *Hortus Eystettensis*, 1613, in its hand-coloured
 * edition). Image colours pass through unchanged so the muted watercolour
 * palette of the period engravings sits naturally on the cream page.
 *
 * Drop public-domain PNG/WebP scans into `app/src/main/res/drawable/` and
 * reference them via [BotanicalPlate].
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
            modifier = Modifier.size(size),
        )
    }
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
