package app.verdant.repository

import app.verdant.entity.Bouquet
import app.verdant.entity.BouquetItem
import app.verdant.entity.ItemRole
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp

@ApplicationScoped
class BouquetRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Bouquet? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bouquet WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toBouquet() else null }
            }
        }

    fun findByOrgId(orgId: Long, limit: Int = 100, offset: Int = 0): List<Bouquet> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM bouquet WHERE org_id = ? ORDER BY assembled_at DESC LIMIT ? OFFSET ?"
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toBouquet()) }
                }
            }
        }

    fun persist(bouquet: Bouquet): Bouquet {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO bouquet (org_id, source_recipe_id, name, description, image_url, price_cents, assembled_at, notes, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, bouquet.orgId)
                ps.setObject(2, bouquet.sourceRecipeId)
                ps.setString(3, bouquet.name)
                ps.setString(4, bouquet.description)
                ps.setString(5, bouquet.imageUrl)
                ps.setObject(6, bouquet.priceCents)
                ps.setTimestamp(7, Timestamp.from(bouquet.assembledAt))
                ps.setString(8, bouquet.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return bouquet.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(bouquet: Bouquet) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE bouquet
                   SET source_recipe_id = ?, name = ?, description = ?, image_url = ?,
                       price_cents = ?, assembled_at = ?, notes = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setObject(1, bouquet.sourceRecipeId)
                ps.setString(2, bouquet.name)
                ps.setString(3, bouquet.description)
                ps.setString(4, bouquet.imageUrl)
                ps.setObject(5, bouquet.priceCents)
                ps.setTimestamp(6, Timestamp.from(bouquet.assembledAt))
                ps.setString(7, bouquet.notes)
                ps.setLong(8, bouquet.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM bouquet WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Bouquet not found")
            }
        }
    }

    // --- BouquetItem methods ---

    fun findItemsByBouquetId(bouquetId: Long): List<BouquetItem> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM bouquet_item WHERE bouquet_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, bouquetId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toBouquetItem()) }
                }
            }
        }

    fun persistItem(item: BouquetItem): BouquetItem {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO bouquet_item (bouquet_id, species_id, stem_count, role, notes)
                   VALUES (?, ?, ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, item.bouquetId)
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

    fun deleteItemsByBouquetId(bouquetId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM bouquet_item WHERE bouquet_id = ?").use { ps ->
                ps.setLong(1, bouquetId)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toBouquet() = Bouquet(
        id = getLong("id"),
        orgId = getLong("org_id"),
        sourceRecipeId = getLong("source_recipe_id").takeIf { !wasNull() },
        name = getString("name"),
        description = getString("description"),
        imageUrl = getString("image_url"),
        priceCents = getObject("price_cents") as? Int,
        assembledAt = getTimestamp("assembled_at").toInstant(),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )

    private fun ResultSet.toBouquetItem() = BouquetItem(
        id = getLong("id"),
        bouquetId = getLong("bouquet_id"),
        speciesId = getLong("species_id"),
        stemCount = getInt("stem_count"),
        role = ItemRole.valueOf(getString("role")),
        notes = getString("notes"),
    )
}
