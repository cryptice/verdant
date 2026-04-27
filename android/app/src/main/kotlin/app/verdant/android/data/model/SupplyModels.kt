package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Supplies ──

data class SupplyTypeResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("properties") val properties: Map<String, Any?>,
    @SerializedName("inexhaustible") val inexhaustible: Boolean = false,
    @SerializedName("createdAt") val createdAt: String,
)

data class SupplyInventoryResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("supplyTypeId") val supplyTypeId: Long,
    @SerializedName("supplyTypeName") val supplyTypeName: String,
    @SerializedName("category") val category: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("properties") val properties: Map<String, Any?>,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("costCents") val costCents: Int?,
    @SerializedName("seasonId") val seasonId: Long?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class DecrementSupplyRequest(
    @SerializedName("quantity") val quantity: Double,
)

data class BedEventResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("bedId") val bedId: Long,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("plantsAffected") val plantsAffected: Int? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateSupplyTypeRequest(
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("properties") val properties: Map<String, Any?> = emptyMap(),
    @SerializedName("inexhaustible") val inexhaustible: Boolean = false,
)

data class UpdateSupplyTypeRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("properties") val properties: Map<String, Any?>? = null,
    @SerializedName("inexhaustible") val inexhaustible: Boolean? = null,
)

data class CreateSupplyInventoryRequest(
    @SerializedName("supplyTypeId") val supplyTypeId: Long,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("costCents") val costCents: Int? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class SupplyApplicationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("trayLocationId") val trayLocationId: Long? = null,
    @SerializedName("supplyInventoryId") val supplyInventoryId: Long?,
    @SerializedName("supplyTypeId") val supplyTypeId: Long,
    @SerializedName("supplyTypeName") val supplyTypeName: String,
    @SerializedName("supplyUnit") val supplyUnit: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("targetScope") val targetScope: String,
    @SerializedName("appliedAt") val appliedAt: String,
    @SerializedName("appliedByName") val appliedByName: String?,
    @SerializedName("workflowStepId") val workflowStepId: Long?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("plantIds") val plantIds: List<Long>,
)

data class CreateSupplyApplicationRequest(
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("trayLocationId") val trayLocationId: Long? = null,
    @SerializedName("supplyInventoryId") val supplyInventoryId: Long? = null,
    @SerializedName("supplyTypeId") val supplyTypeId: Long? = null,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("targetScope") val targetScope: String,
    @SerializedName("plantIds") val plantIds: List<Long>? = null,
    @SerializedName("workflowStepId") val workflowStepId: Long? = null,
    @SerializedName("notes") val notes: String? = null,
)

object SupplyApplicationScope {
    const val BED = "BED"
    const val PLANTS = "PLANTS"
    val values = listOf(BED, PLANTS)
}
