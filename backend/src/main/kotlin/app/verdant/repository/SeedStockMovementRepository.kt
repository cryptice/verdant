package app.verdant.repository

import app.verdant.entity.MovementKind
import app.verdant.entity.SeedStockMovement
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.time.LocalDate

@ApplicationScoped
class SeedStockMovementRepository(private val ds: AgroalDataSource) {

    fun persist(m: SeedStockMovement): SeedStockMovement {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO seed_stock_movement (org_id, seed_inventory_id, species_id, kind, quantity, shortfall, unit_cost_cents, occurred_on, season_id, first_plant_id, plant_count, corrects_movement_id, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, m.orgId)
                ps.setObject(2, m.seedInventoryId)
                ps.setLong(3, m.speciesId)
                ps.setString(4, m.kind.name)
                ps.setInt(5, m.quantity)
                ps.setInt(6, m.shortfall)
                ps.setObject(7, m.unitCostCents)
                ps.setObject(8, m.occurredOn)
                ps.setObject(9, m.seasonId)
                ps.setObject(10, m.firstPlantId)
                ps.setObject(11, m.plantCount)
                ps.setObject(12, m.correctsMovementId)
                ps.setTimestamp(13, java.sql.Timestamp.from(m.createdAt))
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return m.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun findById(id: Long): SeedStockMovement? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM seed_stock_movement WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toMovement() else null }
            }
        }

    /** Shortfall already unwound by earlier corrections of the draw [movementId]. */
    fun correctedShortfall(movementId: Long): Int =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT COALESCE(SUM(shortfall), 0) FROM seed_stock_movement WHERE corrects_movement_id = ?"
            ).use { ps ->
                ps.setLong(1, movementId)
                ps.executeQuery().use { rs -> rs.next(); rs.getInt(1) }
            }
        }

    fun findBySeedInventoryId(id: Long): List<SeedStockMovement> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM seed_stock_movement WHERE seed_inventory_id = ? ORDER BY occurred_on, id"
            ).use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toMovement()) }
                }
            }
        }

    /** The day the ledger was introduced; usage before it is not recorded. */
    fun ledgerStartDate(): LocalDate? =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT installed_on::date FROM flyway_schema_history WHERE version = '49'"
            ).use { ps ->
                ps.executeQuery().use { rs -> if (rs.next()) rs.getObject(1, LocalDate::class.java) else null }
            }
        }

    private fun ResultSet.toMovement() = SeedStockMovement(
        id = getLong("id"),
        orgId = getLong("org_id"),
        seedInventoryId = getObject("seed_inventory_id") as? Long,
        speciesId = getLong("species_id"),
        kind = MovementKind.valueOf(getString("kind")),
        quantity = getInt("quantity"),
        shortfall = getInt("shortfall"),
        unitCostCents = getObject("unit_cost_cents") as? Int,
        occurredOn = getObject("occurred_on", LocalDate::class.java),
        seasonId = getObject("season_id") as? Long,
        firstPlantId = getObject("first_plant_id") as? Long,
        plantCount = getObject("plant_count") as? Int,
        correctsMovementId = getObject("corrects_movement_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
