package app.verdant.dto

import app.verdant.entity.ItemRole
import java.time.Instant

data class BouquetRecipeResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val priceCents: Int?,
    val items: List<BouquetRecipeItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class BouquetRecipeItemResponse(
    val id: Long,
    val speciesId: Long,
    val speciesName: String?,
    val stemCount: Int,
    val role: ItemRole,
    val notes: String?,
)

data class CreateBouquetRecipeRequest(
    val name: String,
    val description: String? = null,
    val imageBase64: String? = null,
    val priceCents: Int? = null,
    val items: List<CreateBouquetRecipeItemRequest> = emptyList(),
)

data class CreateBouquetRecipeItemRequest(
    val speciesId: Long,
    val stemCount: Int,
    val role: ItemRole = ItemRole.FLOWER,
    val notes: String? = null,
)

data class UpdateBouquetRecipeRequest(
    val name: String? = null,
    val description: String? = null,
    val imageBase64: String? = null,
    val priceCents: Int? = null,
    val items: List<CreateBouquetRecipeItemRequest>? = null,
)
