package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class PlantResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("speciesId") val speciesId: Long?,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("plantedDate") val plantedDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("seedCount") val seedCount: Int?,
    @SerializedName("survivingCount") val survivingCount: Int?,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CreatePlantRequest(
    @SerializedName("name") val name: String,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("plantedDate") val plantedDate: String? = null,
    @SerializedName("status") val status: String = "SEEDED",
    @SerializedName("seedCount") val seedCount: Int? = null,
    @SerializedName("survivingCount") val survivingCount: Int? = null,
)

data class BatchSowRequest(
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("trayLocationId") val trayLocationId: Long? = null,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("seedCount") val seedCount: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
    @SerializedName("plantedDate") val plantedDate: String? = null,
)

data class BatchSowResponse(
    @SerializedName("plantIds") val plantIds: List<Long>,
    @SerializedName("count") val count: Int,
)

data class TraySummaryEntry(
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int,
    @SerializedName("trayLocationId") val trayLocationId: Long? = null,
    @SerializedName("trayLocationName") val trayLocationName: String? = null,
)

data class TrayLocationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("activePlantCount") val activePlantCount: Int,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateTrayLocationRequest(
    @SerializedName("name") val name: String,
)

data class UpdateTrayLocationRequest(
    @SerializedName("name") val name: String? = null,
)

data class BulkLocationActionResponse(
    @SerializedName("plantsAffected") val plantsAffected: Int,
)

data class BulkLocationNoteRequest(
    @SerializedName("text") val text: String,
)

data class MoveTrayLocationRequest(
    @SerializedName("targetLocationId") val targetLocationId: Long? = null,
    @SerializedName("count") val count: Int = -1,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("status") val status: String? = null,
)

data class MoveTrayPlantsRequest(
    @SerializedName("fromTrayLocationId") val fromTrayLocationId: Long? = null,
    @SerializedName("toTrayLocationId") val toTrayLocationId: Long? = null,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int,
)

data class PlantGroupResponse(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("gardenName") val gardenName: String?,
    @SerializedName("plantedDate") val plantedDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int,
)

data class BatchEventRequest(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("plantedDate") val plantedDate: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("count") val count: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
    @SerializedName("targetBedId") val targetBedId: Long? = null,
)

data class BatchEventResponse(
    @SerializedName("updatedCount") val updatedCount: Int,
)

data class UpdatePlantRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("plantedDate") val plantedDate: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("seedCount") val seedCount: Int? = null,
    @SerializedName("survivingCount") val survivingCount: Int? = null,
)

// ── Plant Events ──

data class PlantEventResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("plantId") val plantId: Long,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("plantCount") val plantCount: Int?,
    @SerializedName("weightGrams") val weightGrams: Double?,
    @SerializedName("quantity") val quantity: Int?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("aiSuggestions") val aiSuggestions: String?,
    @SerializedName("stemCount") val stemCount: Int? = null,
    @SerializedName("stemLengthCm") val stemLengthCm: Int? = null,
    @SerializedName("qualityGrade") val qualityGrade: String? = null,
    @SerializedName("vaseLifeDays") val vaseLifeDays: Int? = null,
    @SerializedName("supplyApplicationId") val supplyApplicationId: Long? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreatePlantEventRequest(
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String? = null,
    @SerializedName("plantCount") val plantCount: Int? = null,
    @SerializedName("weightGrams") val weightGrams: Double? = null,
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
    @SerializedName("aiSuggestions") val aiSuggestions: String? = null,
    @SerializedName("stemCount") val stemCount: Int? = null,
    @SerializedName("stemLengthCm") val stemLengthCm: Int? = null,
    @SerializedName("qualityGrade") val qualityGrade: String? = null,
    @SerializedName("vaseLifeDays") val vaseLifeDays: Int? = null,
)

data class IdentifyPlantRequest(
    @SerializedName("imageBase64") val imageBase64: String,
)

data class ExtractSpeciesInfoRequest(
    @SerializedName("imageBase64") val imageBase64: String,
)

data class ExtractedSpeciesInfo(
    @SerializedName("commonName") val commonName: String? = null,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("variantNameSv") val variantNameSv: String? = null,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("germinationTimeDaysMin") val germinationTimeDaysMin: Int? = null,
    @SerializedName("germinationTimeDaysMax") val germinationTimeDaysMax: Int? = null,
    @SerializedName("sowingDepthMm") val sowingDepthMm: Int? = null,
    @SerializedName("heightCmMin") val heightCmMin: Int? = null,
    @SerializedName("heightCmMax") val heightCmMax: Int? = null,
    @SerializedName("bloomMonths") val bloomMonths: List<Int>? = null,
    @SerializedName("sowingMonths") val sowingMonths: List<Int>? = null,
    @SerializedName("germinationRate") val germinationRate: Int? = null,
    @SerializedName("growingPositions") val growingPositions: List<String>? = null,
    @SerializedName("soils") val soils: List<String>? = null,
    @SerializedName("daysToHarvestMin") val daysToHarvestMin: Int? = null,
    @SerializedName("daysToHarvestMax") val daysToHarvestMax: Int? = null,
    @SerializedName("cropBox") val cropBox: CropBox? = null,
)

data class PlantSuggestion(
    @SerializedName("species") val species: String,
    @SerializedName("commonName") val commonName: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("cropBox") val cropBox: CropBox? = null,
)

data class CropBox(
    @SerializedName("x") val x: Double,
    @SerializedName("y") val y: Double,
    @SerializedName("width") val width: Double,
    @SerializedName("height") val height: Double,
)

data class HarvestStatRow(
    @SerializedName("species") val species: String,
    @SerializedName("totalWeightGrams") val totalWeightGrams: Double,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("harvestCount") val harvestCount: Int,
    @SerializedName("totalStems") val totalStems: Int = 0,
)

// ── Species Plant Summary ──

data class SpeciesPlantSummary(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("variantName") val variantName: String? = null,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("activePlantCount") val activePlantCount: Int,
    @SerializedName("totalPlantCount") val totalPlantCount: Int,
)

data class SpeciesEventSummaryEntry(
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("currentStatus") val currentStatus: String? = null,
    @SerializedName("count") val count: Int,
    @SerializedName("fromLocationName") val fromLocationName: String? = null,
    @SerializedName("toLocationName") val toLocationName: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateSpeciesEventDateRequest(
    @SerializedName("eventType") val eventType: String,
    @SerializedName("oldDate") val oldDate: String,
    @SerializedName("newDate") val newDate: String,
    @SerializedName("currentStatus") val currentStatus: String? = null,
    @SerializedName("trayOnly") val trayOnly: Boolean = false,
)

data class UpdateSpeciesEventDateResponse(
    @SerializedName("updated") val updated: Int,
)

data class DeleteSpeciesEventRequest(
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("count") val count: Int,
    @SerializedName("currentStatus") val currentStatus: String? = null,
    @SerializedName("trayOnly") val trayOnly: Boolean = false,
)

data class DeleteSpeciesEventResponse(
    @SerializedName("eventsDeleted") val eventsDeleted: Int,
    @SerializedName("plantsRemoved") val plantsRemoved: Int,
)

data class PlantLocationGroup(
    @SerializedName("gardenName") val gardenName: String?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("trayLocationId") val trayLocationId: Long? = null,
    @SerializedName("trayLocationName") val trayLocationName: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("count") val count: Int,
    @SerializedName("year") val year: Int,
)
