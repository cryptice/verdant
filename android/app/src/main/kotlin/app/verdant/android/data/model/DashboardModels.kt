package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("user") val user: UserResponse,
    @SerializedName("gardens") val gardens: List<GardenSummary>,
    @SerializedName("stats") val stats: DashboardStats
)

data class GardenSummary(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String?,
    @SerializedName("bedCount") val bedCount: Int,
    @SerializedName("plantCount") val plantCount: Int
)

data class DashboardStats(
    @SerializedName("totalGardens") val totalGardens: Int,
    @SerializedName("totalBeds") val totalBeds: Int,
    @SerializedName("totalPlants") val totalPlants: Int,
    @SerializedName("totalActivePlants") val totalActivePlants: Int = 0,
    @SerializedName("totalActiveSpecies") val totalActiveSpecies: Int = 0,
)

data class LatLng(@SerializedName("lat") val lat: Double, @SerializedName("lng") val lng: Double)

data class SuggestLayoutRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String? = null
)

data class SuggestedBed(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("boundary") val boundary: List<LatLng>
)

data class SuggestLayoutResponse(
    @SerializedName("gardenName") val gardenName: String,
    @SerializedName("boundary") val boundary: List<LatLng>,
    @SerializedName("beds") val beds: List<SuggestedBed>
)

data class CreateGardenWithLayoutRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("emoji") val emoji: String? = "\uD83C\uDF31",
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("beds") val beds: List<BedLayoutItem> = emptyList()
)

data class BedLayoutItem(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null
)

data class GardenWithBedsResponse(
    @SerializedName("garden") val garden: GardenResponse,
    @SerializedName("beds") val beds: List<BedResponse>
)
