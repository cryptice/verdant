package app.verdant.repository

import app.verdant.entity.SupplyApplication
import app.verdant.entity.SupplyApplicationScope
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.time.ZoneOffset

@ApplicationScoped
class SupplyApplicationRepository(private val ds: AgroalDataSource) {

    fun insert(app: SupplyApplication): SupplyApplication {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO supply_application
                   (org_id, bed_id, tray_location_id, supply_inventory_id, supply_type_id, quantity,
                    target_scope, applied_at, applied_by, workflow_step_id, notes)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf("id")
            ).use { ps ->
                ps.setLong(1, app.orgId)
                app.bedId?.let { ps.setLong(2, it) } ?: ps.setNull(2, java.sql.Types.BIGINT)
                app.trayLocationId?.let { ps.setLong(3, it) } ?: ps.setNull(3, java.sql.Types.BIGINT)
                app.supplyInventoryId?.let { ps.setLong(4, it) } ?: ps.setNull(4, java.sql.Types.BIGINT)
                ps.setLong(5, app.supplyTypeId)
                ps.setBigDecimal(6, app.quantity)
                ps.setString(7, app.targetScope.name)
                ps.setObject(8, app.appliedAt.atOffset(ZoneOffset.UTC))
                ps.setLong(9, app.appliedBy)
                app.workflowStepId?.let { ps.setLong(10, it) } ?: ps.setNull(10, java.sql.Types.BIGINT)
                ps.setString(11, app.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs -> rs.next(); return app.copy(id = rs.getLong(1)) }
            }
        }
    }

    fun findByTrayLocation(trayLocationId: Long, limit: Int = 20): List<SupplyApplication> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM supply_application
                   WHERE tray_location_id = ?
                   ORDER BY applied_at DESC
                   LIMIT ?"""
            ).use { ps ->
                ps.setLong(1, trayLocationId)
                ps.setInt(2, limit)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSupplyApplication()) }
                }
            }
        }

    fun findById(id: Long): SupplyApplication? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM supply_application WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSupplyApplication() else null }
            }
        }

    fun findByBed(bedId: Long, limit: Int = 20): List<SupplyApplication> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM supply_application
                   WHERE bed_id = ?
                   ORDER BY applied_at DESC
                   LIMIT ?"""
            ).use { ps ->
                ps.setLong(1, bedId)
                ps.setInt(2, limit)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSupplyApplication()) }
                }
            }
        }

    fun findByGarden(gardenId: Long, limit: Int = 20): List<SupplyApplication> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT sa.* FROM supply_application sa
                   JOIN bed b ON b.id = sa.bed_id
                   WHERE b.garden_id = ?
                   ORDER BY sa.applied_at DESC
                   LIMIT ?"""
            ).use { ps ->
                ps.setLong(1, gardenId)
                ps.setInt(2, limit)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSupplyApplication()) }
                }
            }
        }

    fun findPlantIdsForApplication(applicationId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT plant_id FROM plant_event WHERE supply_application_id = ? ORDER BY plant_id"
            ).use { ps ->
                ps.setLong(1, applicationId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("plant_id")) }
                }
            }
        }

    private fun ResultSet.toSupplyApplication(): SupplyApplication = SupplyApplication(
        id = getLong("id"),
        orgId = getLong("org_id"),
        bedId = getLong("bed_id").takeIf { !wasNull() },
        trayLocationId = getLong("tray_location_id").takeIf { !wasNull() },
        supplyInventoryId = getLong("supply_inventory_id").takeIf { !wasNull() },
        supplyTypeId = getLong("supply_type_id"),
        quantity = getBigDecimal("quantity"),
        targetScope = SupplyApplicationScope.valueOf(getString("target_scope")),
        appliedAt = getTimestamp("applied_at").toInstant(),
        appliedBy = getLong("applied_by"),
        workflowStepId = getLong("workflow_step_id").takeIf { !wasNull() },
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
