package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Species
import app.verdant.entity.UnitType
import app.verdant.repository.SharedScheduleRepository
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal
import java.text.Normalizer

data class SuggestedProductionProfile(val profile: ProductionProfile, val info: DefaultScheduleInfo)
data class SharedScheduleCatalogue(val schedules: List<SharedSchedule>, val assignments: Map<Long, String>)

/** Shared editable defaults. Organization profiles and existing workflows take precedence. */
@ApplicationScoped
class PlanningDefaults(private val repository: SharedScheduleRepository) {
    fun catalogue() = SharedScheduleCatalogue(repository.findAll(), repository.assignments())

    fun suggest(species: Species, catalogue: SharedScheduleCatalogue = catalogue()): SuggestedProductionProfile {
        val genus = genus(species)
        val botanical = normalize(species.scientificName.orEmpty())
        val assigned = catalogue.assignments[species.id]
        val schedule = catalogue.schedules.firstOrNull { it.key == assigned }
            ?: catalogue.schedules.firstOrNull { rule ->
                rule.autoMatch && species.defaultUnitType in rule.startingUnits &&
                    (rule.plantTypes.isEmpty() || species.plantType in rule.plantTypes) &&
                    (rule.genera.isEmpty() || rule.genera.any { normalize(it) == genus }) &&
                    (rule.scientificPrefixes.isEmpty() || rule.scientificPrefixes.any {
                        botanical == normalize(it) || botanical.startsWith(normalize(it) + " ")
                    })
            } ?: return unassigned(species)
        // An assigned species may later have its starting material changed in the species editor.
        if (schedule.profile.startingUnit != species.defaultUnitType) return unassigned(species)
        var profile = schedule.profile
        val sourceLead = species.daysToHarvestMin?.takeIf { schedule.useSpeciesTiming && it in 1..3629 }
        val propagation = if (profile.startingUnit == UnitType.SEED) "SOW" else "PLANT"
        val start = profile.steps.first { it.quantityBasis == PlanningQuantityBasis.START && it.activityType == propagation }
        if (sourceLead != null) {
            val templateLead = start.daysBeforeHarvest
            profile = profile.copy(steps = profile.steps.map { step -> step.copy(daysBeforeHarvest = when {
                step.activityType == "PURCHASE" -> sourceLead + 21
                step.key == start.key -> sourceLead
                step.daysBeforeHarvest == 0 -> 0
                templateLead != null && templateLead > 0 -> step.daysBeforeHarvest?.let { it * sourceLead / templateLead }
                else -> step.daysBeforeHarvest
            }) })
        }
        val germinationRate = species.germinationRate
        if (species.defaultUnitType == UnitType.SEED && germinationRate != null && profile.establishmentPercent != null) {
            profile = profile.copy(establishmentPercent = minOf(profile.establishmentPercent!!,
                BigDecimal(germinationRate.coerceIn(0, 100)).multiply(BigDecimal("0.9"))))
        }
        val months = schedule.harvestMonths
        val harvestMonths = if (species.bloomMonths.isEmpty()) months else
            if (months.isEmpty()) species.bloomMonths else months.intersect(species.bloomMonths.toSet()).sorted()
        val conflict = months.isNotEmpty() && species.bloomMonths.isNotEmpty() && harvestMonths.isEmpty()
        return SuggestedProductionProfile(profile, DefaultScheduleInfo(
            schedule.key, schedule.name, "${schedule.version} / ${schedule.revision}", schedule.climate,
            schedule.description + " Tider och etableringsgrad är justerbara planeringsantaganden." +
                (if (sourceLead != null) " Tiden till skörd ($sourceLead dagar) kommer från artens befintliga uppgifter." else "") +
                (if (conflict) " Leverantörens blomningsmånader avviker från grundschemat; kontrollera säsongen." else ""),
            schedule.reviewRequired || conflict, harvestMonths, schedule.plantingMonths, schedule.sources,
        ))
    }

    private fun unassigned(species: Species) = SuggestedProductionProfile(
        ProductionProfile(startingUnit = species.defaultUnitType, steps = listOf(
            LifecycleStep("purchase", "Purchase starting material", "PURCHASE", null, PlanningQuantityBasis.START),
            LifecycleStep("start", "Start growing", if (species.defaultUnitType == UnitType.SEED) "SOW" else "PLANT", null, PlanningQuantityBasis.START),
            LifecycleStep("harvest", "Harvest", "HARVEST", 0, PlanningQuantityBasis.OUTPUT),
        )),
        DefaultScheduleInfo("unassigned", "No matching schedule", "1", "", "Assign a shared schedule or configure a production profile.",
            true, emptyList(), emptyList(), emptyList()),
    )

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
