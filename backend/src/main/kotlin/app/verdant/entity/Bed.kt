package app.verdant.entity

import java.time.Instant

// SoilType is defined on Species — same 6 values; reused here.
enum class SunExposure { FULL_SUN, PARTIAL_SUN, PARTIAL_SHADE, FULL_SHADE }
enum class Drainage { POOR, MODERATE, GOOD, SHARP }
enum class CompassDirection { N, NE, E, SE, S, SW, W, NW }
enum class IrrigationType { DRIP, SPRINKLER, SOAKER_HOSE, MANUAL, NONE }
enum class Protection { OPEN_FIELD, ROW_COVER, LOW_TUNNEL, HIGH_TUNNEL, GREENHOUSE, COLDFRAME }

data class Bed(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val gardenId: Long,
    val boundaryJson: String? = null,
    val lengthMeters: Double? = null,
    val widthMeters: Double? = null,
    val soilType: SoilType? = null,
    val soilPh: Double? = null,
    val sunExposure: SunExposure? = null,
    val drainage: Drainage? = null,
    /** Compass directions the bed actually receives sun from. Empty/null when not specified. */
    val sunDirections: List<CompassDirection> = emptyList(),
    val irrigationType: IrrigationType? = null,
    val protection: Protection? = null,
    val raisedBed: Boolean? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
