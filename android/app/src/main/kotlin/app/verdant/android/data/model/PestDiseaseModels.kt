package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Pest/Disease Logs ──

data class PestDiseaseLogResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("seasonId") val seasonId: Long?,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("speciesId") val speciesId: Long?,
    @SerializedName("observedDate") val observedDate: String,
    @SerializedName("category") val category: String,
    @SerializedName("name") val name: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("treatment") val treatment: String?,
    @SerializedName("outcome") val outcome: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreatePestDiseaseLogRequest(
    @SerializedName("category") val category: String,
    @SerializedName("name") val name: String,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("observedDate") val observedDate: String? = null,
    @SerializedName("severity") val severity: String = "MODERATE",
    @SerializedName("treatment") val treatment: String? = null,
    @SerializedName("outcome") val outcome: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
)

data class UpdatePestDiseaseLogRequest(
    @SerializedName("category") val category: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("seasonId") val seasonId: Long? = null,
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("observedDate") val observedDate: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("treatment") val treatment: String? = null,
    @SerializedName("outcome") val outcome: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
)

object PestCategory {
    const val PEST = "PEST"
    const val DISEASE = "DISEASE"
    const val DEFICIENCY = "DEFICIENCY"
    const val OTHER = "OTHER"
    val values = listOf(PEST, DISEASE, DEFICIENCY, OTHER)
}

object Severity {
    const val LOW = "LOW"
    const val MODERATE = "MODERATE"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
    val values = listOf(LOW, MODERATE, HIGH, CRITICAL)
}

object Outcome {
    const val RESOLVED = "RESOLVED"
    const val ONGOING = "ONGOING"
    const val CROP_LOSS = "CROP_LOSS"
    const val MONITORING = "MONITORING"
    val values = listOf(RESOLVED, ONGOING, CROP_LOSS, MONITORING)
}
