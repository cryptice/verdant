package app.verdant.entity

import java.time.Instant

data class Bouquet(
    val id: Long? = null,
    val orgId: Long,
    /** Recipe this bouquet was built from, if any. Stored for analytics; the
     *  bouquet's own item list is the source of truth after creation. */
    val sourceRecipeId: Long? = null,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val priceSek: Int? = null,
    val assembledAt: Instant = Instant.now(),
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class BouquetItem(
    val id: Long? = null,
    val bouquetId: Long,
    val speciesId: Long,
    val stemCount: Int,
    val role: ItemRole = ItemRole.FLOWER,
    val notes: String? = null,
)
