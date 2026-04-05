package app.verdant.repository

import app.verdant.entity.SupplyInventory
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SupplyInventoryRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SupplyInventory? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM supply_inventory WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSupplyInventory() else null }
            }
        }

    fun findByOrgId(orgId: Long, limit: Int = 50, offset: Int = 0): List<SupplyInventory> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT si.* FROM supply_inventory si
                   JOIN supply_type st ON si.supply_type_id = st.id
                   WHERE si.org_id = ?
                   ORDER BY st.name, si.created_at DESC LIMIT ? OFFSET ?"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSupplyInventory()) }
                }
            }
        }

    fun findBySupplyTypeId(supplyTypeId: Long): List<SupplyInventory> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM supply_inventory WHERE supply_type_id = ? ORDER BY created_at DESC"
            ).use { ps ->
                ps.setLong(1, supplyTypeId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSupplyInventory()) }
                }
            }
        }

    fun persist(item: SupplyInventory): SupplyInventory {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO supply_inventory (org_id, supply_type_id, quantity, cost_sek, season_id, notes, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, item.orgId)
                ps.setLong(2, item.supplyTypeId)
                ps.setBigDecimal(3, item.quantity)
                ps.setObject(4, item.costSek)
                ps.setObject(5, item.seasonId)
                ps.setString(6, item.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return item.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(item: SupplyInventory) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE supply_inventory SET quantity = ?, cost_sek = ?, season_id = ?, notes = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setBigDecimal(1, item.quantity)
                ps.setObject(2, item.costSek)
                ps.setObject(3, item.seasonId)
                ps.setString(4, item.notes)
                ps.setLong(5, item.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun decrementQuantity(id: Long, amount: BigDecimal) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE supply_inventory SET quantity = GREATEST(quantity - ?, 0) WHERE id = ?"
            ).use { ps ->
                ps.setBigDecimal(1, amount)
                ps.setLong(2, id)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM supply_inventory WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Supply inventory item not found")
            }
        }
    }

    private fun ResultSet.toSupplyInventory() = SupplyInventory(
        id = getLong("id"),
        orgId = getLong("org_id"),
        supplyTypeId = getLong("supply_type_id"),
        quantity = getBigDecimal("quantity"),
        costSek = getObject("cost_sek") as? Int,
        seasonId = getObject("season_id") as? Long,
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
