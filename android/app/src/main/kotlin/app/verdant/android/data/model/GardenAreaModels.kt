package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class GardenAreaResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("gardenId") val gardenId: Long,
    @SerializedName("gardenName") val gardenName: String?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val category: String,
    @SerializedName("boundaryJson") val boundaryJson: String?,
    @SerializedName("sizeSqm") val sizeSqm: Double?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class CreateGardenAreaRequest(
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("sizeSqm") val sizeSqm: Double? = null,
)

/** Omitted fields keep their current server-side value. */
data class UpdateGardenAreaRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("sizeSqm") val sizeSqm: Double? = null,
    /**
     * The ONLY way to empty the description — a null is indistinguishable
     * from an omitted field server-side, so it means "keep". Cannot be
     * combined with a [description] value; doing so is a 400.
     */
    @SerializedName("clearDescription") val clearDescription: Boolean = false,
    /** The ONLY way to empty the size. See [clearDescription]. */
    @SerializedName("clearSizeSqm") val clearSizeSqm: Boolean = false,
)

data class GardenAreaEventResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("gardenAreaId") val gardenAreaId: Long,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateGardenAreaEventRequest(
    @SerializedName("activityType") val activityType: String,
    @SerializedName("eventDate") val eventDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class GardenAreaPhotoResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("gardenAreaId") val gardenAreaId: Long,
    @SerializedName("photoUrl") val photoUrl: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("description") val description: String?,
    @SerializedName("capturedAt") val capturedAt: String,
    @SerializedName("createdAt") val createdAt: String,
)

/**
 * Raw image bytes. The server mints the storage path and public URL — the
 * contract deliberately has no client-supplied URL field, because one would
 * let a caller aim a later delete at another org's blob.
 */
data class CreateGardenAreaPhotoRequest(
    @SerializedName("imageBase64") val imageBase64: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("capturedAt") val capturedAt: String? = null,
)
