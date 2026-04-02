package app.verdant.repository

import app.verdant.entity.SuccessionSchedule
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SuccessionScheduleRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SuccessionSchedule? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM succession_schedule WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSuccessionSchedule() else null }
            }
        }

    fun findByUserId(userId: Long, limit: Int = 50, offset: Int = 0): List<SuccessionSchedule> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM succession_schedule WHERE user_id = ? ORDER BY first_sow_date, id LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, userId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSuccessionSchedule()) }
                }
            }
        }

    fun findBySeasonId(userId: Long, seasonId: Long, limit: Int = 50, offset: Int = 0): List<SuccessionSchedule> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM succession_schedule WHERE user_id = ? AND season_id = ? ORDER BY first_sow_date, id LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, seasonId)
                ps.setInt(3, limit)
                ps.setInt(4, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSuccessionSchedule()) }
                }
            }
        }

    fun persist(schedule: SuccessionSchedule): SuccessionSchedule {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO succession_schedule (user_id, season_id, species_id, bed_id,
                   first_sow_date, interval_days, total_successions, seeds_per_succession, notes, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, schedule.userId)
                ps.setLong(2, schedule.seasonId)
                ps.setLong(3, schedule.speciesId)
                ps.setObject(4, schedule.bedId)
                ps.setDate(5, Date.valueOf(schedule.firstSowDate))
                ps.setInt(6, schedule.intervalDays)
                ps.setInt(7, schedule.totalSuccessions)
                ps.setInt(8, schedule.seedsPerSuccession)
                ps.setString(9, schedule.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return schedule.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(schedule: SuccessionSchedule) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE succession_schedule SET season_id = ?, species_id = ?, bed_id = ?,
                   first_sow_date = ?, interval_days = ?, total_successions = ?,
                   seeds_per_succession = ?, notes = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setLong(1, schedule.seasonId)
                ps.setLong(2, schedule.speciesId)
                ps.setObject(3, schedule.bedId)
                ps.setDate(4, Date.valueOf(schedule.firstSowDate))
                ps.setInt(5, schedule.intervalDays)
                ps.setInt(6, schedule.totalSuccessions)
                ps.setInt(7, schedule.seedsPerSuccession)
                ps.setString(8, schedule.notes)
                ps.setLong(9, schedule.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM succession_schedule WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Succession schedule not found")
            }
        }
    }

    private fun ResultSet.toSuccessionSchedule() = SuccessionSchedule(
        id = getLong("id"),
        userId = getLong("user_id"),
        seasonId = getLong("season_id"),
        speciesId = getLong("species_id"),
        bedId = getObject("bed_id") as? Long,
        firstSowDate = getDate("first_sow_date").toLocalDate(),
        intervalDays = getInt("interval_days"),
        totalSuccessions = getInt("total_successions"),
        seedsPerSuccession = getInt("seeds_per_succession"),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
