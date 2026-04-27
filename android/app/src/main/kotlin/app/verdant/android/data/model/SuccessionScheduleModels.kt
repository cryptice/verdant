package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Succession Schedules ──

data class SuccessionScheduleResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("firstSowDate") val firstSowDate: String,
    @SerializedName("intervalDays") val intervalDays: Int,
    @SerializedName("totalSuccessions") val totalSuccessions: Int,
    @SerializedName("seedsPerSuccession") val seedsPerSuccession: Int,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)
