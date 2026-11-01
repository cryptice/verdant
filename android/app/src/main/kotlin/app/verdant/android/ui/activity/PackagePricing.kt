package app.verdant.android.ui.activity

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Price per seed and per package, linked through the package size like the web form: entering one
 * price fills in the other. Prices are typed in kronor and may use a decimal comma.
 */
internal fun parseSek(value: String): BigDecimal? = value.trim().replace(',', '.').toBigDecimalOrNull()

internal fun formatSek(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** The package price for a price per seed, or blank without a package size. */
internal fun packagePriceFor(perSeed: String, seedsPerPackage: Int): String =
    parseSek(perSeed)?.takeIf { seedsPerPackage > 0 }?.let { formatSek(it * BigDecimal(seedsPerPackage)) } ?: ""

/** The price per seed for a package price, or null without a package size or a price. */
internal fun perSeedPriceFor(packagePrice: String, seedsPerPackage: Int): String? =
    parseSek(packagePrice)?.takeIf { seedsPerPackage > 0 }?.let { formatSek(it.divide(BigDecimal(seedsPerPackage), 4, RoundingMode.HALF_UP)) }

internal fun sekToCents(value: String): Int? = parseSek(value)?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toInt()

internal fun centsToSek(cents: Int?): String = cents?.let { formatSek(BigDecimal(it).movePointLeft(2)) } ?: ""
