package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

/** "Sow [seeds] from this batch to have [plants] ready on [readyDate]" (ISO date). */
data class SeedBatchPlanRequest(
    @SerializedName("seeds") val seeds: Int,
    @SerializedName("plants") val plants: Int,
    @SerializedName("readyDate") val readyDate: String,
)

data class SeedBatchPlanStep(
    @SerializedName("name") val name: String,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("date") val date: String,
    @SerializedName("count") val count: Int,
    /** Optional workflow steps keep their place in the timeline but get no task. */
    @SerializedName("optional") val optional: Boolean,
)

data class SeedBatchPlanResponse(
    @SerializedName("seedBatchId") val seedBatchId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("seeds") val seeds: Int,
    @SerializedName("plants") val plants: Int,
    @SerializedName("readyDate") val readyDate: String,
    @SerializedName("steps") val steps: List<SeedBatchPlanStep>,
    @SerializedName("taskIds") val taskIds: List<Long> = emptyList(),
)
