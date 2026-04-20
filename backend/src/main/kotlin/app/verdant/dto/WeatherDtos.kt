package app.verdant.dto

import app.verdant.entity.ObservationType
import java.time.LocalDate

data class DailyWeatherResponse(
    val date: LocalDate,
    val observationType: ObservationType,
    val tempMinC: Double?,
    val tempMaxC: Double?,
    val tempMeanC: Double?,
    val precipitationMm: Double?,
    val windMaxMs: Double?,
    val humidityPct: Double?,
)

data class GardenWeatherResponse(
    val gardenId: Long,
    val backfillStatus: String?,
    val days: List<DailyWeatherResponse>,
)
