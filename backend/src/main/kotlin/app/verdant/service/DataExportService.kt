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
    private val listingRepository: ListingRepository,
    private val marketOrderRepository: MarketOrderRepository,
    private val speciesRepository: SpeciesRepository,
    private val speciesGroupRepository: SpeciesGroupRepository,
    private val speciesTagRepository: SpeciesTagRepository,
    private val orderItemRepository: OrderItemRepository,
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
        val speciesNamesForTasks = speciesRepository.findNamesByIds(scheduledTasks.map { it.speciesId }.toSet())
        val scheduledTaskResponses = scheduledTasks.map { task ->
            ScheduledTaskResponse(
                id = task.id!!,
                speciesId = task.speciesId,
                speciesName = speciesNamesForTasks[task.speciesId] ?: "Unknown",
                activityType = task.activityType,
                deadline = task.deadline,
                targetCount = task.targetCount,
                remainingCount = task.remainingCount,
                status = task.status.name,
                notes = task.notes,
                seasonId = task.seasonId,
                successionScheduleId = task.successionScheduleId,
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

        val listings = listingRepository.findByOrgId(orgId, limit = Int.MAX_VALUE)
        val speciesNamesForListings = speciesRepository.findNamesByIds(listings.map { it.speciesId }.toSet())
        val sellerNames = userRepository.findByIds(setOf(userId))
        val sellerName = sellerNames[userId]?.displayName ?: "Unknown"
        val listingResponses = listings.map { listing ->
            val species = speciesRepository.findById(listing.speciesId)
            ListingResponse(
                id = listing.id!!,
                sellerName = sellerName,
                producerName = sellerName,
                speciesId = listing.speciesId,
                speciesName = speciesNamesForListings[listing.speciesId] ?: "Unknown",
                speciesNameSv = species?.commonNameSv,
                title = listing.title,
                description = listing.description,
                quantityAvailable = listing.quantityAvailable,
                pricePerStemSek = listing.pricePerStemSek,
                availableFrom = listing.availableFrom,
                availableUntil = listing.availableUntil,
                imageUrl = listing.imageUrl,
                isActive = listing.isActive,
                createdAt = listing.createdAt,
            )
        }

        // Market orders: include both as purchaser and producer
        val purchasedOrders = marketOrderRepository.findByPurchaserOrgId(orgId, limit = Int.MAX_VALUE)
        val producedOrders = marketOrderRepository.findByProducerOrgId(orgId, limit = Int.MAX_VALUE)
        val allOrderIds = (purchasedOrders.mapNotNull { it.id } + producedOrders.mapNotNull { it.id }).toSet()
        val allOrders = (purchasedOrders + producedOrders).distinctBy { it.id }
        val allParticipantIds = allOrders.flatMap { listOf(it.purchaserOrgId, it.producerOrgId) }.toSet()
        val participantUsers = userRepository.findByIds(allParticipantIds)
        val allOrderItems = allOrderIds.flatMap { orderId -> orderItemRepository.findByOrderId(orderId) }
        val itemsByOrder = allOrderItems.groupBy { it.orderId }
        val marketOrderResponses = allOrders.map { order ->
            val items = itemsByOrder[order.id] ?: emptyList()
            MarketOrderResponse(
                id = order.id!!,
                purchaserId = order.purchaserOrgId,
                purchaserName = participantUsers[order.purchaserOrgId]?.displayName ?: "Unknown",
                producerId = order.producerOrgId,
                producerName = participantUsers[order.producerOrgId]?.displayName ?: "Unknown",
                status = order.status.name,
                deliveryDate = order.deliveryDate,
                totalSek = order.totalSek,
                notes = order.notes,
                items = items.map { item ->
                    OrderItemResponse(
                        id = item.id!!,
                        listingId = item.listingId,
                        speciesId = item.speciesId,
                        speciesName = item.speciesName,
                        quantity = item.quantity,
                        pricePerStemSek = item.pricePerStemSek,
                    )
                },
                createdAt = order.createdAt,
                updatedAt = order.updatedAt,
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
            marketListings = listingResponses,
            marketOrders = marketOrderResponses,
            exportedAt = Instant.now(),
        )
    }
}
