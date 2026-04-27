package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Bouquet Recipes ──

data class BouquetRecipeResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("priceCents") val priceCents: Int?,
    @SerializedName("items") val items: List<BouquetRecipeItemResponse>,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class BouquetRecipeItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("stemCount") val stemCount: Int,
    @SerializedName("role") val role: String,
    @SerializedName("notes") val notes: String?,
)

data class CreateBouquetRecipeRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
    @SerializedName("priceCents") val priceCents: Int? = null,
    @SerializedName("items") val items: List<CreateBouquetRecipeItemRequest> = emptyList(),
)

data class CreateBouquetRecipeItemRequest(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("stemCount") val stemCount: Int,
    @SerializedName("role") val role: String = "FLOWER",
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateBouquetRecipeRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageBase64") val imageBase64: String? = null,
    @SerializedName("priceCents") val priceCents: Int? = null,
    @SerializedName("items") val items: List<CreateBouquetRecipeItemRequest>? = null,
)

// ── Bouquets (instances) ──

data class BouquetResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("sourceRecipeId") val sourceRecipeId: Long? = null,
    @SerializedName("sourceRecipeName") val sourceRecipeName: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("priceCents") val priceCents: Int? = null,
    @SerializedName("assembledAt") val assembledAt: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<BouquetItemResponse> = emptyList(),
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class BouquetItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String? = null,
    @SerializedName("stemCount") val stemCount: Int,
    @SerializedName("role") val role: String,
    @SerializedName("notes") val notes: String? = null,
)

data class CreateBouquetRequest(
    @SerializedName("sourceRecipeId") val sourceRecipeId: Long? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priceCents") val priceCents: Int? = null,
    @SerializedName("assembledAt") val assembledAt: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<CreateBouquetItemRequest> = emptyList(),
)

data class CreateBouquetItemRequest(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("stemCount") val stemCount: Int,
    @SerializedName("role") val role: String = "FLOWER",
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateBouquetRequest(
    @SerializedName("sourceRecipeId") val sourceRecipeId: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priceCents") val priceCents: Int? = null,
    @SerializedName("assembledAt") val assembledAt: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<CreateBouquetItemRequest>? = null,
)

object ItemRole {
    const val FLOWER = "FLOWER"
    const val FOLIAGE = "FOLIAGE"
    const val FILLER = "FILLER"
    const val ACCENT = "ACCENT"
    val values = listOf(FLOWER, FOLIAGE, FILLER, ACCENT)
}
