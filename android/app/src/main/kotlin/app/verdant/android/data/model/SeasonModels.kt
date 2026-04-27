package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Seasons ──

data class SeasonResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: Int,
    @SerializedName("startDate") val startDate: String?,
    @SerializedName("endDate") val endDate: String?,
    @SerializedName("lastFrostDate") val lastFrostDate: String?,
    @SerializedName("firstFrostDate") val firstFrostDate: String?,
    @SerializedName("growingDegreeBaseC") val growingDegreeBaseC: Double?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class CreateSeasonRequest(
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: Int,
    @SerializedName("lastFrostDate") val lastFrostDate: String? = null,
    @SerializedName("firstFrostDate") val firstFrostDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
)
