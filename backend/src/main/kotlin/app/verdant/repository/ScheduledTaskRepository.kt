package app.verdant.repository

import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class ScheduledTaskRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): ScheduledTask? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM scheduled_task WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toScheduledTask() else null }
            }
        }

    fun findByUserId(userId: Long): List<ScheduledTask> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM scheduled_task WHERE user_id = ?
                   ORDER BY CASE status WHEN 'PENDING' THEN 0 ELSE 1 END, deadline"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toScheduledTask()) }
                }
            }
        }

    fun findBySeasonId(userId: Long, seasonId: Long): List<ScheduledTask> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM scheduled_task WHERE user_id = ? AND season_id = ?
                   ORDER BY CASE status WHEN 'PENDING' THEN 0 ELSE 1 END, deadline"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, seasonId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toScheduledTask()) }
                }
            }
        }

    fun persist(task: ScheduledTask): ScheduledTask {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO scheduled_task (user_id, species_id, activity_type, deadline, target_count, remaining_count, status, notes, season_id, succession_schedule_id, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, task.userId)
                ps.setLong(2, task.speciesId)
                ps.setString(3, task.activityType)
                ps.setDate(4, Date.valueOf(task.deadline))
                ps.setInt(5, task.targetCount)
                ps.setInt(6, task.remainingCount)
                ps.setString(7, task.status.name)
                ps.setString(8, task.notes)
                ps.setObject(9, task.seasonId)
                ps.setObject(10, task.successionScheduleId)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return task.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(task: ScheduledTask) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE scheduled_task SET species_id = ?, activity_type = ?, deadline = ?,
                   target_count = ?, remaining_count = ?, status = ?, notes = ?,
                   season_id = ?, succession_schedule_id = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setLong(1, task.speciesId)
                ps.setString(2, task.activityType)
                ps.setDate(3, Date.valueOf(task.deadline))
                ps.setInt(4, task.targetCount)
                ps.setInt(5, task.remainingCount)
                ps.setString(6, task.status.name)
                ps.setString(7, task.notes)
                ps.setObject(8, task.seasonId)
                ps.setObject(9, task.successionScheduleId)
                ps.setLong(10, task.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun decrementRemainingCount(id: Long, count: Int) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE scheduled_task
                   SET remaining_count = GREATEST(remaining_count - ?, 0),
                       status = CASE WHEN remaining_count - ? <= 0 THEN 'COMPLETED' ELSE status END,
                       updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setInt(1, count)
                ps.setInt(2, count)
                ps.setLong(3, id)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM scheduled_task WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toScheduledTask() = ScheduledTask(
        id = getLong("id"),
        userId = getLong("user_id"),
        speciesId = getLong("species_id"),
        activityType = getString("activity_type"),
        deadline = getDate("deadline").toLocalDate(),
        targetCount = getInt("target_count"),
        remainingCount = getInt("remaining_count"),
        status = ScheduledTaskStatus.valueOf(getString("status")),
        notes = getString("notes"),
        seasonId = getObject("season_id") as? Long,
        successionScheduleId = getObject("succession_schedule_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
