package app.verdant.entity

import java.time.Instant

data class VarietyTrial(
    val id: Long? = null,
    val userId: Long,
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
    val createdAt: Instant = Instant.now(),
)

enum class Verdict { KEEP, EXPAND, REDUCE, DROP, UNDECIDED }
enum class Reception { LOVED, LIKED, NEUTRAL, DISLIKED }
