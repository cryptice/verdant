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
                   (org_id, bed_id, supply_inventory_id, supply_type_id, quantity,
                    target_scope, applied_at, applied_by, workflow_step_id, notes)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf("id")
            ).use { ps ->
                ps.setLong(1, app.orgId)
                ps.setLong(2, app.bedId)
                ps.setLong(3, app.supplyInventoryId)
                ps.setLong(4, app.supplyTypeId)
                ps.setBigDecimal(5, app.quantity)
                ps.setString(6, app.targetScope.name)
                ps.setObject(7, app.appliedAt.atOffset(ZoneOffset.UTC))
                ps.setLong(8, app.appliedBy)
                app.workflowStepId?.let { ps.setLong(9, it) } ?: ps.setNull(9, java.sql.Types.BIGINT)
                ps.setString(10, app.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs -> rs.next(); return app.copy(id = rs.getLong(1)) }
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
        bedId = getLong("bed_id"),
        supplyInventoryId = getLong("supply_inventory_id"),
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
