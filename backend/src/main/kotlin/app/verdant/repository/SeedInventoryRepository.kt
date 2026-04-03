package app.verdant.repository

import app.verdant.entity.SeedInventory
import app.verdant.entity.UnitType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SeedInventoryRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SeedInventory? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM seed_inventory WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSeedInventory() else null }
            }
        }

    fun findByUserId(userId: Long, limit: Int = 50, offset: Int = 0): List<SeedInventory> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT si.* FROM seed_inventory si
                   JOIN species s ON si.species_id = s.id
                   WHERE si.user_id = ?
                   ORDER BY s.common_name, si.expiration_date NULLS LAST LIMIT ? OFFSET ?"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSeedInventory()) }
                }
            }
        }

    fun findBySeasonId(userId: Long, seasonId: Long, limit: Int = 50, offset: Int = 0): List<SeedInventory> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT si.* FROM seed_inventory si
                   JOIN species s ON si.species_id = s.id
                   WHERE si.user_id = ? AND si.season_id = ?
                   ORDER BY s.common_name, si.expiration_date NULLS LAST LIMIT ? OFFSET ?"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, seasonId)
                ps.setInt(3, limit)
                ps.setInt(4, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSeedInventory()) }
                }
            }
        }

    fun findByUserIdAndSpeciesId(userId: Long, speciesId: Long): List<SeedInventory> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM seed_inventory
                   WHERE user_id = ? AND species_id = ? AND quantity > 0
                   ORDER BY expiration_date NULLS LAST"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSeedInventory()) }
                }
            }
        }

    fun persist(inventory: SeedInventory): SeedInventory {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO seed_inventory (user_id, species_id, quantity, collection_date, expiration_date, cost_per_unit_sek, unit_type, season_id, species_provider_id, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, inventory.userId)
                ps.setLong(2, inventory.speciesId)
                ps.setInt(3, inventory.quantity)
                ps.setObject(4, inventory.collectionDate)
                ps.setObject(5, inventory.expirationDate)
                ps.setObject(6, inventory.costPerUnitSek)
                ps.setString(7, inventory.unitType.name)
                ps.setObject(8, inventory.seasonId)
                ps.setObject(9, inventory.speciesProviderId)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return inventory.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(inventory: SeedInventory) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE seed_inventory SET quantity = ?, collection_date = ?, expiration_date = ?,
                   cost_per_unit_sek = ?, unit_type = ?, season_id = ?, species_provider_id = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setInt(1, inventory.quantity)
                ps.setObject(2, inventory.collectionDate)
                ps.setObject(3, inventory.expirationDate)
                ps.setObject(4, inventory.costPerUnitSek)
                ps.setString(5, inventory.unitType.name)
                ps.setObject(6, inventory.seasonId)
                ps.setObject(7, inventory.speciesProviderId)
                ps.setLong(8, inventory.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun decrementQuantity(id: Long, amount: Int): Boolean =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE seed_inventory SET quantity = quantity - ? WHERE id = ? AND quantity >= ?"
            ).use { ps ->
                ps.setInt(1, amount)
                ps.setLong(2, id)
                ps.setInt(3, amount)
                ps.executeUpdate() > 0
            }
        }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM seed_inventory WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Seed stock not found")
            }
        }
    }

    private fun ResultSet.toSeedInventory() = SeedInventory(
        id = getLong("id"),
        userId = getLong("user_id"),
        speciesId = getLong("species_id"),
        quantity = getInt("quantity"),
        collectionDate = getObject("collection_date", java.time.LocalDate::class.java),
        expirationDate = getObject("expiration_date", java.time.LocalDate::class.java),
        costPerUnitSek = getObject("cost_per_unit_sek") as? Int,
        unitType = getString("unit_type")?.let { UnitType.valueOf(it) } ?: UnitType.SEED,
        seasonId = getObject("season_id") as? Long,
        speciesProviderId = getObject("species_provider_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
