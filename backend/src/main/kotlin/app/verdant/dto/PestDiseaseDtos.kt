package app.verdant.dto

import app.verdant.entity.Outcome
import app.verdant.entity.PestCategory
import app.verdant.entity.Severity
import java.time.Instant
import java.time.LocalDate

data class PestDiseaseLogResponse(
    val id: Long,
    val seasonId: Long?,
    val bedId: Long?,
    val speciesId: Long?,
    val observedDate: LocalDate,
    val category: PestCategory,
    val name: String,
    val severity: Severity,
    val treatment: String?,
    val outcome: Outcome?,
    val notes: String?,
    val imageUrl: String?,
    val createdAt: Instant,
)

data class CreatePestDiseaseLogRequest(
    val category: PestCategory,
    val name: String,
    val seasonId: Long? = null,
    val bedId: Long? = null,
    val speciesId: Long? = null,
    val observedDate: LocalDate = LocalDate.now(),
    val severity: Severity = Severity.MODERATE,
    val treatment: String? = null,
    val outcome: Outcome? = null,
    val notes: String? = null,
    val imageBase64: String? = null,
)

data class UpdatePestDiseaseLogRequest(
    val category: PestCategory? = null,
    val name: String? = null,
    val seasonId: Long? = null,
    val bedId: Long? = null,
    val speciesId: Long? = null,
    val observedDate: LocalDate? = null,
    val severity: Severity? = null,
    val treatment: String? = null,
    val outcome: Outcome? = null,
    val notes: String? = null,
    val imageBase64: String? = null,
)
