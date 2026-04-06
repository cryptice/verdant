package app.verdant.android.voice

import java.text.Normalizer

// Parsed command types
sealed class VoiceCommand {
    data class PlantActivity(
        val action: String,           // SOW, SOAK, POT_UP, PLANT, HARVEST
        val quantity: Int,
        val speciesQuery: String,     // raw species text to match
    ) : VoiceCommand()

    data class SupplyUsage(
        val quantity: Double,
        val unit: String?,            // liters, kg, etc. (raw from speech)
        val supplyQuery: String,      // raw supply name to match
    ) : VoiceCommand()

    data class Unparseable(val text: String) : VoiceCommand()
}

object VoiceCommandParser {

    // Action keyword mappings (English + Swedish)
    private val actionKeywords = mapOf(
        // English
        "sow" to "SOW", "sowed" to "SOW", "sowing" to "SOW",
        "soak" to "SOAK", "soaked" to "SOAK", "soaking" to "SOAK",
        "pot up" to "POT_UP", "potted up" to "POT_UP", "potting up" to "POT_UP",
        "plant" to "PLANT", "planted" to "PLANT", "planting" to "PLANT",
        "plant out" to "PLANT", "planted out" to "PLANT",
        "harvest" to "HARVEST", "harvested" to "HARVEST", "harvesting" to "HARVEST",
        // Swedish
        "sa" to "SOW", "sadde" to "SOW", "sar" to "SOW",
        "blotlagg" to "SOAK", "blotlade" to "SOAK", "blotlagt" to "SOAK",
        "omplantera" to "POT_UP", "omplanerade" to "POT_UP",
        "plantera" to "PLANT", "planterade" to "PLANT", "plantera ut" to "PLANT", "planterade ut" to "PLANT",
        "skorda" to "HARVEST", "skordade" to "HARVEST",
    )

    // Supply usage keywords
    private val usageKeywords = setOf("used", "use", "anvant", "anvande", "forbrukat", "forbrukade")

    // Unit keywords mapping to normalized form
    private val unitKeywords = mapOf(
        "liter" to "LITERS", "liters" to "LITERS", "l" to "LITERS",
        "kilo" to "KILOGRAMS", "kilos" to "KILOGRAMS", "kg" to "KILOGRAMS", "kilogram" to "KILOGRAMS",
        "gram" to "GRAMS", "grams" to "GRAMS", "g" to "GRAMS",
        "meter" to "METERS", "meters" to "METERS", "m" to "METERS",
        "styck" to "COUNT", "stycken" to "COUNT", "st" to "COUNT", "pieces" to "COUNT", "pcs" to "COUNT",
        "paket" to "PACKETS", "packets" to "PACKETS",
    )

    // Filler words to strip
    private val fillerWords = setOf("i", "have", "har", "jag", "of", "av", "some", "lite", "variety", "sort", "the", "a", "an", "ett", "en")

    fun parse(rawText: String): VoiceCommand {
        // Strip everything before and including "verdant"
        val normalized = normalize(rawText)
        val verdantIdx = normalized.indexOf("verdant")
        val commandText = if (verdantIdx >= 0) {
            normalized.substring(verdantIdx + "verdant".length).trim()
        } else {
            normalized.trim()
        }

        if (commandText.isBlank()) return VoiceCommand.Unparseable(rawText)

        // Try supply usage first (has distinct "used" keyword)
        tryParseSupplyUsage(commandText)?.let { return it }

        // Try plant activity
        tryParsePlantActivity(commandText)?.let { return it }

        return VoiceCommand.Unparseable(rawText)
    }

    private fun tryParsePlantActivity(text: String): VoiceCommand.PlantActivity? {
        val words = text.split("\\s+".toRegex())

        // Find action -- try multi-word first ("pot up", "plant out"), then single word
        var action: String? = null
        var actionEndIdx = 0

        for (i in words.indices) {
            if (i + 1 < words.size) {
                val twoWord = "${words[i]} ${words[i + 1]}"
                actionKeywords[twoWord]?.let {
                    action = it
                    actionEndIdx = i + 2
                }
            }
            if (action == null) {
                actionKeywords[words[i]]?.let {
                    action = it
                    actionEndIdx = i + 1
                }
            }
            if (action != null) break
        }

        if (action == null) return null

        // Find quantity -- first number after action
        val remaining = words.drop(actionEndIdx)
        var quantity: Int? = null
        var quantityIdx = -1
        for ((i, word) in remaining.withIndex()) {
            word.toIntOrNull()?.let {
                quantity = it
                quantityIdx = i
            }
            if (quantity != null) break
        }

        if (quantity == null) {
            // Try before action
            for (word in words.take(actionEndIdx)) {
                word.toIntOrNull()?.let { quantity = it }
                if (quantity != null) break
            }
            if (quantity == null) return null
            quantityIdx = -1
        }

        // Everything after quantity (or after action if quantity was before) is the species query
        val speciesWords = if (quantityIdx >= 0) {
            remaining.drop(quantityIdx + 1)
        } else {
            remaining
        }.filter { it !in fillerWords }

        val speciesQuery = speciesWords.joinToString(" ").trim()
        if (speciesQuery.isBlank()) return null

        return VoiceCommand.PlantActivity(action!!, quantity!!, speciesQuery)
    }

    private fun tryParseSupplyUsage(text: String): VoiceCommand.SupplyUsage? {
        val words = text.split("\\s+".toRegex())

        // Check for usage keyword
        val usageIdx = words.indexOfFirst { it in usageKeywords }
        if (usageIdx < 0) return null

        val remaining = words.drop(usageIdx + 1)

        // Find quantity
        var quantity: Double? = null
        var quantityIdx = -1
        for ((i, word) in remaining.withIndex()) {
            word.replace(",", ".").toDoubleOrNull()?.let {
                quantity = it
                quantityIdx = i
            }
            if (quantity != null) break
        }

        if (quantity == null) return null

        // Find unit (optional, right after quantity)
        var unit: String? = null
        var unitIdx = -1
        if (quantityIdx + 1 < remaining.size) {
            unitKeywords[remaining[quantityIdx + 1]]?.let {
                unit = it
                unitIdx = quantityIdx + 1
            }
        }

        // Everything after quantity+unit is supply name
        val startIdx = maxOf(quantityIdx, unitIdx) + 1
        val supplyWords = remaining.drop(startIdx).filter { it !in fillerWords }
        val supplyQuery = supplyWords.joinToString(" ").trim()

        if (supplyQuery.isBlank()) return null

        return VoiceCommand.SupplyUsage(quantity!!, unit, supplyQuery)
    }

    fun normalize(text: String): String {
        // Lowercase, strip diacritics
        val lower = text.lowercase()
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
    }
}
