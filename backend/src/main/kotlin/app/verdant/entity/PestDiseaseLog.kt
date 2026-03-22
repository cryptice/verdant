package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class PestDiseaseLog(
    val id: Long? = null,
    val userId: Long,
    val seasonId: Long? = null,
    val bedId: Long? = null,
    val speciesId: Long? = null,
    val observedDate: LocalDate = LocalDate.now(),
    val category: PestCategory,
    val name: String,
    val severity: Severity = Severity.MODERATE,
    val treatment: String? = null,
    val outcome: Outcome? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    val createdAt: Instant = Instant.now(),
)

enum class PestCategory { PEST, DISEASE, DEFICIENCY, OTHER }
enum class Severity { LOW, MODERATE, HIGH, CRITICAL }
enum class Outcome { RESOLVED, ONGOING, CROP_LOSS, MONITORING }
