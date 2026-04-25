package app.verdant.dto

import app.verdant.entity.ItemRole
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class BouquetResponse(
    val id: Long,
    val sourceRecipeId: Long?,
    val sourceRecipeName: String?,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val priceCents: Int?,
    val assembledAt: Instant,
    val notes: String?,
    val items: List<BouquetItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class BouquetItemResponse(
    val id: Long,
    val speciesId: Long,
    val speciesName: String?,
    val stemCount: Int,
    val role: ItemRole,
    val notes: String?,
)

data class CreateBouquetRequest(
    val sourceRecipeId: Long? = null,
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Min(0)
    val priceCents: Int? = null,
    val assembledAt: Instant? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
    @field:Size(max = 100)
    @field:Valid
    val items: List<CreateBouquetItemRequest> = emptyList(),
)

data class CreateBouquetItemRequest(
    @field:NotNull
    val speciesId: Long,
    @field:Min(1)
    val stemCount: Int,
    val role: ItemRole = ItemRole.FLOWER,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateBouquetRequest(
    val sourceRecipeId: Long? = null,
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Min(0)
    val priceCents: Int? = null,
    val assembledAt: Instant? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
    @field:Size(max = 100)
    @field:Valid
    val items: List<CreateBouquetItemRequest>? = null,
)
