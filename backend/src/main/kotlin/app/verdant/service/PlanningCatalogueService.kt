package app.verdant.service

import app.verdant.dto.ProductionProfile
import app.verdant.entity.SpeciesGroup
import app.verdant.repository.SpeciesGroupRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

data class PlanningCatalogueEntry(
    val speciesId: Long, val name: String, val groups: List<String>,
    val scheduleKey: String, val reviewRequired: Boolean, val profile: ProductionProfile,
)
data class PlanningCatalogueResult(val species: Int, val groupsCreated: Int, val membershipsAdded: Int)

@ApplicationScoped
class PlanningCatalogueService(
    private val species: SpeciesRepository,
    private val groups: SpeciesGroupRepository,
    private val defaults: PlanningDefaults,
) {
    fun preview(): List<PlanningCatalogueEntry> {
        val catalogue = defaults.catalogue()
        return species.findAll().filter { it.orgId == null }.map { sp ->
            val suggested = defaults.suggest(sp, catalogue)
            PlanningCatalogueEntry(sp.id!!, listOfNotNull(sp.commonNameSv ?: sp.commonName, sp.variantName).joinToString(" — "),
                defaults.groupNames(sp), suggested.info.key, suggested.info.reviewRequired, suggested.profile)
        }
    }

    /** Additive and repeatable: never removes memberships, edits species, or changes org profiles. */
    @Transactional
    fun apply(): PlanningCatalogueResult {
        groups.lockPlanningCatalogue()
        val entries = preview()
        val byName = groups.findAll().filter { it.orgId == null }.associateBy { it.name }.toMutableMap()
        val memberships = groups.findGroupIdsBySpeciesIds(entries.map { it.speciesId }.toSet())
        var created = 0
        var added = 0
        for (entry in entries) for (name in entry.groups) {
            val group = byName.getOrPut(name) {
                created++
                groups.persist(SpeciesGroup(name = name))
            }
            if (group.id !in memberships[entry.speciesId].orEmpty()) {
                groups.addSpeciesToGroup(entry.speciesId, group.id!!)
                added++
            }
        }
        return PlanningCatalogueResult(entries.size, created, added)
    }
}
