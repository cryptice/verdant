package app.verdant.repository

import app.verdant.entity.BouquetRecipe
import app.verdant.entity.BouquetRecipeItem
import app.verdant.entity.ItemRole
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class BouquetRecipeRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): BouquetRecipe? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bouquet_recipe WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toBouquetRecipe() else null }
            }
        }

    fun findByUserId(userId: Long, limit: Int = 50, offset: Int = 0): List<BouquetRecipe> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bouquet_recipe WHERE user_id = ? ORDER BY name LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, userId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toBouquetRecipe()) }
                }
            }
        }

    fun persist(recipe: BouquetRecipe): BouquetRecipe {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO bouquet_recipe (user_id, name, description, image_url, price_sek, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, recipe.userId)
                ps.setString(2, recipe.name)
                ps.setString(3, recipe.description)
                ps.setString(4, recipe.imageUrl)
                ps.setObject(5, recipe.priceSek)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return recipe.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(recipe: BouquetRecipe) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE bouquet_recipe SET name = ?, description = ?, image_url = ?, price_sek = ?, updated_at = now() WHERE id = ?"
            ).use { ps ->
                ps.setString(1, recipe.name)
                ps.setString(2, recipe.description)
                ps.setString(3, recipe.imageUrl)
                ps.setObject(4, recipe.priceSek)
                ps.setLong(5, recipe.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM bouquet_recipe WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Bouquet recipe not found")
            }
        }
    }

    // --- BouquetRecipeItem methods ---

    fun findItemsByRecipeId(recipeId: Long): List<BouquetRecipeItem> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bouquet_recipe_item WHERE recipe_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, recipeId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toBouquetRecipeItem()) }
                }
            }
        }

    fun persistItem(item: BouquetRecipeItem): BouquetRecipeItem {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO bouquet_recipe_item (recipe_id, species_id, stem_count, role, notes)
                   VALUES (?, ?, ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, item.recipeId)
                ps.setLong(2, item.speciesId)
                ps.setInt(3, item.stemCount)
                ps.setString(4, item.role.name)
                ps.setString(5, item.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return item.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun deleteItemsByRecipeId(recipeId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM bouquet_recipe_item WHERE recipe_id = ?").use { ps ->
                ps.setLong(1, recipeId)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toBouquetRecipe() = BouquetRecipe(
        id = getLong("id"),
        userId = getLong("user_id"),
        name = getString("name"),
        description = getString("description"),
        imageUrl = getString("image_url"),
        priceSek = getObject("price_sek") as? Int,
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )

    private fun ResultSet.toBouquetRecipeItem() = BouquetRecipeItem(
        id = getLong("id"),
        recipeId = getLong("recipe_id"),
        speciesId = getLong("species_id"),
        stemCount = getInt("stem_count"),
        role = ItemRole.valueOf(getString("role")),
        notes = getString("notes"),
    )
}
