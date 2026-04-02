package app.verdant.dto

import app.verdant.entity.Reception
import app.verdant.entity.Verdict
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    @field:NotNull
    val seasonId: Long,
    @field:NotNull
    val speciesId: Long,
    val bedId: Long? = null,
    @field:Min(0)
    val plantCount: Int? = null,
    @field:Min(0)
    val stemYield: Int? = null,
    @field:Min(0)
    val avgStemLengthCm: Int? = null,
    @field:Min(0)
    val avgVaseLifeDays: Int? = null,
    @field:Min(1) @field:Max(10)
    val qualityScore: Int? = null,
    val customerReception: Reception? = null,
    val verdict: Verdict = Verdict.UNDECIDED,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateVarietyTrialRequest(
    val seasonId: Long? = null,
    val speciesId: Long? = null,
    val bedId: Long? = null,
    @field:Min(0)
    val plantCount: Int? = null,
    @field:Min(0)
    val stemYield: Int? = null,
    @field:Min(0)
    val avgStemLengthCm: Int? = null,
    @field:Min(0)
    val avgVaseLifeDays: Int? = null,
    @field:Min(1) @field:Max(10)
    val qualityScore: Int? = null,
    val customerReception: Reception? = null,
    val verdict: Verdict? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)
