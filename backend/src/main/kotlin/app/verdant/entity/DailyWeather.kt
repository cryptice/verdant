package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

enum class ObservationType { ACTUAL, FORECAST }

data class DailyWeather(
    val id: Long? = null,
    val gardenId: Long,
    val date: LocalDate,
    val observationType: ObservationType,
    val tempMinC: Double?,
    val tempMaxC: Double?,
    val tempMeanC: Double?,
    val precipitationMm: Double?,
    val windMaxMs: Double?,
    val humidityPct: Double?,
    val fetchedAt: Instant = Instant.now(),
)
