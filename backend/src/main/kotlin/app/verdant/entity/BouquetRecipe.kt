package app.verdant.entity

import java.time.Instant

data class BouquetRecipe(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val priceCents: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class BouquetRecipeItem(
    val id: Long? = null,
    val recipeId: Long,
    val speciesId: Long,
    val stemCount: Int,
    val role: ItemRole = ItemRole.FLOWER,
    val notes: String? = null,
)

enum class ItemRole { FLOWER, FOLIAGE, FILLER, ACCENT }
