package app.verdant.android.ui.plants

import androidx.compose.ui.graphics.Color
import app.verdant.android.ui.faltet.PhotoTone
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInkLine40
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky

internal fun eventLabelSv(eventType: String): String = when (eventType) {
    "SEEDED" -> "Sådda"
    "POTTED_UP" -> "Omskolade"
    "PLANTED_OUT" -> "Utplanterade"
    "HARVESTED" -> "Skördade"
    "RECOVERED" -> "Återhämtade"
    "REMOVED" -> "Borttagna"
    "NOTE" -> "Notering"
    "BUDDING" -> "Knoppar"
    "FIRST_BLOOM" -> "Första blomman"
    "PEAK_BLOOM" -> "Toppblomning"
    "LAST_BLOOM" -> "Sista blomman"
    "LIFTED" -> "Uppgrävda"
    "DIVIDED" -> "Delade"
    "STORED" -> "Lagrade"
    "PINCHED" -> "Toppade"
    "DISBUDDED" -> "Knopprensade"
    "APPLIED_SUPPLY" -> "Gödslade"
    "WATERED" -> "Vattnade"
    "MOVED" -> "Flyttade"
    "WEEDED" -> "Rensade ogräs"
    else -> eventType
}

internal fun statusLabelPluralSv(status: String): String = when (status) {
    "SEEDED" -> "Sådda"
    "POTTED_UP" -> "Omskolade"
    "PLANTED_OUT", "GROWING" -> "Utplanterade"
    "HARVESTED" -> "Skördade"
    "RECOVERED" -> "Återhämtade"
    "REMOVED" -> "Borttagna"
    else -> status
}

internal fun statusLabelSv(status: String): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Omskolad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    else -> status
}

internal fun statusColor(status: String?): Color = when (status) {
    "SEEDED" -> FaltetMustard
    "POTTED_UP" -> FaltetSky
    "PLANTED_OUT", "GROWING" -> FaltetSage
    "HARVESTED" -> FaltetAccent
    "RECOVERED" -> FaltetBerry
    "REMOVED" -> FaltetInkLine40
    else -> FaltetForest
}

internal fun speciesTone(categoryName: String?): PhotoTone {
    val n = categoryName?.lowercase() ?: ""
    return when {
        n.contains("grönsak") -> PhotoTone.Sage
        n.contains("snittblom") || n.contains("blom") -> PhotoTone.Blush
        n.contains("ört") -> PhotoTone.Butter
        n.contains("frukt") -> PhotoTone.Sage
        else -> PhotoTone.Sage
    }
}
