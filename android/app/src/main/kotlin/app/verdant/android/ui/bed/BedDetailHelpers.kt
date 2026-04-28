package app.verdant.android.ui.bed

internal fun statusLabelSv(status: String?): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Omskolad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    null -> "—"
    else -> status
}

internal fun bedEventLabelSv(type: String): String = when (type) {
    "WATERED" -> "Vattnade"
    "WEEDED" -> "Rensade ogräs"
    "APPLIED_SUPPLY" -> "Applicerade material"
    "NOTE" -> "Anteckning"
    else -> type.lowercase().replaceFirstChar { it.uppercase() }
}

internal fun formattedDate(date: String?): String {
    if (date == null) return "—"
    return try {
        val parsed = java.time.LocalDate.parse(date)
        "${parsed.dayOfMonth} ${monthShortSv(parsed.monthValue)}"
    } catch (e: Exception) {
        date
    }
}

internal fun monthShortSv(month: Int): String = arrayOf(
    "jan", "feb", "mar", "apr", "maj", "jun",
    "jul", "aug", "sep", "okt", "nov", "dec",
)[month - 1]
