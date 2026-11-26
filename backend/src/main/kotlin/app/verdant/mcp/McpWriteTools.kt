package app.verdant.mcp

import app.verdant.dto.*
import app.verdant.entity.Outcome
import app.verdant.entity.PestCategory
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantOrigin
import app.verdant.entity.PlantStatus
import app.verdant.entity.Severity
import app.verdant.entity.SupplyApplicationScope
import app.verdant.entity.UnitType
import app.verdant.mcp.McpSchema.bool
import app.verdant.mcp.McpSchema.count
import app.verdant.mcp.McpSchema.date
import app.verdant.mcp.McpSchema.id
import app.verdant.mcp.McpSchema.number
import app.verdant.mcp.McpSchema.oneOf
import app.verdant.mcp.McpSchema.text
import app.verdant.service.*
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Validator
import java.time.LocalDate

/**
 * Tools that change the connected organization's garden records. Each delegates to the same service method as the
 * corresponding REST endpoint, so validation and organization checks are shared. Nothing here deletes records.
 */
@ApplicationScoped
class McpWriteTools(
    private val mapper: ObjectMapper,
    private val validator: Validator,
    private val plants: PlantService,
    private val tasks: ScheduledTaskService,
    private val seeds: SeedInventoryService,
    private val pests: PestDiseaseService,
    private val supplies: SupplyApplicationService,
) {
    private val taskActivities = listOf("SOW", "SOAK", "POT_UP", "PLANT", "HARVEST", "RECOVER", "DISCARD", "WATER", "WEED", "FERTILIZE", "TODO")
    private val groupEvents = listOf(PlantEventType.POTTED_UP, PlantEventType.PLANTED_OUT, PlantEventType.HARVESTED, PlantEventType.RECOVERED, PlantEventType.REMOVED).map { it.name }

    private fun definition(name: String, description: String, properties: Map<String, Any>, required: List<String>, idempotent: Boolean = false): Map<String, Any> = mapOf(
        "name" to name,
        "description" to "$description Changes Verdant Planner records; confirm intent with the user first and check with a read tool before retrying after an unclear failure.",
        "inputSchema" to mapOf("type" to "object", "properties" to properties, "required" to required, "additionalProperties" to false),
        "annotations" to mapOf("readOnlyHint" to false, "destructiveHint" to false, "idempotentHint" to idempotent, "openWorldHint" to false),
        "securitySchemes" to listOf(mapOf("type" to "oauth2", "scopes" to listOf(McpConfig.READ_SCOPE, McpConfig.WRITE_SCOPE))),
    )

    fun definitions() = listOf(
        definition("sow_seeds", "Record a sowing: creates seedCount plants of a species, in a tray location or a bed (not both; neither for an unplaced sowing). Optionally draws the seeds from a seed inventory batch.", mapOf(
            "speciesId" to id(), "name" to text(255, "Label for the sowing, usually the species name"), "seedCount" to count(),
            "trayLocationId" to id("From list_tray_locations"), "bedId" to id("From list_beds"),
            "plantedDate" to date("Sowing date when not today"), "seedBatchId" to id("Seed inventory ID to draw seeds from (list_seed_inventory)"),
            "allowShortfall" to bool("Sow more seeds than the batch holds and empty it"), "notes" to text(2000),
        ), listOf("speciesId", "name", "seedCount")),
        definition("record_group_event", "Advance plants listed by list_plant_groups: pot up, plant out (set targetBedId), mark harvested (finished), recovered or removed. Selects up to count plants that match speciesId, current status and, when given, bedId and plantedDate. To record a harvest yield with stem counts use record_plant_event.", mapOf(
            "speciesId" to id(), "status" to oneOf<PlantStatus>("Current status of the group"), "eventType" to oneOf(groupEvents),
            "count" to count(), "bedId" to id("Current bed of the group"), "plantedDate" to date("Planted date of the group"),
            "targetBedId" to id("Bed the plants move into, typically for PLANTED_OUT"), "notes" to text(2000),
        ), listOf("speciesId", "status", "eventType", "count")),
        definition("record_plant_event", "Record an event on one plant (IDs from list_bed_plants): harvest with stem counts, bloom stages, pinching, notes and similar. Some events update the plant status (e.g. HARVESTED marks it harvested, PLANTED_OUT marks it growing).", mapOf(
            "plantId" to id(), "eventType" to oneOf<PlantEventType>(exclude = setOf(PlantEventType.SEEDED, PlantEventType.APPLIED_SUPPLY)),
            "eventDate" to date("Defaults to today"), "plantCount" to count(minimum = 0), "stemCount" to count("Stems cut, for HARVESTED", minimum = 0),
            "stemLengthCm" to count(minimum = 0), "qualityGrade" to text(255), "vaseLifeDays" to count(minimum = 0),
            "weightGrams" to number(), "quantity" to count(minimum = 0), "notes" to text(2000),
        ), listOf("plantId", "eventType")),
        definition("water_bed", "Record that a bed was watered; logs a watering event on its active plants.", mapOf("bedId" to id()), listOf("bedId")),
        definition("weed_bed", "Record that a bed was weeded; logs a weeding event on its active plants.", mapOf("bedId" to id()), listOf("bedId")),
        definition("move_tray_plants", "Move up to count tray plants of one species and status between tray locations (omit an ID for unplaced trays).", mapOf(
            "speciesId" to id(), "status" to oneOf<PlantStatus>(), "count" to count(),
            "fromTrayLocationId" to id(), "toTrayLocationId" to id(),
        ), listOf("speciesId", "status", "count")),
        definition("complete_task", "Mark processedCount units of a scheduled task as done (IDs from list_tasks). speciesId is required for species tasks and omitted for bed tasks (WATER, WEED, FERTILIZE). The task completes when nothing remains.", mapOf(
            "taskId" to id(), "processedCount" to count(), "speciesId" to id(),
        ), listOf("taskId", "processedCount")),
        definition("create_task", "Schedule a task. Non-TODO tasks need deadline and targetCount. Bed tasks (WATER, WEED, FERTILIZE) take bedId and no species. TODO tasks take only notes, dates and seasonId.", mapOf(
            "activityType" to oneOf(taskActivities), "speciesId" to id(), "speciesGroupId" to id(), "bedId" to id(),
            "earliestDate" to date(), "deadline" to date(), "targetCount" to count(), "notes" to text(2000), "seasonId" to id(),
        ), listOf("activityType")),
        definition("update_task", "Change a scheduled task's dates, target, notes, species or season. Omitted fields keep their values.", mapOf(
            "taskId" to id(), "activityType" to oneOf(taskActivities), "speciesId" to id(), "earliestDate" to date(), "deadline" to date(),
            "targetCount" to count(), "notes" to text(2000), "seasonId" to id(),
        ), listOf("taskId"), idempotent = true),
        definition("add_seed_inventory", "Add a seed batch to inventory, for example after buying or collecting seed.", mapOf(
            "speciesId" to id(), "quantity" to count(minimum = 0), "unitType" to oneOf<UnitType>("Unit of quantity"),
            "collectionDate" to date(), "expirationDate" to date(), "costPerUnitCents" to count(minimum = 0), "seasonId" to id(),
            "seedsPerPackage" to count(), "providerId" to id("Supplier"),
        ), listOf("speciesId", "quantity", "unitType")),
        definition("update_seed_inventory", "Correct a seed batch, e.g. its counted quantity or expiry date. Omitted fields keep their values.", mapOf(
            "inventoryId" to id(), "quantity" to count(minimum = 0), "unitType" to oneOf<UnitType>(), "collectionDate" to date(), "expirationDate" to date(),
            "costPerUnitCents" to count(minimum = 0), "seasonId" to id(), "seedsPerPackage" to count(), "providerId" to id(),
        ), listOf("inventoryId"), idempotent = true),
        definition("log_pest_disease", "Record a pest, disease or deficiency observation.", mapOf(
            "category" to oneOf<PestCategory>(), "name" to text(255), "seasonId" to id(), "bedId" to id(), "speciesId" to id(),
            "observedDate" to date("Defaults to today"), "severity" to oneOf<Severity>(), "treatment" to text(2000),
            "outcome" to oneOf<Outcome>(), "notes" to text(2000),
        ), listOf("category", "name")),
        definition("apply_supply", "Record fertilizer or another supply applied to a whole bed (targetScope BED) or to a tray location (targetScope PLANTS). Give exactly one of bedId or trayLocationId, and a supplyInventoryId (list_supplies) or supplyTypeId. Inventory is drawn down.", mapOf(
            "bedId" to id(), "trayLocationId" to id(), "supplyInventoryId" to id(), "supplyTypeId" to id(),
            "quantity" to number("In the supply type's unit"), "targetScope" to oneOf<SupplyApplicationScope>(), "notes" to text(2000),
        ), listOf("quantity", "targetScope")),
    )

    val names: Set<String> by lazy { definitions().map { it["name"] as String }.toSet() }

    fun call(orgId: Long, userId: Long, name: String, args: JsonNode): Any {
        fun id(key: String) = args.path(key).longValue()
        return when (name) {
            "sow_seeds" -> plants.batchSow(dto<BatchSowRequest>(args, defaults = mapOf("origin" to PlantOrigin.SOWN.name)), orgId)
            "record_group_event" -> plants.batchEvent(dto<BatchEventRequest>(args), orgId)
            "record_plant_event" -> plants.addEvent(id("plantId"), dto<CreatePlantEventRequest>(args, "plantId", defaults = mapOf("eventDate" to today())), orgId)
            "water_bed" -> plants.waterBed(id("bedId"), orgId)
            "weed_bed" -> plants.weedBed(id("bedId"), orgId)
            "move_tray_plants" -> plants.moveTrayPlants(orgId, dto<MoveTrayPlantsRequest>(args))
            "complete_task" -> dto<CompleteTaskPartiallyRequest>(args, "taskId").let { tasks.completePartially(id("taskId"), it.speciesId, it.processedCount, orgId) }
            "create_task" -> tasks.createTask(dto<CreateScheduledTaskRequest>(args), orgId)
            "update_task" -> tasks.updateTask(id("taskId"), dto<UpdateScheduledTaskRequest>(args, "taskId"), orgId)
            "add_seed_inventory" -> seeds.createInventory(dto<CreateSeedInventoryRequest>(args), orgId)
            "update_seed_inventory" -> seeds.updateInventory(id("inventoryId"), dto<UpdateSeedInventoryRequest>(args, "inventoryId"), orgId)
            "log_pest_disease" -> pests.createLog(dto<CreatePestDiseaseLogRequest>(args, defaults = mapOf("observedDate" to today(), "severity" to Severity.MODERATE.name)), orgId)
            "apply_supply" -> supplies.create(dto<CreateSupplyApplicationRequest>(args, defaults = mapOf("plantIds" to emptyList<Long>())), orgId, userId)
            else -> throw IllegalArgumentException("Unknown tool")
        }
    }

    private fun today() = LocalDate.now().toString()

    /**
     * Arguments (minus [exclude]) as the REST request body, with the same bean validation the REST layer applies.
     * Jackson here does not apply Kotlin default values, so non-null DTO fields with defaults are supplied via [defaults].
     */
    private inline fun <reified T : Any> dto(args: JsonNode, vararg exclude: String, defaults: Map<String, Any> = emptyMap()): T {
        val body = (args.deepCopy<JsonNode>() as ObjectNode).apply {
            remove(exclude.toList())
            defaults.forEach { (key, value) -> if (!has(key)) set<JsonNode>(key, mapper.valueToTree(value)) }
        }
        val value = try {
            mapper.treeToValue(body, T::class.java)
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException("Invalid arguments")
        }
        val violations = validator.validate(value)
        require(violations.isEmpty()) { violations.joinToString("; ") { "${it.propertyPath} ${it.message}" } }
        return value
    }
}
