package app.verdant.repository

import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class MaintenanceRuleRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): MaintenanceRule? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM maintenance_rule WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toRule() else null }
        }
    }

    fun findByBedId(bedId: Long): List<MaintenanceRule> = query(
        "SELECT * FROM maintenance_rule WHERE bed_id = ? ORDER BY id", bedId
    )

    fun findByAreaId(areaId: Long): List<MaintenanceRule> = query(
        "SELECT * FROM maintenance_rule WHERE garden_area_id = ? ORDER BY id", areaId
    )

    fun findByOrgId(orgId: Long): List<MaintenanceRule> = query(
        "SELECT * FROM maintenance_rule WHERE org_id = ? ORDER BY id", orgId
    )

    /**
     * Active rules with no PENDING task outstanding — the scheduler's work list.
     * The NOT EXISTS mirrors the partial unique index on scheduled_task.
     */
    fun findActiveWithoutOpenTask(): List<MaintenanceRule> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT r.* FROM maintenance_rule r
               WHERE r.active = true
                 AND NOT EXISTS (
                     SELECT 1 FROM scheduled_task t
                     WHERE t.maintenance_rule_id = r.id AND t.status = 'PENDING'
                 )
               ORDER BY r.id"""
        ).use { ps ->
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toRule()) } }
        }
    }

    fun persist(rule: MaintenanceRule): MaintenanceRule = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO maintenance_rule (org_id, bed_id, garden_area_id, activity_type, interval_days,
                                             anchor_date, season_start_month, season_start_day,
                                             season_end_month, season_end_day, active, notes,
                                             created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, rule.orgId)
            ps.setObject(2, rule.bedId)
            ps.setObject(3, rule.gardenAreaId)
            ps.setString(4, rule.activity.name)
            ps.setInt(5, rule.intervalDays)
            rule.anchorDate?.let { ps.setDate(6, Date.valueOf(it)) } ?: ps.setNull(6, java.sql.Types.DATE)
            ps.setObject(7, rule.seasonStartMonth)
            ps.setObject(8, rule.seasonStartDay)
            ps.setObject(9, rule.seasonEndMonth)
            ps.setObject(10, rule.seasonEndDay)
            ps.setBoolean(11, rule.active)
            ps.setString(12, rule.notes)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); rule.copy(id = rs.getLong(1)) }
        }
    }

    fun update(rule: MaintenanceRule) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE maintenance_rule
                   SET activity_type = ?, interval_days = ?, anchor_date = ?,
                       season_start_month = ?, season_start_day = ?,
                       season_end_month = ?, season_end_day = ?,
                       active = ?, notes = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, rule.activity.name)
                ps.setInt(2, rule.intervalDays)
                rule.anchorDate?.let { ps.setDate(3, Date.valueOf(it)) } ?: ps.setNull(3, java.sql.Types.DATE)
                ps.setObject(4, rule.seasonStartMonth)
                ps.setObject(5, rule.seasonStartDay)
                ps.setObject(6, rule.seasonEndMonth)
                ps.setObject(7, rule.seasonEndDay)
                ps.setBoolean(8, rule.active)
                ps.setString(9, rule.notes)
                ps.setLong(10, rule.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM maintenance_rule WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun query(sql: String, id: Long): List<MaintenanceRule> = ds.connection.use { conn ->
        conn.prepareStatement(sql).use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toRule()) } }
        }
    }

    private fun ResultSet.toRule() = MaintenanceRule(
        id = getLong("id"),
        orgId = getLong("org_id"),
        bedId = getObject("bed_id") as? Long,
        gardenAreaId = getObject("garden_area_id") as? Long,
        activity = MaintenanceActivity.parse(getString("activity_type")),
        intervalDays = getInt("interval_days"),
        anchorDate = getDate("anchor_date")?.toLocalDate(),
        seasonStartMonth = (getObject("season_start_month") as? Number)?.toInt(),
        seasonStartDay = (getObject("season_start_day") as? Number)?.toInt(),
        seasonEndMonth = (getObject("season_end_month") as? Number)?.toInt(),
        seasonEndDay = (getObject("season_end_day") as? Number)?.toInt(),
        active = getBoolean("active"),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
