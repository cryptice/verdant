package app.verdant.resource

import app.verdant.entity.*
import app.verdant.repository.*
import io.quarkus.security.Authenticated
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.LocalDate

@Path("/api/dev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
class DevResource(
    private val userRepository: UserRepository,
    private val speciesRepository: SpeciesRepository,
    private val speciesGroupRepository: SpeciesGroupRepository,
    private val speciesTagRepository: SpeciesTagRepository,
    private val gardenRepository: GardenRepository,
    private val bedRepository: BedRepository,
    private val plantRepository: PlantRepository,
    private val plantEventRepository: PlantEventRepository,
    private val seedInventoryRepository: SeedInventoryRepository,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val seasonRepository: SeasonRepository,
    private val customerRepository: CustomerRepository,
    private val pestDiseaseLogRepository: PestDiseaseLogRepository,
    private val varietyTrialRepository: VarietyTrialRepository,
    private val bouquetRecipeRepository: BouquetRecipeRepository,
    private val successionScheduleRepository: SuccessionScheduleRepository,
    private val productionTargetRepository: ProductionTargetRepository,
) {
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
        val seasonCount: Int,
        val customerCount: Int,
        val pestDiseaseLogCount: Int,
        val varietyTrialCount: Int,
        val bouquetRecipeCount: Int,
        val successionScheduleCount: Int,
        val productionTargetCount: Int,
    )

    private data class HarvestDef(val date: LocalDate, val stems: Int, val lengthCm: Int, val grade: String, val customerIdx: Int)

    private data class Counters(var plants: Int = 0, var events: Int = 0)

    private fun createPlant(
        counters: Counters,
        userId: Long,
        name: String, speciesId: Long, bedId: Long, seasonId: Long,
        plantedDate: LocalDate, status: PlantStatus,
        seedCount: Int, surviving: Int,
        events: List<Pair<PlantEventType, LocalDate>>,
        harvests: List<HarvestDef> = emptyList(),
        customerIds: List<Long> = emptyList(),
    ): Long {
        val plant = plantRepository.persist(Plant(
            name = name, speciesId = speciesId, bedId = bedId, userId = userId,
            seasonId = seasonId, plantedDate = plantedDate, status = status,
            seedCount = seedCount, survivingCount = surviving,
        ))
        counters.plants++
        val pid = plant.id!!

        for ((type, date) in events) {
            plantEventRepository.persist(PlantEvent(plantId = pid, eventType = type, eventDate = date, plantCount = surviving))
            counters.events++
        }
        for (h in harvests) {
            val destId = if (customerIds.isNotEmpty()) customerIds[h.customerIdx % customerIds.size] else null
            plantEventRepository.persist(PlantEvent(
                plantId = pid, eventType = PlantEventType.HARVESTED, eventDate = h.date,
                stemCount = h.stems, stemLengthCm = h.lengthCm, qualityGrade = h.grade,
                harvestDestinationId = destId,
            ))
            counters.events++
        }
        return pid
    }

    private fun dahliaHarvests(start: LocalDate, count: Int): List<HarvestDef> {
        val result = mutableListOf<HarvestDef>()
        var d = start
        var remaining = count
        var ci = 0
        while (remaining > 0) {
            val batch = minOf(remaining, (8..15).random())
            val grade = if ((1..10).random() <= 8) "A" else "B"
            result.add(HarvestDef(d, batch, (50..65).random(), grade, ci % 5))
            remaining -= batch
            d = d.plusDays((3..5).toLong())
            ci++
        }
        return result
    }

    private fun annualHarvests(start: LocalDate, end: LocalDate, total: Int, lengthRange: IntRange): List<HarvestDef> {
        val result = mutableListOf<HarvestDef>()
        var d = start
        var remaining = total
        var ci = 0
        while (d.isBefore(end) && remaining > 0) {
            val batch = minOf(remaining, (5..15).random())
            val grade = when ((1..10).random()) { in 1..7 -> "A"; in 8..9 -> "B"; else -> "C" }
            result.add(HarvestDef(d, batch, lengthRange.random(), grade, ci % 5))
            remaining -= batch
            d = d.plusDays((3..7).toLong())
            ci++
        }
        return result
    }

    @POST
    @Path("/seed")
    fun seedTestData(@QueryParam("email") email: String?): Response {
        val targetEmail = email ?: "erik@l2c.se"
        val user = userRepository.findByEmail(targetEmail)
            ?: return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "User '$targetEmail' not found. Log in with Google first to create the account."))
                .build()
        val userId = user.id!!
        val counters = Counters()

        // ── Seasons ──
        val season2024 = seasonRepository.persist(Season(
            userId = userId, name = "2024", year = 2024,
            startDate = LocalDate.of(2024, 3, 1), endDate = LocalDate.of(2024, 11, 15),
            lastFrostDate = LocalDate.of(2024, 5, 12), firstFrostDate = LocalDate.of(2024, 10, 3),
            notes = "First season. Learning year with core varieties.", isActive = false,
        ))
        val season2025 = seasonRepository.persist(Season(
            userId = userId, name = "2025", year = 2025,
            startDate = LocalDate.of(2025, 3, 1), endDate = LocalDate.of(2025, 11, 15),
            lastFrostDate = LocalDate.of(2025, 5, 10), firstFrostDate = LocalDate.of(2025, 10, 1),
            notes = "Added lisianthus and cosmos. Better yields overall.", isActive = false,
        ))
        val season2026 = seasonRepository.persist(Season(
            userId = userId, name = "2026", year = 2026,
            startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 11, 15),
            lastFrostDate = LocalDate.of(2026, 5, 14), firstFrostDate = LocalDate.of(2026, 10, 5),
            notes = "Full production season. Trialing ranunculus.", isActive = true,
        ))
        val s24 = season2024.id!!
        val s25 = season2025.id!!
        val s26 = season2026.id!!

        // ── Species Groups ──
        val grpFlowers = speciesGroupRepository.persist(SpeciesGroup(name = "Flowers"))
        val grpFoliage = speciesGroupRepository.persist(SpeciesGroup(name = "Foliage"))
        val grpBulbs = speciesGroupRepository.persist(SpeciesGroup(name = "Bulbs & Tubers"))
        val flowersId = grpFlowers.id!!
        val foliageId = grpFoliage.id!!
        val bulbsId = grpBulbs.id!!

        // ── Tags ──
        val tagAnnual = speciesTagRepository.persist(SpeciesTag(name = "Annual"))
        val tagPerennial = speciesTagRepository.persist(SpeciesTag(name = "Perennial"))
        val tagCutFlower = speciesTagRepository.persist(SpeciesTag(name = "Cut flower"))
        val tagHeatLoving = speciesTagRepository.persist(SpeciesTag(name = "Heat loving"))
        val tagCoolSeason = speciesTagRepository.persist(SpeciesTag(name = "Cool season"))
        val tagLongSeason = speciesTagRepository.persist(SpeciesTag(name = "Long season"))
        val tags = listOf(tagAnnual, tagPerennial, tagCutFlower, tagHeatLoving, tagCoolSeason, tagLongSeason)

        // ── Species (15 cut flower varieties + 1 foliage) ──
        val cafeAuLait = speciesRepository.persist(Species(
            commonName = "Dahlia", variantName = "Cafe au Lait",
            commonNameSv = "Dahlia", variantNameSv = "Cafe au Lait",
            scientificName = "Dahlia x hybrida",
            daysToSprout = 14, daysToHarvest = 90, germinationTimeDays = 14,
            sowingDepthMm = 100, heightCm = 120,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY, SoilType.SANDY),
            bloomMonths = listOf(7, 8, 9, 10), sowingMonths = listOf(3, 4),
            germinationRate = 95, groupId = bulbsId,
            costPerSeedSek = 4500, expectedStemsPerPlant = 15, expectedVaseLifeDays = 6,
            plantType = PlantType.TUBER,
        ))
        val labyrinth = speciesRepository.persist(Species(
            commonName = "Dahlia", variantName = "Labyrinth",
            commonNameSv = "Dahlia", variantNameSv = "Labyrinth",
            scientificName = "Dahlia x hybrida",
            daysToSprout = 14, daysToHarvest = 85, germinationTimeDays = 14,
            sowingDepthMm = 100, heightCm = 110,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(7, 8, 9, 10), sowingMonths = listOf(3, 4),
            germinationRate = 95, groupId = bulbsId,
            costPerSeedSek = 3800, expectedStemsPerPlant = 12, expectedVaseLifeDays = 5,
            plantType = PlantType.TUBER,
        ))
        val cornel = speciesRepository.persist(Species(
            commonName = "Dahlia", variantName = "Cornel",
            commonNameSv = "Dahlia", variantNameSv = "Cornel",
            scientificName = "Dahlia x hybrida",
            daysToSprout = 14, daysToHarvest = 85, germinationTimeDays = 14,
            sowingDepthMm = 100, heightCm = 100,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(7, 8, 9), sowingMonths = listOf(3, 4),
            germinationRate = 93, groupId = bulbsId,
            costPerSeedSek = 3200, expectedStemsPerPlant = 10, expectedVaseLifeDays = 5,
            plantType = PlantType.TUBER,
        ))
        val zinniaGiant = speciesRepository.persist(Species(
            commonName = "Zinnia", variantName = "Benary's Giant Mix",
            commonNameSv = "Zinnia", variantNameSv = "Benary's Giant Mix",
            scientificName = "Zinnia elegans",
            daysToSprout = 5, daysToHarvest = 70, germinationTimeDays = 5,
            sowingDepthMm = 6, heightCm = 100,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY, SoilType.SANDY),
            bloomMonths = listOf(6, 7, 8, 9), sowingMonths = listOf(4, 5, 6),
            germinationRate = 85, groupId = flowersId,
            costPerSeedSek = 25, expectedStemsPerPlant = 8, expectedVaseLifeDays = 8,
            plantType = PlantType.ANNUAL,
        ))
        val zinniaQLO = speciesRepository.persist(Species(
            commonName = "Zinnia", variantName = "Queen Lime Orange",
            commonNameSv = "Zinnia", variantNameSv = "Queen Lime Orange",
            scientificName = "Zinnia elegans",
            daysToSprout = 5, daysToHarvest = 75, germinationTimeDays = 5,
            sowingDepthMm = 6, heightCm = 80,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(7, 8, 9), sowingMonths = listOf(4, 5, 6),
            germinationRate = 80, groupId = flowersId,
            costPerSeedSek = 45, expectedStemsPerPlant = 6, expectedVaseLifeDays = 8,
            plantType = PlantType.ANNUAL,
        ))
        val snapMadame = speciesRepository.persist(Species(
            commonName = "Snapdragon", variantName = "Madame Butterfly",
            commonNameSv = "Lejongap", variantNameSv = "Madame Butterfly",
            scientificName = "Antirrhinum majus",
            daysToSprout = 10, daysToHarvest = 90, germinationTimeDays = 10,
            sowingDepthMm = 0, heightCm = 90,
            growingPositions = listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(6, 7, 8), sowingMonths = listOf(2, 3),
            germinationRate = 75, groupId = flowersId,
            costPerSeedSek = 15, expectedStemsPerPlant = 5, expectedVaseLifeDays = 7,
            plantType = PlantType.ANNUAL,
        ))
        val snapChantilly = speciesRepository.persist(Species(
            commonName = "Snapdragon", variantName = "Chantilly",
            commonNameSv = "Lejongap", variantNameSv = "Chantilly",
            scientificName = "Antirrhinum majus",
            daysToSprout = 10, daysToHarvest = 85, germinationTimeDays = 10,
            sowingDepthMm = 0, heightCm = 100,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(6, 7, 8), sowingMonths = listOf(2, 3),
            germinationRate = 78, groupId = flowersId,
            costPerSeedSek = 20, expectedStemsPerPlant = 6, expectedVaseLifeDays = 8,
            plantType = PlantType.ANNUAL,
        ))
        val sunProCut = speciesRepository.persist(Species(
            commonName = "Sunflower", variantName = "ProCut Orange",
            commonNameSv = "Solros", variantNameSv = "ProCut Orange",
            scientificName = "Helianthus annuus",
            daysToSprout = 7, daysToHarvest = 60, germinationTimeDays = 7,
            sowingDepthMm = 25, heightCm = 150,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY, SoilType.SANDY),
            bloomMonths = listOf(7, 8, 9), sowingMonths = listOf(5, 6, 7),
            germinationRate = 90, groupId = flowersId,
            costPerSeedSek = 50, expectedStemsPerPlant = 1, expectedVaseLifeDays = 7,
            plantType = PlantType.ANNUAL,
        ))
        val sunSunrich = speciesRepository.persist(Species(
            commonName = "Sunflower", variantName = "Sunrich Gold",
            commonNameSv = "Solros", variantNameSv = "Sunrich Gold",
            scientificName = "Helianthus annuus",
            daysToSprout = 7, daysToHarvest = 65, germinationTimeDays = 7,
            sowingDepthMm = 25, heightCm = 160,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(7, 8, 9), sowingMonths = listOf(5, 6, 7),
            germinationRate = 88, groupId = flowersId,
            costPerSeedSek = 55, expectedStemsPerPlant = 1, expectedVaseLifeDays = 8,
            plantType = PlantType.ANNUAL,
        ))
        val lisEcho = speciesRepository.persist(Species(
            commonName = "Lisianthus", variantName = "Echo Blue",
            commonNameSv = "Prärieklocka", variantNameSv = "Echo Blue",
            scientificName = "Eustoma grandiflorum",
            daysToSprout = 14, daysToHarvest = 150, germinationTimeDays = 14,
            sowingDepthMm = 0, heightCm = 70,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(7, 8, 9), sowingMonths = listOf(1, 2),
            germinationRate = 60, groupId = flowersId,
            costPerSeedSek = 80, expectedStemsPerPlant = 4, expectedVaseLifeDays = 12,
            plantType = PlantType.ANNUAL,
        ))
        val lisRosita = speciesRepository.persist(Species(
            commonName = "Lisianthus", variantName = "Rosita Green",
            commonNameSv = "Prärieklocka", variantNameSv = "Rosita Green",
            scientificName = "Eustoma grandiflorum",
            daysToSprout = 14, daysToHarvest = 155, germinationTimeDays = 14,
            sowingDepthMm = 0, heightCm = 65,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(7, 8, 9), sowingMonths = listOf(1, 2),
            germinationRate = 55, groupId = flowersId,
            costPerSeedSek = 90, expectedStemsPerPlant = 3, expectedVaseLifeDays = 12,
            plantType = PlantType.ANNUAL,
        ))
        val sweetPea = speciesRepository.persist(Species(
            commonName = "Sweet Pea", variantName = "Spencer Mix",
            commonNameSv = "Luktärt", variantNameSv = "Spencer Mix",
            scientificName = "Lathyrus odoratus",
            daysToSprout = 10, daysToHarvest = 75, germinationTimeDays = 10,
            sowingDepthMm = 20, heightCm = 180,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(6, 7), sowingMonths = listOf(3, 4),
            germinationRate = 80, groupId = flowersId,
            costPerSeedSek = 15, expectedStemsPerPlant = 20, expectedVaseLifeDays = 5,
            plantType = PlantType.ANNUAL,
        ))
        val cosmosDC = speciesRepository.persist(Species(
            commonName = "Cosmos", variantName = "Double Click",
            commonNameSv = "Rosenskära", variantNameSv = "Double Click",
            scientificName = "Cosmos bipinnatus",
            daysToSprout = 7, daysToHarvest = 70, germinationTimeDays = 7,
            sowingDepthMm = 3, heightCm = 120,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY, SoilType.SANDY),
            bloomMonths = listOf(7, 8, 9, 10), sowingMonths = listOf(4, 5),
            germinationRate = 85, groupId = flowersId,
            costPerSeedSek = 20, expectedStemsPerPlant = 10, expectedVaseLifeDays = 6,
            plantType = PlantType.ANNUAL,
        ))
        val cosmosAL = speciesRepository.persist(Species(
            commonName = "Cosmos", variantName = "Apricot Lemonade",
            commonNameSv = "Rosenskära", variantNameSv = "Apricot Lemonade",
            scientificName = "Cosmos bipinnatus",
            daysToSprout = 7, daysToHarvest = 75, germinationTimeDays = 7,
            sowingDepthMm = 3, heightCm = 100,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(7, 8, 9), sowingMonths = listOf(4, 5),
            germinationRate = 82, groupId = flowersId,
            costPerSeedSek = 30, expectedStemsPerPlant = 8, expectedVaseLifeDays = 5,
            plantType = PlantType.ANNUAL,
        ))
        val eucalyptus = speciesRepository.persist(Species(
            commonName = "Eucalyptus", variantName = "Silver Dollar",
            commonNameSv = "Eukalyptus", variantNameSv = "Silver Dollar",
            scientificName = "Eucalyptus cinerea",
            daysToSprout = 21, daysToHarvest = 120, germinationTimeDays = 21,
            sowingDepthMm = 0, heightCm = 150,
            growingPositions = listOf(GrowingPosition.SUNNY),
            soils = listOf(SoilType.LOAMY, SoilType.SANDY),
            bloomMonths = emptyList(), sowingMonths = listOf(2, 3),
            germinationRate = 50, groupId = foliageId,
            costPerSeedSek = 35, expectedStemsPerPlant = 20, expectedVaseLifeDays = 14,
            plantType = PlantType.PERENNIAL,
        ))
        val ranunculus = speciesRepository.persist(Species(
            commonName = "Ranunculus", variantName = "Elegance Mix",
            commonNameSv = "Ranunkel", variantNameSv = "Elegance Mix",
            scientificName = "Ranunculus asiaticus",
            daysToSprout = 21, daysToHarvest = 90, germinationTimeDays = 21,
            sowingDepthMm = 30, heightCm = 45,
            growingPositions = listOf(GrowingPosition.SUNNY, GrowingPosition.PARTIALLY_SUNNY),
            soils = listOf(SoilType.LOAMY),
            bloomMonths = listOf(5, 6, 7), sowingMonths = listOf(2, 3),
            germinationRate = 70, groupId = bulbsId,
            costPerSeedSek = 200, expectedStemsPerPlant = 8, expectedVaseLifeDays = 7,
            plantType = PlantType.BULB,
        ))

        val allSpecies = listOf(
            cafeAuLait, labyrinth, cornel, zinniaGiant, zinniaQLO,
            snapMadame, snapChantilly, sunProCut, sunSunrich,
            lisEcho, lisRosita, sweetPea, cosmosDC, cosmosAL,
            eucalyptus, ranunculus,
        )

        // Tag assignments
        val annualId = tagAnnual.id!!
        val perennialId = tagPerennial.id!!
        val cutFlowerId = tagCutFlower.id!!
        val heatLovingId = tagHeatLoving.id!!
        val coolSeasonId = tagCoolSeason.id!!
        val longSeasonId = tagLongSeason.id!!

        listOf(cafeAuLait, labyrinth, cornel).forEach {
            speciesRepository.setTagsForSpecies(it.id!!, listOf(cutFlowerId, heatLovingId))
        }
        listOf(zinniaGiant, zinniaQLO).forEach {
            speciesRepository.setTagsForSpecies(it.id!!, listOf(annualId, cutFlowerId, heatLovingId))
        }
        listOf(snapMadame, snapChantilly).forEach {
            speciesRepository.setTagsForSpecies(it.id!!, listOf(annualId, cutFlowerId, coolSeasonId))
        }
        listOf(sunProCut, sunSunrich).forEach {
            speciesRepository.setTagsForSpecies(it.id!!, listOf(annualId, cutFlowerId))
        }
        listOf(lisEcho, lisRosita).forEach {
            speciesRepository.setTagsForSpecies(it.id!!, listOf(annualId, cutFlowerId, longSeasonId))
        }
        speciesRepository.setTagsForSpecies(sweetPea.id!!, listOf(annualId, cutFlowerId, coolSeasonId))
        listOf(cosmosDC, cosmosAL).forEach {
            speciesRepository.setTagsForSpecies(it.id!!, listOf(annualId, cutFlowerId))
        }
        speciesRepository.setTagsForSpecies(eucalyptus.id!!, listOf(perennialId))
        speciesRepository.setTagsForSpecies(ranunculus.id!!, listOf(cutFlowerId, coolSeasonId))

        // ── Customers ──
        val cAndersson = customerRepository.persist(Customer(userId = userId, name = "Blomsterhandel Andersson", channel = Channel.FLORIST, contactInfo = "anna@andersson-blommor.se"))
        val cStortorget = customerRepository.persist(Customer(userId = userId, name = "Stortorgets marknad", channel = Channel.FARMERS_MARKET, contactInfo = "Lördagar 08-14"))
        val cCSA = customerRepository.persist(Customer(userId = userId, name = "Blomster-CSA", channel = Channel.CSA, contactInfo = "12 medlemmar, leverans torsdag"))
        val cWedding = customerRepository.persist(Customer(userId = userId, name = "Weddingflowers.se", channel = Channel.WEDDING, contactInfo = "info@weddingflowers.se"))
        val cICA = customerRepository.persist(Customer(userId = userId, name = "ICA Maxi Blommor", channel = Channel.WHOLESALE, contactInfo = "Beställning senast onsdag"))
        val customerIds = listOf(cAndersson.id!!, cStortorget.id!!, cCSA.id!!, cWedding.id!!, cICA.id!!)

        // ── Garden & Beds ──
        val garden = gardenRepository.persist(Garden(
            name = "Blomstergården", emoji = "\uD83C\uDF38", ownerId = userId,
            description = "The Flower Farm — commercial cut flower production",
        ))
        val gId = garden.id!!
        val bedDahlia = bedRepository.persist(Bed(name = "Dahlia field", description = "Dahlias — tubers planted 45cm spacing", gardenId = gId, lengthMeters = 20.0, widthMeters = 1.2))
        val bedAnnualA = bedRepository.persist(Bed(name = "Annual cuts A", description = "Zinnias and snapdragons", gardenId = gId, lengthMeters = 15.0, widthMeters = 1.2))
        val bedAnnualB = bedRepository.persist(Bed(name = "Annual cuts B", description = "Sunflowers and cosmos", gardenId = gId, lengthMeters = 15.0, widthMeters = 1.2))
        val bedLis = bedRepository.persist(Bed(name = "Lisianthus house", description = "Heated tunnel for lisianthus", gardenId = gId, lengthMeters = 10.0, widthMeters = 1.2))
        val bedSweet = bedRepository.persist(Bed(name = "Sweet pea tunnel", description = "Cordon-grown sweet peas", gardenId = gId, lengthMeters = 12.0, widthMeters = 1.2))
        val bedFoliage = bedRepository.persist(Bed(name = "Foliage", description = "Eucalyptus and filler greenery", gardenId = gId, lengthMeters = 8.0, widthMeters = 1.2))
        val beds = listOf(bedDahlia, bedAnnualA, bedAnnualB, bedLis, bedSweet, bedFoliage)

        // ══════════════════════════════════════════
        //  2024 SEASON — first year, complete
        // ══════════════════════════════════════════

        // Dahlias 2024: 3 varieties, ~500 stems total
        createPlant(counters, userId, "Cafe au Lait 2024", cafeAuLait.id!!, bedDahlia.id!!, s24,
            LocalDate.of(2024, 4, 15), PlantStatus.DORMANT, 20, 18,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 5, 20),
                PlantEventType.BUDDING to LocalDate.of(2024, 6, 28),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 7, 8),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2024, 8, 1),
                PlantEventType.LAST_BLOOM to LocalDate.of(2024, 9, 20),
                PlantEventType.LIFTED to LocalDate.of(2024, 10, 10),
                PlantEventType.STORED to LocalDate.of(2024, 10, 12),
            ),
            dahliaHarvests(LocalDate.of(2024, 7, 10), 200), customerIds,
        )
        createPlant(counters, userId, "Labyrinth 2024", labyrinth.id!!, bedDahlia.id!!, s24,
            LocalDate.of(2024, 4, 15), PlantStatus.DORMANT, 15, 14,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 5, 20),
                PlantEventType.BUDDING to LocalDate.of(2024, 7, 1),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 7, 12),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2024, 8, 5),
                PlantEventType.LAST_BLOOM to LocalDate.of(2024, 9, 18),
                PlantEventType.LIFTED to LocalDate.of(2024, 10, 10),
                PlantEventType.STORED to LocalDate.of(2024, 10, 12),
            ),
            dahliaHarvests(LocalDate.of(2024, 7, 14), 170), customerIds,
        )
        createPlant(counters, userId, "Cornel 2024", cornel.id!!, bedDahlia.id!!, s24,
            LocalDate.of(2024, 4, 15), PlantStatus.DORMANT, 12, 11,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 5, 20),
                PlantEventType.BUDDING to LocalDate.of(2024, 7, 3),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 7, 15),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2024, 8, 8),
                PlantEventType.LAST_BLOOM to LocalDate.of(2024, 9, 10),
                PlantEventType.LIFTED to LocalDate.of(2024, 10, 10),
                PlantEventType.STORED to LocalDate.of(2024, 10, 12),
            ),
            dahliaHarvests(LocalDate.of(2024, 7, 18), 130), customerIds,
        )

        // Zinnias 2024: 2 successions, ~300 stems
        createPlant(counters, userId, "Benary's Giant Mix — sow 1", zinniaGiant.id!!, bedAnnualA.id!!, s24,
            LocalDate.of(2024, 4, 10), PlantStatus.REMOVED, 50, 40,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 4, 10),
                PlantEventType.POTTED_UP to LocalDate.of(2024, 4, 25),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 5, 20),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 6, 25),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2024, 7, 15),
                PlantEventType.LAST_BLOOM to LocalDate.of(2024, 9, 5),
            ),
            annualHarvests(LocalDate.of(2024, 6, 28), LocalDate.of(2024, 9, 10), 180, 45..65), customerIds,
        )
        createPlant(counters, userId, "Benary's Giant Mix — sow 2", zinniaGiant.id!!, bedAnnualA.id!!, s24,
            LocalDate.of(2024, 5, 10), PlantStatus.REMOVED, 40, 35,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 5, 10),
                PlantEventType.POTTED_UP to LocalDate.of(2024, 5, 25),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 6, 10),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 7, 20),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2024, 8, 10),
                PlantEventType.LAST_BLOOM to LocalDate.of(2024, 9, 15),
            ),
            annualHarvests(LocalDate.of(2024, 7, 22), LocalDate.of(2024, 9, 20), 120, 45..65), customerIds,
        )

        // Snapdragons 2024: ~200 stems
        createPlant(counters, userId, "Madame Butterfly 2024", snapMadame.id!!, bedAnnualA.id!!, s24,
            LocalDate.of(2024, 3, 1), PlantStatus.REMOVED, 60, 50,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 3, 1),
                PlantEventType.POTTED_UP to LocalDate.of(2024, 3, 25),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 5, 15),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 6, 10),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2024, 7, 1),
                PlantEventType.LAST_BLOOM to LocalDate.of(2024, 8, 15),
            ),
            annualHarvests(LocalDate.of(2024, 6, 12), LocalDate.of(2024, 8, 20), 200, 55..75), customerIds,
        )

        // Sunflowers 2024: 3 successions, ~150 stems
        createPlant(counters, userId, "ProCut Orange — sow 1", sunProCut.id!!, bedAnnualB.id!!, s24,
            LocalDate.of(2024, 5, 5), PlantStatus.REMOVED, 25, 22,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 5, 5),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 5, 20),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 7, 5),
            ),
            annualHarvests(LocalDate.of(2024, 7, 5), LocalDate.of(2024, 7, 20), 20, 60..80), customerIds,
        )
        createPlant(counters, userId, "ProCut Orange — sow 2", sunProCut.id!!, bedAnnualB.id!!, s24,
            LocalDate.of(2024, 5, 25), PlantStatus.REMOVED, 30, 27,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 5, 25),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 6, 8),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 7, 25),
            ),
            annualHarvests(LocalDate.of(2024, 7, 25), LocalDate.of(2024, 8, 10), 25, 60..80), customerIds,
        )
        createPlant(counters, userId, "Sunrich Gold — sow 1", sunSunrich.id!!, bedAnnualB.id!!, s24,
            LocalDate.of(2024, 6, 5), PlantStatus.REMOVED, 40, 35,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 6, 5),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 6, 20),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 8, 10),
            ),
            annualHarvests(LocalDate.of(2024, 8, 10), LocalDate.of(2024, 9, 5), 105, 65..80), customerIds,
        )

        // Sweet Peas 2024: ~250 stems
        createPlant(counters, userId, "Spencer Mix 2024", sweetPea.id!!, bedSweet.id!!, s24,
            LocalDate.of(2024, 4, 1), PlantStatus.REMOVED, 80, 65,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 4, 1),
                PlantEventType.POTTED_UP to LocalDate.of(2024, 4, 15),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 5, 10),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2024, 6, 10),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2024, 6, 25),
                PlantEventType.LAST_BLOOM to LocalDate.of(2024, 7, 25),
            ),
            annualHarvests(LocalDate.of(2024, 6, 12), LocalDate.of(2024, 7, 28), 250, 40..55), customerIds,
        )

        // Eucalyptus 2024 — established, foliage harvest
        createPlant(counters, userId, "Silver Dollar 2024", eucalyptus.id!!, bedFoliage.id!!, s24,
            LocalDate.of(2024, 3, 15), PlantStatus.REMOVED, 10, 8,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2024, 3, 15),
                PlantEventType.POTTED_UP to LocalDate.of(2024, 4, 20),
                PlantEventType.PLANTED_OUT to LocalDate.of(2024, 6, 1),
            ),
            annualHarvests(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 10, 1), 80, 40..60), customerIds,
        )

        // ══════════════════════════════════════════
        //  2025 SEASON — improved yields, added cosmos + lisianthus
        // ══════════════════════════════════════════

        // Dahlias 2025: ~700 stems
        createPlant(counters, userId, "Cafe au Lait 2025", cafeAuLait.id!!, bedDahlia.id!!, s25,
            LocalDate.of(2025, 4, 10), PlantStatus.DORMANT, 25, 24,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 18),
                PlantEventType.PINCHED to LocalDate.of(2025, 6, 12),
                PlantEventType.BUDDING to LocalDate.of(2025, 6, 25),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 5),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 7, 25),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 25),
                PlantEventType.LIFTED to LocalDate.of(2025, 10, 5),
                PlantEventType.DIVIDED to LocalDate.of(2025, 10, 8),
                PlantEventType.STORED to LocalDate.of(2025, 10, 10),
            ),
            dahliaHarvests(LocalDate.of(2025, 7, 7), 280), customerIds,
        )
        createPlant(counters, userId, "Labyrinth 2025", labyrinth.id!!, bedDahlia.id!!, s25,
            LocalDate.of(2025, 4, 10), PlantStatus.DORMANT, 20, 19,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 18),
                PlantEventType.PINCHED to LocalDate.of(2025, 6, 14),
                PlantEventType.BUDDING to LocalDate.of(2025, 6, 28),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 10),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 8, 1),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 20),
                PlantEventType.LIFTED to LocalDate.of(2025, 10, 5),
                PlantEventType.STORED to LocalDate.of(2025, 10, 10),
            ),
            dahliaHarvests(LocalDate.of(2025, 7, 12), 230), customerIds,
        )
        createPlant(counters, userId, "Cornel 2025", cornel.id!!, bedDahlia.id!!, s25,
            LocalDate.of(2025, 4, 10), PlantStatus.DORMANT, 18, 17,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 18),
                PlantEventType.BUDDING to LocalDate.of(2025, 7, 1),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 12),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 8, 5),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 15),
                PlantEventType.LIFTED to LocalDate.of(2025, 10, 5),
                PlantEventType.STORED to LocalDate.of(2025, 10, 10),
            ),
            dahliaHarvests(LocalDate.of(2025, 7, 14), 190), customerIds,
        )

        // Zinnias 2025: ~450 stems
        createPlant(counters, userId, "Benary's Giant Mix 2025 sow 1", zinniaGiant.id!!, bedAnnualA.id!!, s25,
            LocalDate.of(2025, 4, 5), PlantStatus.REMOVED, 60, 52,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 4, 5),
                PlantEventType.POTTED_UP to LocalDate.of(2025, 4, 18),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 15),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 6, 22),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 7, 10),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 1),
            ),
            annualHarvests(LocalDate.of(2025, 6, 25), LocalDate.of(2025, 9, 5), 200, 50..70), customerIds,
        )
        createPlant(counters, userId, "Queen Lime Orange 2025 sow 1", zinniaQLO.id!!, bedAnnualA.id!!, s25,
            LocalDate.of(2025, 4, 20), PlantStatus.REMOVED, 40, 34,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 4, 20),
                PlantEventType.POTTED_UP to LocalDate.of(2025, 5, 3),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 25),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 5),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 7, 25),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 10),
            ),
            annualHarvests(LocalDate.of(2025, 7, 8), LocalDate.of(2025, 9, 15), 150, 45..60), customerIds,
        )
        createPlant(counters, userId, "Benary's Giant Mix 2025 sow 2", zinniaGiant.id!!, bedAnnualA.id!!, s25,
            LocalDate.of(2025, 5, 15), PlantStatus.REMOVED, 40, 36,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 5, 15),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 6, 5),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 20),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 15),
            ),
            annualHarvests(LocalDate.of(2025, 7, 22), LocalDate.of(2025, 9, 18), 100, 50..65), customerIds,
        )

        // Snapdragons 2025: ~300 stems
        createPlant(counters, userId, "Madame Butterfly 2025", snapMadame.id!!, bedAnnualA.id!!, s25,
            LocalDate.of(2025, 2, 25), PlantStatus.REMOVED, 70, 60,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 2, 25),
                PlantEventType.POTTED_UP to LocalDate.of(2025, 3, 20),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 10),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 6, 5),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 6, 25),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 8, 20),
            ),
            annualHarvests(LocalDate.of(2025, 6, 8), LocalDate.of(2025, 8, 25), 180, 55..75), customerIds,
        )
        createPlant(counters, userId, "Chantilly 2025", snapChantilly.id!!, bedAnnualA.id!!, s25,
            LocalDate.of(2025, 2, 25), PlantStatus.REMOVED, 50, 42,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 2, 25),
                PlantEventType.POTTED_UP to LocalDate.of(2025, 3, 22),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 12),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 6, 8),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 7, 1),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 8, 18),
            ),
            annualHarvests(LocalDate.of(2025, 6, 10), LocalDate.of(2025, 8, 22), 120, 60..80), customerIds,
        )

        // Cosmos 2025 (new): ~200 stems
        createPlant(counters, userId, "Double Click 2025", cosmosDC.id!!, bedAnnualB.id!!, s25,
            LocalDate.of(2025, 4, 15), PlantStatus.REMOVED, 50, 45,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 4, 15),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 20),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 5),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 8, 1),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 25),
            ),
            annualHarvests(LocalDate.of(2025, 7, 8), LocalDate.of(2025, 9, 28), 130, 50..70), customerIds,
        )
        createPlant(counters, userId, "Apricot Lemonade 2025", cosmosAL.id!!, bedAnnualB.id!!, s25,
            LocalDate.of(2025, 4, 20), PlantStatus.REMOVED, 35, 30,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 4, 20),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 22),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 10),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 8, 5),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 20),
            ),
            annualHarvests(LocalDate.of(2025, 7, 12), LocalDate.of(2025, 9, 22), 70, 45..65), customerIds,
        )

        // Lisianthus 2025 (new): ~100 stems
        createPlant(counters, userId, "Echo Blue 2025", lisEcho.id!!, bedLis.id!!, s25,
            LocalDate.of(2025, 1, 15), PlantStatus.REMOVED, 40, 22,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 1, 15),
                PlantEventType.POTTED_UP to LocalDate.of(2025, 3, 1),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 15),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 15),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 8, 5),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 10),
            ),
            annualHarvests(LocalDate.of(2025, 7, 18), LocalDate.of(2025, 9, 12), 60, 50..65), customerIds,
        )
        createPlant(counters, userId, "Rosita Green 2025", lisRosita.id!!, bedLis.id!!, s25,
            LocalDate.of(2025, 1, 20), PlantStatus.REMOVED, 30, 15,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 1, 20),
                PlantEventType.POTTED_UP to LocalDate.of(2025, 3, 5),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 18),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 20),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 9, 5),
            ),
            annualHarvests(LocalDate.of(2025, 7, 22), LocalDate.of(2025, 9, 8), 40, 45..60), customerIds,
        )

        // Sunflowers 2025
        createPlant(counters, userId, "ProCut Orange 2025 sow 1", sunProCut.id!!, bedAnnualB.id!!, s25,
            LocalDate.of(2025, 5, 1), PlantStatus.REMOVED, 30, 28,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 5, 1),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 15),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 1),
            ),
            annualHarvests(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 15), 26, 65..80), customerIds,
        )
        createPlant(counters, userId, "Sunrich Gold 2025 sow 1", sunSunrich.id!!, bedAnnualB.id!!, s25,
            LocalDate.of(2025, 5, 20), PlantStatus.REMOVED, 40, 36,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 5, 20),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 6, 5),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 7, 25),
            ),
            annualHarvests(LocalDate.of(2025, 7, 25), LocalDate.of(2025, 8, 10), 34, 65..80), customerIds,
        )
        createPlant(counters, userId, "ProCut Orange 2025 sow 2", sunProCut.id!!, bedAnnualB.id!!, s25,
            LocalDate.of(2025, 6, 10), PlantStatus.REMOVED, 35, 32,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 6, 10),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 6, 25),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 8, 15),
            ),
            annualHarvests(LocalDate.of(2025, 8, 15), LocalDate.of(2025, 8, 30), 30, 60..75), customerIds,
        )

        // Sweet Peas 2025
        createPlant(counters, userId, "Spencer Mix 2025", sweetPea.id!!, bedSweet.id!!, s25,
            LocalDate.of(2025, 3, 20), PlantStatus.REMOVED, 100, 85,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2025, 3, 20),
                PlantEventType.POTTED_UP to LocalDate.of(2025, 4, 5),
                PlantEventType.PLANTED_OUT to LocalDate.of(2025, 5, 5),
                PlantEventType.FIRST_BLOOM to LocalDate.of(2025, 6, 5),
                PlantEventType.PEAK_BLOOM to LocalDate.of(2025, 6, 20),
                PlantEventType.LAST_BLOOM to LocalDate.of(2025, 7, 20),
            ),
            annualHarvests(LocalDate.of(2025, 6, 8), LocalDate.of(2025, 7, 22), 300, 40..55), customerIds,
        )

        // Eucalyptus 2025 — continuing perennial
        createPlant(counters, userId, "Silver Dollar 2025", eucalyptus.id!!, bedFoliage.id!!, s25,
            LocalDate.of(2025, 3, 1), PlantStatus.REMOVED, 8, 8,
            listOf(
                PlantEventType.NOTE to LocalDate.of(2025, 4, 1),
            ),
            annualHarvests(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 10, 1), 120, 40..65), customerIds,
        )

        // ══════════════════════════════════════════
        //  2026 SEASON — current, in-progress
        // ══════════════════════════════════════════

        // Dahlias 2026: planted, growing (pinched, not yet blooming)
        createPlant(counters, userId, "Cafe au Lait 2026", cafeAuLait.id!!, bedDahlia.id!!, s26,
            LocalDate.of(2026, 4, 8), PlantStatus.GROWING, 30, 28,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 16),
                PlantEventType.PINCHED to LocalDate.of(2026, 6, 8),
            ),
        )
        createPlant(counters, userId, "Labyrinth 2026", labyrinth.id!!, bedDahlia.id!!, s26,
            LocalDate.of(2026, 4, 8), PlantStatus.GROWING, 24, 23,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 16),
                PlantEventType.PINCHED to LocalDate.of(2026, 6, 10),
            ),
        )
        createPlant(counters, userId, "Cornel 2026", cornel.id!!, bedDahlia.id!!, s26,
            LocalDate.of(2026, 4, 8), PlantStatus.PLANTED_OUT, 20, 19,
            listOf(
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 16),
            ),
        )

        // Zinnias 2026: first succession planted out, second potted up
        createPlant(counters, userId, "Benary's Giant Mix 2026 sow 1", zinniaGiant.id!!, bedAnnualA.id!!, s26,
            LocalDate.of(2026, 4, 1), PlantStatus.PLANTED_OUT, 60, 55,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 4, 1),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 4, 14),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 12),
            ),
        )
        createPlant(counters, userId, "Queen Lime Orange 2026 sow 1", zinniaQLO.id!!, bedAnnualA.id!!, s26,
            LocalDate.of(2026, 4, 15), PlantStatus.PLANTED_OUT, 45, 40,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 4, 15),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 4, 28),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 20),
            ),
        )
        createPlant(counters, userId, "Benary's Giant Mix 2026 sow 2", zinniaGiant.id!!, bedAnnualA.id!!, s26,
            LocalDate.of(2026, 4, 29), PlantStatus.POTTED_UP, 50, 45,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 4, 29),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 5, 12),
            ),
        )

        // Snapdragons 2026
        createPlant(counters, userId, "Madame Butterfly 2026", snapMadame.id!!, bedAnnualA.id!!, s26,
            LocalDate.of(2026, 2, 20), PlantStatus.PLANTED_OUT, 80, 68,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 2, 20),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 3, 15),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 8),
            ),
        )
        createPlant(counters, userId, "Chantilly 2026", snapChantilly.id!!, bedAnnualA.id!!, s26,
            LocalDate.of(2026, 2, 20), PlantStatus.PLANTED_OUT, 60, 52,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 2, 20),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 3, 18),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 10),
            ),
        )

        // Sunflowers 2026: first succession planted
        createPlant(counters, userId, "ProCut Orange 2026 sow 1", sunProCut.id!!, bedAnnualB.id!!, s26,
            LocalDate.of(2026, 5, 1), PlantStatus.PLANTED_OUT, 35, 33,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 5, 1),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 15),
            ),
        )
        createPlant(counters, userId, "Sunrich Gold 2026 sow 1", sunSunrich.id!!, bedAnnualB.id!!, s26,
            LocalDate.of(2026, 5, 10), PlantStatus.SEEDED, 40, 38,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 5, 10),
            ),
        )

        // Lisianthus 2026: planted, growing
        createPlant(counters, userId, "Echo Blue 2026", lisEcho.id!!, bedLis.id!!, s26,
            LocalDate.of(2026, 1, 10), PlantStatus.PLANTED_OUT, 50, 30,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 1, 10),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 2, 25),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 10),
            ),
        )
        createPlant(counters, userId, "Rosita Green 2026", lisRosita.id!!, bedLis.id!!, s26,
            LocalDate.of(2026, 1, 15), PlantStatus.PLANTED_OUT, 40, 22,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 1, 15),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 3, 1),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 12),
            ),
        )

        // Sweet Peas 2026
        createPlant(counters, userId, "Spencer Mix 2026", sweetPea.id!!, bedSweet.id!!, s26,
            LocalDate.of(2026, 3, 15), PlantStatus.PLANTED_OUT, 120, 100,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 3, 15),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 3, 30),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 1),
            ),
        )

        // Cosmos 2026
        createPlant(counters, userId, "Double Click 2026", cosmosDC.id!!, bedAnnualB.id!!, s26,
            LocalDate.of(2026, 4, 10), PlantStatus.PLANTED_OUT, 60, 55,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 4, 10),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 18),
            ),
        )
        createPlant(counters, userId, "Apricot Lemonade 2026", cosmosAL.id!!, bedAnnualB.id!!, s26,
            LocalDate.of(2026, 4, 15), PlantStatus.PLANTED_OUT, 40, 36,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 4, 15),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 5, 20),
            ),
        )

        // Eucalyptus 2026 — perennial, continuing
        createPlant(counters, userId, "Silver Dollar 2026", eucalyptus.id!!, bedFoliage.id!!, s26,
            LocalDate.of(2026, 3, 1), PlantStatus.GROWING, 8, 8,
            listOf(
                PlantEventType.NOTE to LocalDate.of(2026, 3, 15),
            ),
        )

        // Ranunculus 2026 — new trial variety
        createPlant(counters, userId, "Elegance Mix 2026 (trial)", ranunculus.id!!, bedFoliage.id!!, s26,
            LocalDate.of(2026, 3, 1), PlantStatus.GROWING, 50, 35,
            listOf(
                PlantEventType.SEEDED to LocalDate.of(2026, 3, 1),
                PlantEventType.POTTED_UP to LocalDate.of(2026, 3, 25),
                PlantEventType.PLANTED_OUT to LocalDate.of(2026, 4, 20),
                PlantEventType.BUDDING to LocalDate.of(2026, 5, 28),
            ),
        )

        // ── Seed Inventory (2026 season) ──
        var seedInvCount = 0
        fun inv(speciesId: Long, qty: Int, cost: Int, unit: UnitType = UnitType.SEED) {
            seedInventoryRepository.persist(SeedInventory(
                userId = userId, speciesId = speciesId, quantity = qty, seasonId = s26,
                collectionDate = LocalDate.of(2026, 1, 15),
                expirationDate = LocalDate.of(2028, 12, 31),
                costPerUnitSek = cost, unitType = unit,
            ))
            seedInvCount++
        }
        inv(cafeAuLait.id!!, 40, 4500, UnitType.TUBER)
        inv(labyrinth.id!!, 30, 3800, UnitType.TUBER)
        inv(cornel.id!!, 25, 3200, UnitType.TUBER)
        inv(zinniaGiant.id!!, 500, 25)
        inv(zinniaQLO.id!!, 300, 45)
        inv(snapMadame.id!!, 400, 15)
        inv(snapChantilly.id!!, 300, 20)
        inv(sunProCut.id!!, 250, 50)
        inv(sunSunrich.id!!, 200, 55)
        inv(lisEcho.id!!, 150, 80)
        inv(lisRosita.id!!, 100, 90)
        inv(sweetPea.id!!, 600, 15)
        inv(cosmosDC.id!!, 400, 20)
        inv(cosmosAL.id!!, 250, 30)
        inv(eucalyptus.id!!, 50, 35)
        inv(ranunculus.id!!, 80, 200, UnitType.BULB)

        // ── Succession Schedules (2026) ──
        val succZinnia = successionScheduleRepository.persist(SuccessionSchedule(
            userId = userId, seasonId = s26, speciesId = zinniaGiant.id!!,
            bedId = bedAnnualA.id!!, firstSowDate = LocalDate.of(2026, 4, 15),
            intervalDays = 14, totalSuccessions = 4, seedsPerSuccession = 50,
            notes = "Benary's Giant Mix — 14 day intervals for continuous harvest",
        ))
        val succSunflower = successionScheduleRepository.persist(SuccessionSchedule(
            userId = userId, seasonId = s26, speciesId = sunProCut.id!!,
            bedId = bedAnnualB.id!!, firstSowDate = LocalDate.of(2026, 5, 1),
            intervalDays = 10, totalSuccessions = 6, seedsPerSuccession = 30,
            notes = "ProCut Orange — tight succession for wedding season",
        ))

        // ── Production Targets (2026) ──
        productionTargetRepository.persist(ProductionTarget(
            userId = userId, seasonId = s26, speciesId = cafeAuLait.id!!,
            stemsPerWeek = 150, startDate = LocalDate.of(2026, 7, 1), endDate = LocalDate.of(2026, 9, 30),
            notes = "Combined dahlia target across all 3 varieties",
        ))
        productionTargetRepository.persist(ProductionTarget(
            userId = userId, seasonId = s26, speciesId = zinniaGiant.id!!,
            stemsPerWeek = 100, startDate = LocalDate.of(2026, 6, 15), endDate = LocalDate.of(2026, 9, 15),
            notes = "Zinnia target — both varieties combined",
        ))
        productionTargetRepository.persist(ProductionTarget(
            userId = userId, seasonId = s26, speciesId = snapMadame.id!!,
            stemsPerWeek = 50, startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 8, 31),
            notes = "Snapdragon target — both varieties",
        ))

        // ── Scheduled Tasks (2026) ──
        scheduledTaskRepository.persist(ScheduledTask(
            userId = userId, speciesId = zinniaGiant.id!!, activityType = "SOW",
            deadline = LocalDate.of(2026, 5, 13), targetCount = 50, remainingCount = 50,
            seasonId = s26, successionScheduleId = succZinnia.id,
            notes = "Succession 3 of 4",
        ))
        scheduledTaskRepository.persist(ScheduledTask(
            userId = userId, speciesId = zinniaGiant.id!!, activityType = "SOW",
            deadline = LocalDate.of(2026, 5, 27), targetCount = 50, remainingCount = 50,
            seasonId = s26, successionScheduleId = succZinnia.id,
            notes = "Succession 4 of 4",
        ))
        scheduledTaskRepository.persist(ScheduledTask(
            userId = userId, speciesId = sunProCut.id!!, activityType = "SOW",
            deadline = LocalDate.of(2026, 5, 11), targetCount = 30, remainingCount = 30,
            seasonId = s26, successionScheduleId = succSunflower.id,
            notes = "Sunflower succession 2 of 6",
        ))
        scheduledTaskRepository.persist(ScheduledTask(
            userId = userId, speciesId = sunProCut.id!!, activityType = "SOW",
            deadline = LocalDate.of(2026, 5, 21), targetCount = 30, remainingCount = 30,
            seasonId = s26, successionScheduleId = succSunflower.id,
            notes = "Sunflower succession 3 of 6",
        ))
        scheduledTaskRepository.persist(ScheduledTask(
            userId = userId, speciesId = snapMadame.id!!, activityType = "PINCH",
            deadline = LocalDate.of(2026, 6, 5), targetCount = 68, remainingCount = 68,
            seasonId = s26, notes = "Pinch snapdragon main stems for branching",
        ))
        scheduledTaskRepository.persist(ScheduledTask(
            userId = userId, speciesId = cafeAuLait.id!!, activityType = "DISBUD",
            deadline = LocalDate.of(2026, 6, 15), targetCount = 28, remainingCount = 28,
            seasonId = s26, notes = "Disbud Cafe au Lait for larger blooms",
        ))

        // ── Variety Trials ──
        // 2024: dahlia varieties
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s24, speciesId = cafeAuLait.id!!,
            bedId = bedDahlia.id!!, plantCount = 20, stemYield = 200,
            avgStemLengthCm = 55, avgVaseLifeDays = 6, qualityScore = 9,
            customerReception = Reception.LOVED, verdict = Verdict.EXPAND,
            notes = "Huge demand, best seller. Expand significantly.",
        ))
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s24, speciesId = labyrinth.id!!,
            bedId = bedDahlia.id!!, plantCount = 15, stemYield = 170,
            avgStemLengthCm = 50, avgVaseLifeDays = 5, qualityScore = 8,
            customerReception = Reception.LIKED, verdict = Verdict.KEEP,
            notes = "Good variety, reliable producer. Keep at current level.",
        ))
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s24, speciesId = cornel.id!!,
            bedId = bedDahlia.id!!, plantCount = 12, stemYield = 130,
            avgStemLengthCm = 48, avgVaseLifeDays = 5, qualityScore = 7,
            customerReception = Reception.LIKED, verdict = Verdict.KEEP,
            notes = "Solid performer, great color. Compact growth habit.",
        ))
        // 2025: zinnias and cosmos
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s25, speciesId = zinniaGiant.id!!,
            bedId = bedAnnualA.id!!, plantCount = 100, stemYield = 300,
            avgStemLengthCm = 60, avgVaseLifeDays = 8, qualityScore = 9,
            customerReception = Reception.LOVED, verdict = Verdict.EXPAND,
            notes = "Workhorse variety. Big stems, great colors, long vase life.",
        ))
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s25, speciesId = zinniaQLO.id!!,
            bedId = bedAnnualA.id!!, plantCount = 40, stemYield = 150,
            avgStemLengthCm = 52, avgVaseLifeDays = 8, qualityScore = 8,
            customerReception = Reception.LOVED, verdict = Verdict.EXPAND,
            notes = "Unique color, florists love it. Slightly shorter stems.",
        ))
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s25, speciesId = cosmosDC.id!!,
            bedId = bedAnnualB.id!!, plantCount = 50, stemYield = 130,
            avgStemLengthCm = 58, avgVaseLifeDays = 6, qualityScore = 7,
            customerReception = Reception.LIKED, verdict = Verdict.KEEP,
            notes = "Nice in bouquets, slightly floppy stems. Good filler.",
        ))
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s25, speciesId = cosmosAL.id!!,
            bedId = bedAnnualB.id!!, plantCount = 35, stemYield = 70,
            avgStemLengthCm = 50, avgVaseLifeDays = 5, qualityScore = 6,
            customerReception = Reception.NEUTRAL, verdict = Verdict.REDUCE,
            notes = "Beautiful color but short vase life and low yield.",
        ))
        // 2026: ranunculus trial
        varietyTrialRepository.persist(VarietyTrial(
            userId = userId, seasonId = s26, speciesId = ranunculus.id!!,
            bedId = bedFoliage.id!!, plantCount = 50,
            verdict = Verdict.UNDECIDED,
            notes = "First trial. Planted from pre-sprouted corms. Monitoring establishment.",
        ))

        // ── Bouquet Recipes ──
        val sommardrom = bouquetRecipeRepository.persist(BouquetRecipe(
            userId = userId, name = "Sommardröm",
            description = "Summer Dream — lush seasonal bouquet with dahlias as focal flower",
            priceSek = 15000,
        ))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = sommardrom.id!!, speciesId = cafeAuLait.id!!, stemCount = 5, role = ItemRole.FLOWER))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = sommardrom.id!!, speciesId = zinniaGiant.id!!, stemCount = 3, role = ItemRole.FLOWER))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = sommardrom.id!!, speciesId = cosmosDC.id!!, stemCount = 5, role = ItemRole.FILLER))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = sommardrom.id!!, speciesId = eucalyptus.id!!, stemCount = 3, role = ItemRole.FOLIAGE))

        val solsken = bouquetRecipeRepository.persist(BouquetRecipe(
            userId = userId, name = "Solsken",
            description = "Sunshine — bright warm-toned bouquet for farmers market",
            priceSek = 12000,
        ))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = solsken.id!!, speciesId = sunProCut.id!!, stemCount = 3, role = ItemRole.FLOWER))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = solsken.id!!, speciesId = zinniaQLO.id!!, stemCount = 5, role = ItemRole.FLOWER))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = solsken.id!!, speciesId = snapMadame.id!!, stemCount = 3, role = ItemRole.ACCENT))

        val romantik = bouquetRecipeRepository.persist(BouquetRecipe(
            userId = userId, name = "Romantik",
            description = "Romance — elegant wedding bouquet with soft tones",
            priceSek = 18000,
        ))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = romantik.id!!, speciesId = lisEcho.id!!, stemCount = 3, role = ItemRole.FLOWER))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = romantik.id!!, speciesId = ranunculus.id!!, stemCount = 3, role = ItemRole.FLOWER))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = romantik.id!!, speciesId = sweetPea.id!!, stemCount = 5, role = ItemRole.ACCENT))
        bouquetRecipeRepository.persistItem(BouquetRecipeItem(recipeId = romantik.id!!, speciesId = eucalyptus.id!!, stemCount = 3, role = ItemRole.FOLIAGE))

        // ── Pest/Disease Logs ──
        pestDiseaseLogRepository.persist(PestDiseaseLog(
            userId = userId, seasonId = s24, bedId = bedSweet.id!!,
            speciesId = sweetPea.id!!, observedDate = LocalDate.of(2024, 6, 20),
            category = PestCategory.PEST, name = "Aphids",
            severity = Severity.MODERATE, treatment = "Insecticidal soap spray, 3 applications",
            outcome = Outcome.RESOLVED,
            notes = "Caught early. Concentrated on growing tips. Ladybugs helped after initial spray.",
        ))
        pestDiseaseLogRepository.persist(PestDiseaseLog(
            userId = userId, seasonId = s25, bedId = bedAnnualA.id!!,
            speciesId = zinniaGiant.id!!, observedDate = LocalDate.of(2025, 8, 5),
            category = PestCategory.DISEASE, name = "Powdery mildew",
            severity = Severity.HIGH, treatment = "Sulfur-based fungicide, improved air circulation",
            outcome = Outcome.RESOLVED,
            notes = "Late summer humidity triggered outbreak. Removed worst-affected leaves. Sulfur treatment effective.",
        ))
        pestDiseaseLogRepository.persist(PestDiseaseLog(
            userId = userId, seasonId = s25, bedId = bedLis.id!!,
            speciesId = lisEcho.id!!, observedDate = LocalDate.of(2025, 8, 20),
            category = PestCategory.DISEASE, name = "Botrytis (gray mold)",
            severity = Severity.MODERATE, treatment = "Improved ventilation, reduced watering frequency",
            outcome = Outcome.ONGOING,
            notes = "Tunnel humidity too high. Some bud drop. Need better ventilation system for 2026.",
        ))
        pestDiseaseLogRepository.persist(PestDiseaseLog(
            userId = userId, seasonId = s26, bedId = bedDahlia.id!!,
            speciesId = cafeAuLait.id!!, observedDate = LocalDate.of(2026, 5, 28),
            category = PestCategory.PEST, name = "Thrips",
            severity = Severity.LOW, outcome = Outcome.MONITORING,
            notes = "Low numbers found on new growth. Monitoring weekly. Will treat if numbers increase.",
        ))

        return Response.ok(SeedResult(
            speciesCount = allSpecies.size,
            groupCount = 3,
            tagCount = tags.size,
            gardenCount = 1,
            bedCount = beds.size,
            plantCount = counters.plants,
            eventCount = counters.events,
            seedInventoryCount = seedInvCount,
            taskCount = 6,
            seasonCount = 3,
            customerCount = customerIds.size,
            pestDiseaseLogCount = 4,
            varietyTrialCount = 9,
            bouquetRecipeCount = 3,
            successionScheduleCount = 2,
            productionTargetCount = 3,
        )).build()
    }

    private fun IntRange.toLong(): Long = this.random().toLong()
}
