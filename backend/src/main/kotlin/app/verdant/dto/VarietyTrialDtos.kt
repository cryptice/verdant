package app.verdant.dto

import app.verdant.entity.Reception
import app.verdant.entity.Verdict
import java.time.Instant

data class VarietyTrialResponse(
    val id: Long,
    val seasonId: Long,
    val speciesId: Long,
    val speciesName: String?,
    val bedId: Long?,
    val plantCount: Int?,
    val stemYield: Int?,
    val avgStemLengthCm: Int?,
    val avgVaseLifeDays: Int?,
    val qualityScore: Int?,
    val customerReception: Reception?,
    val verdict: Verdict,
    val notes: String?,
    val createdAt: Instant,
)

data class CreateVarietyTrialRequest(
    val seasonId: Long,
    val speciesId: Long,
    val bedId: Long? = null,
    val plantCount: Int? = null,
    val stemYield: Int? = null,
    val avgStemLengthCm: Int? = null,
    val avgVaseLifeDays: Int? = null,
    val qualityScore: Int? = null,
    val customerReception: Reception? = null,
    val verdict: Verdict = Verdict.UNDECIDED,
    val notes: String? = null,
)

data class UpdateVarietyTrialRequest(
    val seasonId: Long? = null,
    val speciesId: Long? = null,
    val bedId: Long? = null,
    val plantCount: Int? = null,
    val stemYield: Int? = null,
    val avgStemLengthCm: Int? = null,
    val avgVaseLifeDays: Int? = null,
    val qualityScore: Int? = null,
    val customerReception: Reception? = null,
    val verdict: Verdict? = null,
    val notes: String? = null,
)
