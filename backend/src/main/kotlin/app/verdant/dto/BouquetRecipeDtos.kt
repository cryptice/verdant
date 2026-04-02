package app.verdant.dto

import app.verdant.entity.ItemRole
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class BouquetRecipeResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val priceSek: Int?,
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
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    val imageBase64: String? = null,
    @field:Min(0)
    val priceSek: Int? = null,
    @field:Size(max = 100)
    @field:Valid
    val items: List<CreateBouquetRecipeItemRequest> = emptyList(),
)

data class CreateBouquetRecipeItemRequest(
    @field:NotNull
    val speciesId: Long,
    @field:Min(1)
    val stemCount: Int,
    val role: ItemRole = ItemRole.FLOWER,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateBouquetRecipeRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    val imageBase64: String? = null,
    @field:Min(0)
    val priceSek: Int? = null,
    @field:Size(max = 100)
    @field:Valid
    val items: List<CreateBouquetRecipeItemRequest>? = null,
)
