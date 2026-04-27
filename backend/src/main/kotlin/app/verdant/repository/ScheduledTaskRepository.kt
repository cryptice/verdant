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

    fun findByOrgId(orgId: Long, limit: Int = 50, offset: Int = 0): List<ScheduledTask> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM scheduled_task WHERE org_id = ?
                   ORDER BY CASE status WHEN 'PENDING' THEN 0 ELSE 1 END, deadline LIMIT ? OFFSET ?"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toScheduledTask()) }
                }
            }
        }

    fun findBySeasonId(orgId: Long, seasonId: Long, limit: Int = 50, offset: Int = 0): List<ScheduledTask> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT * FROM scheduled_task WHERE org_id = ? AND season_id = ?
                   ORDER BY CASE status WHEN 'PENDING' THEN 0 ELSE 1 END, deadline LIMIT ? OFFSET ?"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, seasonId)
                ps.setInt(3, limit)
                ps.setInt(4, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toScheduledTask()) }
                }
            }
        }

    fun persist(task: ScheduledTask): ScheduledTask {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO scheduled_task (org_id, species_id, bed_id, activity_type, deadline, target_count, remaining_count, status, notes, season_id, succession_schedule_id, origin_group_id, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, task.orgId)
                ps.setObject(2, task.speciesId)
                ps.setObject(3, task.bedId)
                ps.setString(4, task.activityType)
                ps.setDate(5, Date.valueOf(task.deadline))
                ps.setInt(6, task.targetCount)
                ps.setInt(7, task.remainingCount)
                ps.setString(8, task.status.name)
                ps.setString(9, task.notes)
                ps.setObject(10, task.seasonId)
                ps.setObject(11, task.successionScheduleId)
                ps.setObject(12, task.originGroupId)
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
                   season_id = ?, succession_schedule_id = ?, origin_group_id = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setObject(1, task.speciesId)
                ps.setString(2, task.activityType)
                ps.setDate(3, Date.valueOf(task.deadline))
                ps.setInt(4, task.targetCount)
                ps.setInt(5, task.remainingCount)
                ps.setString(6, task.status.name)
                ps.setString(7, task.notes)
                ps.setObject(8, task.seasonId)
                ps.setObject(9, task.successionScheduleId)
                ps.setObject(10, task.originGroupId)
                ps.setLong(11, task.id!!)
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

    fun addAcceptableSpecies(taskId: Long, speciesId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO scheduled_task_species (scheduled_task_id, species_id) VALUES (?, ?) ON CONFLICT DO NOTHING"
            ).use { ps ->
                ps.setLong(1, taskId)
                ps.setLong(2, speciesId)
                ps.executeUpdate()
            }
        }
    }

    fun setAcceptableSpecies(taskId: Long, speciesIds: List<Long>) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM scheduled_task_species WHERE scheduled_task_id = ?").use { ps ->
                ps.setLong(1, taskId)
                ps.executeUpdate()
            }
            if (speciesIds.isNotEmpty()) {
                conn.prepareStatement("INSERT INTO scheduled_task_species (scheduled_task_id, species_id) VALUES (?, ?)").use { ps ->
                    for (speciesId in speciesIds) {
                        ps.setLong(1, taskId)
                        ps.setLong(2, speciesId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
        }
    }

    fun findAcceptableSpeciesIds(taskId: Long): List<Long> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT species_id FROM scheduled_task_species WHERE scheduled_task_id = ?").use { ps ->
                ps.setLong(1, taskId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getLong("species_id")) }
                }
            }
        }

    fun findAcceptableSpeciesIdsByTaskIds(taskIds: Set<Long>): Map<Long, List<Long>> {
        if (taskIds.isEmpty()) return emptyMap()
        val placeholders = taskIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT scheduled_task_id, species_id FROM scheduled_task_species WHERE scheduled_task_id IN ($placeholders)").use { ps ->
                taskIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    val result = mutableMapOf<Long, MutableList<Long>>()
                    while (rs.next()) {
                        result.getOrPut(rs.getLong("scheduled_task_id")) { mutableListOf() }
                            .add(rs.getLong("species_id"))
                    }
                    result
                }
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM scheduled_task WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Scheduled task not found")
            }
        }
    }

    private fun ResultSet.toScheduledTask() = ScheduledTask(
        id = getLong("id"),
        orgId = getLong("org_id"),
        speciesId = getObject("species_id") as? Long,
        bedId = getObject("bed_id") as? Long,
        activityType = getString("activity_type"),
        deadline = getDate("deadline").toLocalDate(),
        targetCount = getInt("target_count"),
        remainingCount = getInt("remaining_count"),
        status = ScheduledTaskStatus.valueOf(getString("status")),
        notes = getString("notes"),
        seasonId = getObject("season_id") as? Long,
        successionScheduleId = getObject("succession_schedule_id") as? Long,
        originGroupId = getObject("origin_group_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
