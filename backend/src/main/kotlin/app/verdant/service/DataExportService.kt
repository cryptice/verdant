package app.verdant.service

import app.verdant.dto.*
import app.verdant.repository.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import java.time.Instant

@ApplicationScoped
class DataExportService(
    private val userRepository: UserRepository,
    private val gardenRepository: GardenRepository,
    private val bedRepository: BedRepository,
    private val plantRepository: PlantRepository,
    private val plantEventRepository: PlantEventRepository,
    private val speciesService: SpeciesService,
    private val seedInventoryRepository: SeedInventoryRepository,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val seasonRepository: SeasonRepository,
    private val customerRepository: CustomerRepository,
    private val pestDiseaseLogRepository: PestDiseaseLogRepository,
    private val varietyTrialRepository: VarietyTrialRepository,
    private val bouquetRecipeRepository: BouquetRecipeRepository,
    private val successionScheduleRepository: SuccessionScheduleRepository,
    private val productionTargetRepository: ProductionTargetRepository,
    private val speciesRepository: SpeciesRepository,
    private val speciesGroupRepository: SpeciesGroupRepository,
    private val speciesTagRepository: SpeciesTagRepository,
) {
    fun exportUserData(orgId: Long, userId: Long): UserDataExport {
        val user = userRepository.findById(userId) ?: throw NotFoundException("User not found")

        val gardens = gardenRepository.findByOrgId(orgId)
        val gardenIds = gardens.mapNotNull { it.id }.toSet()

        val allBeds = gardenIds.flatMap { bedRepository.findByGardenId(it) }

        // Plants — fetch without pagination limit for export
        val plants = plantRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val plantIds = plants.mapNotNull { it.id }
        val speciesNamesForPlants = speciesRepository.findNamesByIds(plants.mapNotNull { it.speciesId }.toSet())
        val plantResponses = plants.map { plant ->
            PlantResponse(
                id = plant.id!!,
                name = plant.name,
                speciesId = plant.speciesId,
                speciesName = plant.speciesId?.let { speciesNamesForPlants[it] },
                plantedDate = plant.plantedDate,
                status = plant.status,
                seedCount = plant.seedCount,
                survivingCount = plant.survivingCount,
                bedId = plant.bedId,
                seasonId = plant.seasonId,
                createdAt = plant.createdAt,
                updatedAt = plant.updatedAt,
            )
        }

        val plantEvents = plantIds.flatMap { plantEventRepository.findByPlantId(it) }
            .map { it.toResponse() }

        val species = speciesService.getSpeciesForUser(orgId, limit = Int.MAX_VALUE)

        val seedInventoryItems = seedInventoryRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val speciesNamesForSeeds = speciesRepository.findNamesByIds(seedInventoryItems.map { it.speciesId }.toSet())
        val seedInventoryResponses = seedInventoryItems.map { item ->
            SeedInventoryResponse(
                id = item.id!!,
                speciesId = item.speciesId,
                speciesName = speciesNamesForSeeds[item.speciesId] ?: "Unknown",
                quantity = item.quantity,
                collectionDate = item.collectionDate,
                expirationDate = item.expirationDate,
                costPerUnitSek = item.costPerUnitSek,
                unitType = item.unitType.name,
                seasonId = item.seasonId,
                speciesProviderId = item.speciesProviderId,
                providerName = null,
                createdAt = item.createdAt,
            )
        }

        val scheduledTasks = scheduledTaskRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val taskIds = scheduledTasks.map { it.id!! }.toSet()
        val acceptableByTask = scheduledTaskRepository.findAcceptableSpeciesIdsByTaskIds(taskIds)
        val allSpeciesIdsForTasks = acceptableByTask.values.flatten().toSet() +
            scheduledTasks.mapNotNull { it.speciesId }.toSet()
        val speciesNamesForTasks = speciesRepository.findNamesByIds(allSpeciesIdsForTasks)
        val speciesByIdForTasks = speciesRepository.findByIds(allSpeciesIdsForTasks)

        val groupIds = scheduledTasks.mapNotNull { it.originGroupId }.toSet()
        val groupNames = speciesGroupRepository.findNamesByIds(groupIds)

        val scheduledTaskResponses = scheduledTasks.map { task ->
            val myAcceptable = acceptableByTask[task.id] ?: emptyList()
            ScheduledTaskResponse(
                id = task.id!!,
                speciesId = task.speciesId,
                speciesName = task.speciesId?.let { speciesNamesForTasks[it] },
                activityType = task.activityType,
                deadline = task.deadline,
                targetCount = task.targetCount,
                remainingCount = task.remainingCount,
                status = task.status.name,
                notes = task.notes,
                seasonId = task.seasonId,
                successionScheduleId = task.successionScheduleId,
                originGroupId = task.originGroupId,
                originGroupName = task.originGroupId?.let { groupNames[it] },
                acceptableSpecies = myAcceptable.map { sid ->
                    val sp = speciesByIdForTasks[sid]
                    AcceptableSpeciesEntry(
                        speciesId = sid,
                        speciesName = speciesNamesForTasks[sid] ?: "Unknown",
                        commonName = sp?.commonName ?: "Unknown",
                        variantName = sp?.variantName,
                        commonNameSv = sp?.commonNameSv,
                        variantNameSv = sp?.variantNameSv,
                    )
                },
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
            )
        }

        val seasons = seasonRepository.findByOrgId(orgId).map { season ->
            SeasonResponse(
                id = season.id!!,
                name = season.name,
                year = season.year,
                startDate = season.startDate,
                endDate = season.endDate,
                lastFrostDate = season.lastFrostDate,
                firstFrostDate = season.firstFrostDate,
                growingDegreeBaseC = season.growingDegreeBaseC,
                notes = season.notes,
                isActive = season.isActive,
                createdAt = season.createdAt,
                updatedAt = season.updatedAt,
            )
        }

        val customers = customerRepository.findByOrgId(orgId, limit = Int.MAX_VALUE).map { customer ->
            CustomerResponse(
                id = customer.id!!,
                name = customer.name,
                channel = customer.channel,
                contactInfo = customer.contactInfo,
                notes = customer.notes,
                createdAt = customer.createdAt,
            )
        }

        val pestDiseaseLogs = pestDiseaseLogRepository.findByOrgId(orgId, limit = Int.MAX_VALUE).map { log ->
            PestDiseaseLogResponse(
                id = log.id!!,
                seasonId = log.seasonId,
                bedId = log.bedId,
                speciesId = log.speciesId,
                observedDate = log.observedDate,
                category = log.category,
                name = log.name,
                severity = log.severity,
                treatment = log.treatment,
                outcome = log.outcome,
                notes = log.notes,
                imageUrl = log.imageUrl,
                createdAt = log.createdAt,
            )
        }

        val varietyTrials = varietyTrialRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val speciesNamesForTrials = speciesRepository.findNamesByIds(varietyTrials.map { it.speciesId }.toSet())
        val varietyTrialResponses = varietyTrials.map { trial ->
            VarietyTrialResponse(
                id = trial.id!!,
                seasonId = trial.seasonId,
                speciesId = trial.speciesId,
                speciesName = speciesNamesForTrials[trial.speciesId],
                bedId = trial.bedId,
                plantCount = trial.plantCount,
                stemYield = trial.stemYield,
                avgStemLengthCm = trial.avgStemLengthCm,
                avgVaseLifeDays = trial.avgVaseLifeDays,
                qualityScore = trial.qualityScore,
                customerReception = trial.customerReception,
                verdict = trial.verdict,
                notes = trial.notes,
                createdAt = trial.createdAt,
            )
        }

        val bouquetRecipes = bouquetRecipeRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val allRecipeItems = bouquetRecipes.mapNotNull { it.id }.flatMap { recipeId ->
            bouquetRecipeRepository.findItemsByRecipeId(recipeId)
        }
        val speciesNamesForRecipes = speciesRepository.findNamesByIds(allRecipeItems.map { it.speciesId }.toSet())
        val itemsByRecipe = allRecipeItems.groupBy { it.recipeId }
        val bouquetRecipeResponses = bouquetRecipes.map { recipe ->
            val items = itemsByRecipe[recipe.id] ?: emptyList()
            BouquetRecipeResponse(
                id = recipe.id!!,
                name = recipe.name,
                description = recipe.description,
                imageUrl = recipe.imageUrl,
                priceSek = recipe.priceSek,
                items = items.map { item ->
                    BouquetRecipeItemResponse(
                        id = item.id!!,
                        speciesId = item.speciesId,
                        speciesName = speciesNamesForRecipes[item.speciesId],
                        stemCount = item.stemCount,
                        role = item.role,
                        notes = item.notes,
                    )
                },
                createdAt = recipe.createdAt,
                updatedAt = recipe.updatedAt,
            )
        }

        val successionSchedules = successionScheduleRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val speciesNamesForSchedules = speciesRepository.findNamesByIds(successionSchedules.map { it.speciesId }.toSet())
        val successionScheduleResponses = successionSchedules.map { schedule ->
            SuccessionScheduleResponse(
                id = schedule.id!!,
                seasonId = schedule.seasonId,
                speciesId = schedule.speciesId,
                speciesName = speciesNamesForSchedules[schedule.speciesId],
                bedId = schedule.bedId,
                firstSowDate = schedule.firstSowDate,
                intervalDays = schedule.intervalDays,
                totalSuccessions = schedule.totalSuccessions,
                seedsPerSuccession = schedule.seedsPerSuccession,
                notes = schedule.notes,
                createdAt = schedule.createdAt,
            )
        }

        val productionTargets = productionTargetRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val speciesNamesForTargets = speciesRepository.findNamesByIds(productionTargets.map { it.speciesId }.toSet())
        val productionTargetResponses = productionTargets.map { target ->
            ProductionTargetResponse(
                id = target.id!!,
                seasonId = target.seasonId,
                speciesId = target.speciesId,
                speciesName = speciesNamesForTargets[target.speciesId],
                stemsPerWeek = target.stemsPerWeek,
                startDate = target.startDate,
                endDate = target.endDate,
                notes = target.notes,
                createdAt = target.createdAt,
            )
        }

        return UserDataExport(
            user = user.toResponse(),
            gardens = gardens.map { garden ->
                GardenResponse(
                    id = garden.id!!,
                    name = garden.name,
                    description = garden.description,
                    emoji = garden.emoji,
                    latitude = garden.latitude,
                    longitude = garden.longitude,
                    address = garden.address,
                    boundaryJson = garden.boundaryJson,
                    createdAt = garden.createdAt,
                    updatedAt = garden.updatedAt,
                )
            },
            beds = allBeds.map { bed ->
                BedResponse(
                    id = bed.id!!,
                    name = bed.name,
                    description = bed.description,
                    gardenId = bed.gardenId,
                    boundaryJson = bed.boundaryJson,
                    lengthMeters = bed.lengthMeters,
                    widthMeters = bed.widthMeters,
                    createdAt = bed.createdAt,
                    updatedAt = bed.updatedAt,
                )
            },
            plants = plantResponses,
            plantEvents = plantEvents,
            species = species,
            seedInventory = seedInventoryResponses,
            scheduledTasks = scheduledTaskResponses,
            seasons = seasons,
            customers = customers,
            pestDiseaseLogs = pestDiseaseLogs,
            varietyTrials = varietyTrialResponses,
            bouquetRecipes = bouquetRecipeResponses,
            successionSchedules = successionScheduleResponses,
            productionTargets = productionTargetResponses,
            exportedAt = Instant.now(),
        )
    }
}
