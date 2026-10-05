package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Species
import app.verdant.entity.UnitType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal
import java.text.Normalizer

data class SuggestedProductionProfile(val profile: ProductionProfile, val info: DefaultScheduleInfo)

/** Versioned horticultural defaults. Organization profiles always take precedence. */
@ApplicationScoped
class PlanningDefaults(private val mapper: ObjectMapper) {
    private val catalogue: JsonNode = javaClass.getResourceAsStream("/planning/defaults.json")!!.use(mapper::readTree)
    private val schedules = catalogue["schedules"].toList()

    fun suggest(species: Species): SuggestedProductionProfile {
        val genus = genus(species)
        val botanical = normalize(species.scientificName.orEmpty())
        val schedule = schedules.firstOrNull { rule ->
            rule["startingUnits"].any { it.asText() == species.defaultUnitType.name } &&
                (rule["plantTypes"].isEmpty || rule["plantTypes"].any { it.asText() == species.plantType.name }) &&
                (rule["genera"].isEmpty || rule["genera"].any { normalize(it.asText()) == genus }) &&
                (rule["scientificPrefixes"].isEmpty || rule["scientificPrefixes"].any {
                    botanical == normalize(it.asText()) || botanical.startsWith(normalize(it.asText()) + " ")
                })
        } ?: schedules.first { it["key"].asText() == "perennial-seed" }
        var profile = mapper.treeToValue(schedule["profile"], ProductionProfile::class.java)
            .copy(startingUnit = species.defaultUnitType)
        val sourceLead = species.daysToHarvestMin?.takeIf { it in 1..3629 }
        if (sourceLead != null) {
            val templateLead = profile.steps.first { it.key == "start" }.daysBeforeHarvest
            profile = profile.copy(steps = profile.steps.map { step -> step.copy(daysBeforeHarvest = when {
                step.key == "purchase" -> sourceLead + 21
                step.key == "start" -> sourceLead
                step.daysBeforeHarvest == 0 -> 0
                templateLead != null && templateLead > 0 -> step.daysBeforeHarvest?.let { it * sourceLead / templateLead }
                else -> step.daysBeforeHarvest
            }) })
        }
        // A supplier germination percentage is a ceiling, not establishment success.
        if (species.defaultUnitType == UnitType.SEED && species.germinationRate != null) {
            profile = profile.copy(establishmentPercent = minOf(profile.establishmentPercent!!,
                BigDecimal(species.germinationRate.coerceIn(0, 100)).multiply(BigDecimal("0.9"))))
        }
        val months = schedule["harvestMonths"].map { it.asInt() }
        // Do not silently discard a supplier's flowering window when it conflicts.
        val harvestMonths = if (species.bloomMonths.isEmpty()) months else
            if (months.isEmpty()) species.bloomMonths else months.intersect(species.bloomMonths.toSet()).sorted()
        val conflict = months.isNotEmpty() && species.bloomMonths.isNotEmpty() && harvestMonths.isEmpty()
        return SuggestedProductionProfile(profile, DefaultScheduleInfo(
            schedule["key"].asText(), schedule["name"].asText(), catalogue["version"].asText(),
            catalogue["climate"].asText(), schedule["description"].asText() +
                " Tider och etableringsgrad är justerbara planeringsantaganden. Räkna med en säljbar enhet per etablerad planta på måldatumet, inte säsongsskörden." +
                (if (sourceLead != null) " Tiden till skörd ($sourceLead dagar) kommer från artens befintliga uppgifter." else "") +
                (if (conflict) " Leverantörens blomningsmånader avviker från grundschemat; kontrollera säsongen." else ""),
            schedule["reviewRequired"].asBoolean() || conflict, harvestMonths,
            schedule["plantingMonths"].map { it.asInt() }, schedule["sourceKeys"].map {
                mapper.treeToValue(catalogue["sources"][it.asText()], PlanningSource::class.java)
            },
        ))
    }

    fun groupNames(species: Species): List<String> {
        val genus = genus(species)
        val primary = GROUP_NAMES[genus] ?: genus.takeIf { it.matches(Regex("[a-z]{3,40}")) }
            ?.replaceFirstChar { it.uppercase() } ?: (species.commonNameSv ?: species.commonName)
        val names = mutableListOf(primary)
        if (genus == "dahlia") {
            val name = normalize(species.commonNameSv ?: species.commonName)
            val form = DAHLIA_FORMS.entries.firstOrNull { name.contains(it.key) }?.value
            if (form != null) names.add("Dahlior – $form")
        }
        return names.distinct()
    }

    private fun genus(species: Species): String {
        val latin = normalize(species.scientificName.orEmpty()).substringBefore(' ')
        if (latin.isNotEmpty()) return if (latin == "hellianthus") "helianthus" else latin
        val common = normalize(species.commonNameSv ?: species.commonName)
        return COMMON_GENERA.entries.firstOrNull { common.startsWith(it.key) }?.value ?: ""
    }

    companion object {
        private fun normalize(text: String) = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").trim()
        private val GROUP_NAMES = mapOf(
            "dahlia" to "Dahlior", "tulipa" to "Tulpaner", "narcissus" to "Narcisser",
            "zinnia" to "Zinnior", "cosmos" to "Rosenskäror", "lathyrus" to "Luktärter och vialer",
            "antirrhinum" to "Lejongap", "helianthus" to "Solrosor", "paeonia" to "Pioner",
            "ranunculus" to "Ranunkler", "anemone" to "Anemoner", "echinacea" to "Solhattar",
            "callistephus" to "Sommarastrar", "eustoma" to "Prärieklockor", "calendula" to "Ringblommor",
            "papaver" to "Vallmor", "tagetes" to "Tagetes", "viola" to "Violer och penséer",
            "dianthus" to "Nejlikor", "gladiolus" to "Gladioler", "allium" to "Prydnadslökar",
            "delphinium" to "Riddarsporrar", "consolida" to "Ettåriga riddarsporrar",
            "scabiosa" to "Väddar", "limonium" to "Risp", "matthiola" to "Lövkojor",
            "tropaeolum" to "Krasse", "digitalis" to "Fingerborgsblommor", "alcea" to "Stockrosor",
            "rudbeckia" to "Rudbeckior", "amaranthus" to "Amaranter", "celosia" to "Celosia",
            "gypsophila" to "Brudslöjor", "lavandula" to "Lavendel", "salvia" to "Salvior",
            "xerochrysum" to "Eterneller (Xerochrysum)", "helichrysum" to "Eterneller (Helichrysum)",
        )
        private val COMMON_GENERA = mapOf(
            "zinnia" to "zinnia", "rosenskara" to "cosmos", "luktart" to "lathyrus",
            "dahlia" to "dahlia", "tulpan" to "tulipa", "fingerborgsblomma" to "digitalis",
            "lovkoja" to "matthiola", "petunia" to "petunia", "somntuta" to "eschscholzia",
            "eucalyptus" to "eucalyptus", "hyacint" to "hyacinthus", "allium" to "allium",
        )
        private val DAHLIA_FORMS = mapOf(
            "pompon" to "pompon", "dinnerplate" to "dinnerplate", "dekorativ" to "dekorativa",
            "kaktus" to "kaktus", "halskras" to "halskrås", "nackros" to "näckros",
            "anemon" to "anemonblommande", "bolldahlia" to "boll",
        )
    }
}
