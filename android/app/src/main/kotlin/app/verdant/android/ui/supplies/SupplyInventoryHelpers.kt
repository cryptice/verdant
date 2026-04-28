package app.verdant.android.ui.supplies

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector
import app.verdant.android.data.model.SupplyInventoryResponse

internal val CATEGORY_ORDER = listOf("SOIL", "POT", "FERTILIZER", "TRAY", "LABEL", "OTHER")
internal val SUPPLY_CATEGORIES = listOf("SOIL", "POT", "FERTILIZER", "TRAY", "LABEL", "OTHER")
internal val SUPPLY_UNITS = listOf("COUNT", "LITERS", "KILOGRAMS", "GRAMS", "METERS", "PACKETS")

internal fun categoryLabelSv(category: String): String = when (category) {
    "SOIL" -> "Jord"
    "POT" -> "Krukor"
    "FERTILIZER" -> "Gödsel"
    "TRAY" -> "Brätten"
    "LABEL" -> "Etiketter"
    "OTHER" -> "Övrigt"
    else -> category
}

internal fun unitLabelSv(unit: String): String = when (unit) {
    "COUNT" -> "st"
    "LITERS" -> "L"
    "KILOGRAMS" -> "kg"
    "GRAMS" -> "g"
    "METERS" -> "m"
    "PACKETS" -> "påsar"
    else -> unit
}

/**
 * The most natural unit for a freshly-picked category. Trays, pots, and
 * labels are counted; soil and fertilizer are measured by volume by
 * default. Everything else defaults to litres so users can still measure
 * homemade tinctures or compost teas without changing the unit.
 */
internal fun defaultUnitFor(category: String): String = when (category) {
    "TRAY", "POT", "LABEL" -> "COUNT"
    else -> "LITERS"
}

internal fun categoryIcon(category: String): ImageVector = when (category) {
    "SOIL" -> Icons.Default.Grass
    "POT" -> Icons.Default.Inventory2
    "FERTILIZER" -> Icons.Default.Science
    "TRAY" -> Icons.Default.Inventory2
    "LABEL" -> Icons.AutoMirrored.Filled.Label
    "OTHER" -> Icons.Default.Category
    else -> Icons.Default.Category
}

internal fun formatQuantity(quantity: Double, unit: String): String {
    val formatted = if (quantity == quantity.toLong().toDouble()) {
        quantity.toLong().toString()
    } else {
        String.format("%.1f", quantity)
    }
    return "$formatted $unit"
}

internal data class SupplyTypeGroup(
    val supplyTypeId: Long,
    val name: String,
    val unit: String,
    val totalQuantity: Double,
    val batches: List<SupplyInventoryResponse>,
    val inexhaustible: Boolean = false,
)
