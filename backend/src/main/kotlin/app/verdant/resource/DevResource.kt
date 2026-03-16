package app.verdant.resource

import app.verdant.entity.*
import app.verdant.repository.*
import io.quarkus.security.Authenticated
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.jwt.JsonWebToken
import java.time.LocalDate

@Path("/api/dev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class DevResource(
    private val jwt: JsonWebToken,
    private val speciesRepository: SpeciesRepository,
    private val speciesGroupRepository: SpeciesGroupRepository,
    private val speciesTagRepository: SpeciesTagRepository,
    private val gardenRepository: GardenRepository,
    private val bedRepository: BedRepository,
    private val plantRepository: PlantRepository,
    private val plantEventRepository: PlantEventRepository,
    private val seedInventoryRepository: SeedInventoryRepository,
    private val scheduledTaskRepository: ScheduledTaskRepository,
) {
    private fun userId() = jwt.subject.toLong()

    data class SeedResult(
        val speciesCount: Int,
        val groupCount: Int,
        val tagCount: Int,
        val gardenCount: Int,
        val bedCount: Int,
        val plantCount: Int,
        val eventCount: Int,
        val seedInventoryCount: Int,
        val taskCount: Int,
    )

    @POST
    @Path("/seed")
    fun seedTestData(): Response {
        val userId = userId()
        val today = LocalDate.now()

        // ── Species Groups (system-level, no user_id) ──
        val groups = listOf("Vegetables", "Herbs", "Fruits", "Flowers", "Root Vegetables").map { name ->
            speciesGroupRepository.persist(SpeciesGroup(name = name))
        }
        val veggies = groups[0].id!!
        val herbs = groups[1].id!!
        val fruits = groups[2].id!!
        val flowers = groups[3].id!!
        val roots = groups[4].id!!

        // ── Tags (system-level, no user_id) ──
        val tags = listOf("Annual", "Perennial", "Heirloom", "Easy to grow", "Cold hardy").map { name ->
            speciesTagRepository.persist(SpeciesTag(name = name))
        }

        // ── 20 Species ──
        data class SpeciesDef(
            val commonName: String, val sv: String, val scientific: String,
            val groupId: Long, val positions: List<GrowingPosition>, val soils: List<SoilType>,
            val daysToSprout: Int, val daysToHarvest: Int, val heightCm: Int,
            val germinationRate: Int, val sowingDepthMm: Int,
            val bloomMonths: List<Int>, val sowingMonths: List<Int>,
            val tagIndices: List<Int> = emptyList(),
            val variantName: String? = null,
        )

        // Species data sourced from impecta.se and standard Scandinavian growing guides
        val speciesDefs = listOf(
            // Tomato: impecta.se BID=105 — germination 4-6 days at 25°C, spacing 35-55cm, full sun, 7-8 weeks sowing to transplant
            SpeciesDef("Tomato", "Tomat", "Solanum lycopersicum", veggies, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY), 5, 80, 150, 85, 10, listOf(6, 7, 8, 9), listOf(2, 3, 4), listOf(0, 3)),
            // Basil: impecta.se BID=219 — min 12°C, don't cover seeds (light-dependent), moist seed soil
            SpeciesDef("Basil", "Basilika", "Ocimum basilicum", herbs, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY, SoilType.SANDY), 7, 60, 45, 75, 0, listOf(7, 8, 9), listOf(3, 4, 5), listOf(0, 3)),
            // Carrot: standard — deep sandy/loamy soil, direct sow, 14-21 day germination
            SpeciesDef("Carrot", "Morot", "Daucus carota", roots, listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY), listOf(SoilType.SANDY, SoilType.LOAMY), 17, 75, 30, 70, 10, emptyList(), listOf(4, 5, 6), listOf(0, 4)),
            // Strawberry: standard Scandinavian — full sun, humus-rich soil, runners for propagation
            SpeciesDef("Strawberry", "Jordgubbe", "Fragaria × ananassa", fruits, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY), 14, 90, 25, 75, 3, listOf(5, 6), listOf(2, 3), listOf(1, 3)),
            // Sunflower: standard — direct sow after frost, 20-25mm deep, full sun, 7-10 day germination
            SpeciesDef("Sunflower", "Solros", "Helianthus annuus", flowers, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY, SoilType.SANDY), 8, 80, 200, 90, 25, listOf(7, 8, 9), listOf(4, 5), listOf(0)),
            // Cucumber: impecta.se BID=111 — germination "a few days" at 25°C, spacing 60-80cm, full sun, nutrient-rich soil
            SpeciesDef("Cucumber", "Gurka", "Cucumis sativus", veggies, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY), 5, 60, 40, 85, 15, listOf(6, 7, 8), listOf(4, 5), listOf(0, 3)),
            // Mint: impecta.se BID=252 — spreads vigorously, 40-50cm height, Jul-Sep bloom, use root barriers
            SpeciesDef("Mint", "Mynta", "Mentha spicata", herbs, listOf(GrowingPosition.PARTIALLY_SUNNY, GrowingPosition.SHADOWY), listOf(SoilType.LOAMY, SoilType.CLAY), 12, 90, 50, 70, 3, listOf(7, 8, 9), listOf(3, 4, 5), listOf(1, 3)),
            // Pepper: impecta.se BID=62 — germination at 25°C, light-dependent seeds (don't cover), sow Feb-March
            SpeciesDef("Pepper", "Paprika", "Capsicum annuum", veggies, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY), 12, 75, 60, 75, 0, listOf(6, 7, 8, 9), listOf(2, 3), listOf(0)),
            // Lavender: impecta.se BID=204 — full sun, well-drained soil, slow germination (may need cold stratification)
            SpeciesDef("Lavender", "Lavendel", "Lavandula angustifolia", flowers, listOf(GrowingPosition.SUNNY), listOf(SoilType.SANDY, SoilType.CHALKY), 21, 120, 60, 50, 3, listOf(6, 7, 8), listOf(2, 3), listOf(1, 4)),
            // Zucchini: standard — direct sow or transplant, warm soil needed, spacing 80-100cm
            SpeciesDef("Zucchini", "Zucchini", "Cucurbita pepo", veggies, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY), 6, 55, 50, 90, 20, listOf(6, 7, 8, 9), listOf(4, 5), listOf(0, 3)),
            // Parsley: standard — notoriously slow germination (up to 28 days), soak seeds to speed up
            SpeciesDef("Parsley", "Persilja", "Petroselinum crispum", herbs, listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY), listOf(SoilType.LOAMY), 21, 75, 30, 60, 5, emptyList(), listOf(3, 4, 5), listOf(0)),
            // Beetroot: standard Scandinavian — direct sow, multi-germ seeds, thin seedlings
            SpeciesDef("Beetroot", "Rödbeta", "Beta vulgaris", roots, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY, SoilType.SANDY), 10, 60, 35, 75, 15, emptyList(), listOf(4, 5, 6), listOf(0, 4)),
            // Raspberry: standard — perennial canes, full sun to part shade, humus-rich moist soil
            SpeciesDef("Raspberry", "Hallon", "Rubus idaeus", fruits, listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY), listOf(SoilType.LOAMY), 28, 365, 150, 70, 5, listOf(5, 6), listOf(3, 4), listOf(1)),
            // Dill: standard — direct sow, dislikes transplanting, self-seeds freely
            SpeciesDef("Dill", "Dill", "Anethum graveolens", herbs, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY, SoilType.SANDY), 10, 60, 90, 70, 5, listOf(6, 7, 8), listOf(4, 5, 6), listOf(0)),
            // Pea: standard — direct sow early spring, cool weather crop, nitrogen fixer
            SpeciesDef("Pea", "Ärt", "Pisum sativum", veggies, listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY), listOf(SoilType.LOAMY), 8, 65, 100, 85, 30, listOf(6, 7), listOf(3, 4, 5), listOf(0, 4)),
            // Lettuce: impecta.se BID=132 — spacing 25-30cm, semi-shade preferred, bolts in heat
            SpeciesDef("Lettuce", "Sallat", "Lactuca sativa", veggies, listOf(GrowingPosition.PARTIALLY_SUNNY), listOf(SoilType.LOAMY), 7, 45, 25, 85, 3, emptyList(), listOf(3, 4, 5, 6, 7), listOf(0, 3)),
            // Radish: standard — fastest vegetable, direct sow, 3-5 day germination, harvest in 25 days
            SpeciesDef("Radish", "Rädisa", "Raphanus sativus", roots, listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY), listOf(SoilType.SANDY, SoilType.LOAMY), 4, 25, 15, 90, 10, emptyList(), listOf(4, 5, 6, 7, 8), listOf(0, 3)),
            // Thyme: impecta.se BID=296 — full sun, well-drained sandy soil, sow late winter/early spring indoors
            SpeciesDef("Thyme", "Timjan", "Thymus vulgaris", herbs, listOf(GrowingPosition.SUNNY), listOf(SoilType.SANDY, SoilType.CHALKY), 14, 90, 25, 60, 3, listOf(6, 7), listOf(2, 3), listOf(1, 4)),
            // Blueberry: standard — acidic peaty soil required (pH 4.5-5.5), full sun, slow to establish
            SpeciesDef("Blueberry", "Blåbär", "Vaccinium corymbosum", fruits, listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY), listOf(SoilType.PEATY), 28, 365, 150, 55, 5, listOf(5, 6), listOf(3, 4), listOf(1)),
            // Marigold: standard — easy annual, direct sow after frost, fast germination
            SpeciesDef("Marigold", "Tagetes", "Tagetes erecta", flowers, listOf(GrowingPosition.SUNNY), listOf(SoilType.LOAMY, SoilType.SANDY), 5, 55, 40, 90, 5, listOf(6, 7, 8, 9, 10), listOf(4, 5), listOf(0, 3)),
        )

        val speciesIds = speciesDefs.map { def ->
            val species = speciesRepository.persist(Species(
                commonName = def.commonName,
                variantName = def.variantName,
                commonNameSv = def.sv,
                scientificName = def.scientific,
                imageFrontUrl = "https://storage.googleapis.com/verdant-species/system/${def.commonName.lowercase().replace(" ", "-")}/front.jpg",
                imageBackUrl = "https://storage.googleapis.com/verdant-species/system/${def.commonName.lowercase().replace(" ", "-")}/back.jpg",
                daysToSprout = def.daysToSprout,
                daysToHarvest = def.daysToHarvest,
                germinationTimeDays = def.daysToSprout,
                sowingDepthMm = def.sowingDepthMm,
                growingPositions = def.positions,
                soils = def.soils,
                heightCm = def.heightCm,
                bloomMonths = def.bloomMonths,
                sowingMonths = def.sowingMonths,
                germinationRate = def.germinationRate,
                groupId = def.groupId,
            ))
            val sid = species.id!!
            if (def.tagIndices.isNotEmpty()) {
                speciesRepository.setTagsForSpecies(sid, def.tagIndices.map { tags[it].id!! })
            }
            sid
        }

        // ── Gardens & Beds ──
        val garden1 = gardenRepository.persist(Garden(name = "Home Garden", emoji = "\uD83C\uDF3B", ownerId = userId, description = "Main vegetable and herb garden"))
        val garden2 = gardenRepository.persist(Garden(name = "Allotment", emoji = "\uD83C\uDF3E", ownerId = userId, description = "Community allotment plot"))

        val beds = listOf(
            bedRepository.persist(Bed(name = "Raised Bed A", description = "Tomatoes and peppers", gardenId = garden1.id!!)),
            bedRepository.persist(Bed(name = "Herb Spiral", description = "Kitchen herbs", gardenId = garden1.id!!)),
            bedRepository.persist(Bed(name = "Berry Corner", description = "Strawberries and raspberries", gardenId = garden1.id!!)),
            bedRepository.persist(Bed(name = "Main Plot", description = "Mixed vegetables", gardenId = garden2.id!!)),
            bedRepository.persist(Bed(name = "Flower Border", description = "Pollinator-friendly flowers", gardenId = garden2.id!!)),
        )

        // ── Plants & Events ──
        data class PlantDef(val speciesIdx: Int, val bedIdx: Int, val name: String, val status: PlantStatus, val seedCount: Int, val surviving: Int, val plantedDaysAgo: Long)

        val plantDefs = listOf(
            // Raised Bed A
            PlantDef(0, 0, "Tomato - Roma", PlantStatus.GROWING, 12, 8, 45),
            PlantDef(7, 0, "Pepper - Bell", PlantStatus.GROWING, 8, 6, 50),
            PlantDef(5, 0, "Cucumber - Marketmore", PlantStatus.PLANTED_OUT, 6, 5, 30),
            // Herb Spiral
            PlantDef(1, 1, "Sweet Basil", PlantStatus.GROWING, 20, 15, 35),
            PlantDef(6, 1, "Spearmint", PlantStatus.GROWING, 4, 4, 60),
            PlantDef(10, 1, "Flat Parsley", PlantStatus.POTTED_UP, 15, 10, 20),
            PlantDef(13, 1, "Garden Dill", PlantStatus.SEEDED, 30, 30, 5),
            PlantDef(17, 1, "Creeping Thyme", PlantStatus.GROWING, 6, 5, 90),
            // Berry Corner
            PlantDef(3, 2, "Strawberry Patch", PlantStatus.HARVESTED, 20, 18, 120),
            PlantDef(12, 2, "Raspberry Canes", PlantStatus.GROWING, 5, 5, 365),
            PlantDef(18, 2, "Blueberry Bush", PlantStatus.GROWING, 3, 3, 365),
            // Main Plot
            PlantDef(2, 3, "Nantes Carrots", PlantStatus.GROWING, 50, 40, 40),
            PlantDef(14, 3, "Sugar Snap Peas", PlantStatus.HARVESTED, 30, 25, 60),
            PlantDef(15, 3, "Butterhead Lettuce", PlantStatus.GROWING, 20, 18, 25),
            PlantDef(11, 3, "Golden Beetroot", PlantStatus.GROWING, 25, 20, 35),
            PlantDef(9, 3, "Green Zucchini", PlantStatus.GROWING, 4, 3, 40),
            PlantDef(16, 3, "Cherry Belle Radish", PlantStatus.HARVESTED, 40, 35, 20),
            // Flower Border
            PlantDef(4, 4, "Giant Sunflower", PlantStatus.GROWING, 10, 8, 30),
            PlantDef(8, 4, "English Lavender", PlantStatus.GROWING, 8, 7, 120),
            PlantDef(19, 4, "French Marigold", PlantStatus.GROWING, 15, 12, 25),
        )

        var eventCount = 0
        val plantIds = plantDefs.map { def ->
            val plantedDate = today.minusDays(def.plantedDaysAgo)
            val plant = plantRepository.persist(Plant(
                name = def.name,
                speciesId = speciesIds[def.speciesIdx],
                plantedDate = plantedDate,
                status = def.status,
                seedCount = def.seedCount,
                survivingCount = def.surviving,
                bedId = beds[def.bedIdx].id!!,
                userId = userId,
            ))
            val pid = plant.id!!

            // Create lifecycle events
            plantEventRepository.persist(PlantEvent(plantId = pid, eventType = PlantEventType.SEEDED, eventDate = plantedDate, plantCount = def.seedCount))
            eventCount++

            if (def.status in listOf(PlantStatus.POTTED_UP, PlantStatus.PLANTED_OUT, PlantStatus.GROWING, PlantStatus.HARVESTED)) {
                plantEventRepository.persist(PlantEvent(plantId = pid, eventType = PlantEventType.POTTED_UP, eventDate = plantedDate.plusDays(14), plantCount = def.surviving))
                eventCount++
            }
            if (def.status in listOf(PlantStatus.PLANTED_OUT, PlantStatus.GROWING, PlantStatus.HARVESTED)) {
                plantEventRepository.persist(PlantEvent(plantId = pid, eventType = PlantEventType.PLANTED_OUT, eventDate = plantedDate.plusDays(28), plantCount = def.surviving))
                eventCount++
            }
            if (def.status == PlantStatus.HARVESTED) {
                plantEventRepository.persist(PlantEvent(plantId = pid, eventType = PlantEventType.HARVESTED, eventDate = today.minusDays(3), plantCount = def.surviving, weightGrams = (def.surviving * 150).toDouble(), quantity = def.surviving * 2))
                eventCount++
            }

            pid
        }

        // ── Seed Inventory ──
        val inventorySpecies = listOf(0, 2, 4, 5, 9, 14, 15, 16) // Tomato, Carrot, Sunflower, Cucumber, Pea, Lettuce, Radish, Thyme(nope, use indices)
        var seedInvCount = 0
        inventorySpecies.forEach { idx ->
            seedInventoryRepository.persist(SeedInventory(
                userId = userId,
                speciesId = speciesIds[idx],
                quantity = (50..200).random(),
                collectionDate = today.minusDays((30..365).toLong()),
                expirationDate = today.plusDays((180..730).toLong()),
            ))
            seedInvCount++
        }

        // ── Scheduled Tasks ──
        val taskDefs = listOf(
            Triple(0, "SOW", 20),       // Sow more tomatoes
            Triple(5, "SOW", 10),       // Sow cucumbers
            Triple(10, "POT_UP", 8),    // Pot up parsley
            Triple(2, "PLANT", 15),     // Plant carrots
            Triple(14, "HARVEST", 20),  // Harvest peas
            Triple(9, "SOW", 6),        // Sow zucchini
        )
        taskDefs.forEach { (speciesIdx, activity, count) ->
            scheduledTaskRepository.persist(ScheduledTask(
                userId = userId,
                speciesId = speciesIds[speciesIdx],
                activityType = activity,
                deadline = today.plusDays((3..21).toLong()),
                targetCount = count,
                remainingCount = count,
            ))
        }

        return Response.ok(SeedResult(
            speciesCount = speciesDefs.size,
            groupCount = groups.size,
            tagCount = tags.size,
            gardenCount = 2,
            bedCount = beds.size,
            plantCount = plantDefs.size,
            eventCount = eventCount,
            seedInventoryCount = seedInvCount,
            taskCount = taskDefs.size,
        )).build()
    }

    private fun IntRange.toLong(): Long = this.random().toLong()
}
