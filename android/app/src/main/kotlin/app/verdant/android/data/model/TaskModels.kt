package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Scheduled Tasks ──

data class ScheduledTaskResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("speciesId") val speciesId: Long?,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("bedName") val bedName: String? = null,
    @SerializedName("gardenName") val gardenName: String? = null,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("deadline") val deadline: String,
    @SerializedName("targetCount") val targetCount: Int,
    @SerializedName("remainingCount") val remainingCount: Int,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("successionScheduleId") val successionScheduleId: Long? = null,
    @SerializedName("originGroupId") val originGroupId: Long? = null,
    @SerializedName("originGroupName") val originGroupName: String? = null,
    @SerializedName("acceptableSpecies") val acceptableSpecies: List<AcceptableSpeciesEntry> = emptyList(),
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class AcceptableSpeciesEntry(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("commonName") val commonName: String,
    @SerializedName("variantName") val variantName: String?,
    @SerializedName("commonNameSv") val commonNameSv: String?,
    @SerializedName("variantNameSv") val variantNameSv: String?,
)

data class CreateScheduledTaskRequest(
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("speciesGroupId") val speciesGroupId: Long? = null,
    @SerializedName("speciesIds") val speciesIds: List<Long>? = null,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("deadline") val deadline: String,
    @SerializedName("targetCount") val targetCount: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("successionScheduleId") val successionScheduleId: Long? = null,
)

data class UpdateScheduledTaskRequest(
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("activityType") val activityType: String? = null,
    @SerializedName("deadline") val deadline: String? = null,
    @SerializedName("targetCount") val targetCount: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
)

data class CompleteTaskPartiallyRequest(
    @SerializedName("processedCount") val processedCount: Int,
    @SerializedName("speciesId") val speciesId: Long,
)
