package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Variety Trials ──

data class VarietyTrialResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("plantCount") val plantCount: Int?,
    @SerializedName("stemYield") val stemYield: Int?,
    @SerializedName("avgStemLengthCm") val avgStemLengthCm: Int?,
    @SerializedName("avgVaseLifeDays") val avgVaseLifeDays: Int?,
    @SerializedName("qualityScore") val qualityScore: Int?,
    @SerializedName("customerReception") val customerReception: String?,
    @SerializedName("verdict") val verdict: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateVarietyTrialRequest(
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("plantCount") val plantCount: Int? = null,
    @SerializedName("stemYield") val stemYield: Int? = null,
    @SerializedName("avgStemLengthCm") val avgStemLengthCm: Int? = null,
    @SerializedName("avgVaseLifeDays") val avgVaseLifeDays: Int? = null,
    @SerializedName("qualityScore") val qualityScore: Int? = null,
    @SerializedName("customerReception") val customerReception: String? = null,
    @SerializedName("verdict") val verdict: String = "UNDECIDED",
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateVarietyTrialRequest(
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("plantCount") val plantCount: Int? = null,
    @SerializedName("stemYield") val stemYield: Int? = null,
    @SerializedName("avgStemLengthCm") val avgStemLengthCm: Int? = null,
    @SerializedName("avgVaseLifeDays") val avgVaseLifeDays: Int? = null,
    @SerializedName("qualityScore") val qualityScore: Int? = null,
    @SerializedName("customerReception") val customerReception: String? = null,
    @SerializedName("verdict") val verdict: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

object Verdict {
    const val KEEP = "KEEP"
    const val EXPAND = "EXPAND"
    const val REDUCE = "REDUCE"
    const val DROP = "DROP"
    const val UNDECIDED = "UNDECIDED"
    val values = listOf(KEEP, EXPAND, REDUCE, DROP, UNDECIDED)
}

object Reception {
    const val LOVED = "LOVED"
    const val LIKED = "LIKED"
    const val NEUTRAL = "NEUTRAL"
    const val DISLIKED = "DISLIKED"
    val values = listOf(LOVED, LIKED, NEUTRAL, DISLIKED)
}
