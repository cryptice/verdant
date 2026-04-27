package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class GardenResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("emoji") val emoji: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("address") val address: String?,
    @SerializedName("boundaryJson") val boundaryJson: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CreateGardenRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("emoji") val emoji: String? = "\uD83C\uDF31",
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class UpdateGardenRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class BedResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("gardenId") val gardenId: Long,
    @SerializedName("boundaryJson") val boundaryJson: String?,
    @SerializedName("lengthMeters") val lengthMeters: Double? = null,
    @SerializedName("widthMeters") val widthMeters: Double? = null,
    @SerializedName("soilType") val soilType: String? = null,
    @SerializedName("soilPh") val soilPh: Double? = null,
    @SerializedName("sunExposure") val sunExposure: String? = null,
    @SerializedName("drainage") val drainage: String? = null,
    @SerializedName("sunDirections") val sunDirections: List<String>? = null,
    @SerializedName("irrigationType") val irrigationType: String? = null,
    @SerializedName("protection") val protection: String? = null,
    @SerializedName("raisedBed") val raisedBed: Boolean? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CreateBedRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("soilType") val soilType: String? = null,
    @SerializedName("soilPh") val soilPh: Double? = null,
    @SerializedName("sunExposure") val sunExposure: String? = null,
    @SerializedName("drainage") val drainage: String? = null,
    @SerializedName("sunDirections") val sunDirections: List<String>? = null,
    @SerializedName("irrigationType") val irrigationType: String? = null,
    @SerializedName("protection") val protection: String? = null,
    @SerializedName("raisedBed") val raisedBed: Boolean? = null
)

data class UpdateBedRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("soilType") val soilType: String? = null,
    @SerializedName("soilPh") val soilPh: Double? = null,
    @SerializedName("sunExposure") val sunExposure: String? = null,
    @SerializedName("drainage") val drainage: String? = null,
    @SerializedName("sunDirections") val sunDirections: List<String>? = null,
    @SerializedName("irrigationType") val irrigationType: String? = null,
    @SerializedName("protection") val protection: String? = null,
    @SerializedName("raisedBed") val raisedBed: Boolean? = null
)

// Bed condition enum values (matches backend enums)
object BedSoilType {
    const val SANDY = "SANDY"
    const val LOAMY = "LOAMY"
    const val CLAY = "CLAY"
    const val SILTY = "SILTY"
    const val PEATY = "PEATY"
    const val CHALKY = "CHALKY"
    val values = listOf(SANDY, LOAMY, CLAY, SILTY, PEATY, CHALKY)
}

object BedSunExposure {
    const val FULL_SUN = "FULL_SUN"
    const val PARTIAL_SUN = "PARTIAL_SUN"
    const val PARTIAL_SHADE = "PARTIAL_SHADE"
    const val FULL_SHADE = "FULL_SHADE"
    val values = listOf(FULL_SUN, PARTIAL_SUN, PARTIAL_SHADE, FULL_SHADE)
}

object BedDrainage {
    const val POOR = "POOR"
    const val MODERATE = "MODERATE"
    const val GOOD = "GOOD"
    const val SHARP = "SHARP"
    val values = listOf(POOR, MODERATE, GOOD, SHARP)
}

object CompassDirection {
    const val N = "N"
    const val NE = "NE"
    const val E = "E"
    const val SE = "SE"
    const val S = "S"
    const val SW = "SW"
    const val W = "W"
    const val NW = "NW"
    val values = listOf(N, NE, E, SE, S, SW, W, NW)
}

object BedIrrigationType {
    const val DRIP = "DRIP"
    const val SPRINKLER = "SPRINKLER"
    const val SOAKER_HOSE = "SOAKER_HOSE"
    const val MANUAL = "MANUAL"
    const val NONE = "NONE"
    val values = listOf(DRIP, SPRINKLER, SOAKER_HOSE, MANUAL, NONE)
}

object BedProtection {
    const val OPEN_FIELD = "OPEN_FIELD"
    const val ROW_COVER = "ROW_COVER"
    const val LOW_TUNNEL = "LOW_TUNNEL"
    const val HIGH_TUNNEL = "HIGH_TUNNEL"
    const val GREENHOUSE = "GREENHOUSE"
    const val COLDFRAME = "COLDFRAME"
    val values = listOf(OPEN_FIELD, ROW_COVER, LOW_TUNNEL, HIGH_TUNNEL, GREENHOUSE, COLDFRAME)
}

data class BedWithGardenResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("gardenId") val gardenId: Long,
    @SerializedName("gardenName") val gardenName: String,
    @SerializedName("boundaryJson") val boundaryJson: String?,
)
