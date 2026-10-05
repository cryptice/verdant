package app.verdant.repository

import app.verdant.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.NotFoundException
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet

@ApplicationScoped
class HarvestPlanRepository(private val ds: AgroalDataSource, private val mapper: ObjectMapper) {
    fun profile(orgId: Long, speciesId: Long): ProductionProfile? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT profile FROM species_production_profile WHERE org_id = ? AND species_id = ?").use { ps ->
            ps.setLong(1, orgId); ps.setLong(2, speciesId)
            ps.executeQuery().use { rs -> if (rs.next()) mapper.readValue(rs.getString(1), ProductionProfile::class.java) else null }
        }
    }

    fun saveProfile(orgId: Long, speciesId: Long, profile: ProductionProfile) = ds.connection.use { conn ->
        conn.prepareStatement("""INSERT INTO species_production_profile (org_id, species_id, profile) VALUES (?, ?, ?::jsonb)
            ON CONFLICT (org_id, species_id) DO UPDATE SET profile = EXCLUDED.profile""").use { ps ->
            ps.setLong(1, orgId); ps.setLong(2, speciesId); ps.setString(3, mapper.writeValueAsString(profile)); ps.executeUpdate()
        }
    }

    fun list(orgId: Long, limit: Int, offset: Int): List<HarvestPlanResponse> = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM harvest_plan WHERE org_id = ? ORDER BY id DESC LIMIT ? OFFSET ?").use { ps ->
            ps.setLong(1, orgId); ps.setInt(2, limit); ps.setInt(3, offset)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(readPlan(conn, rs)) } }
        }
    }

    fun get(orgId: Long, id: Long): HarvestPlanResponse = ds.connection.use { find(it, orgId, id) }

    fun existing(orgId: Long, request: CreateHarvestPlanRequest): HarvestPlanResponse? = ds.connection.use { conn ->
        findRequest(conn, orgId, request)
    }

    /** The plan and all tasks commit together. The org lock serializes retries of request keys. */
    fun create(orgId: Long, request: CreateHarvestPlanRequest, preview: HarvestPlanPreview): HarvestPlanResponse = transaction { conn ->
        conn.prepareStatement("SELECT pg_advisory_xact_lock(?)").use { ps -> ps.setLong(1, orgId); ps.execute() }
        findRequest(conn, orgId, request)?.let { return@transaction it }
        val id = conn.prepareStatement("""INSERT INTO harvest_plan (org_id, season_id, request_key, request, snapshot)
            VALUES (?, ?, ?, ?::jsonb, ?::jsonb) RETURNING id""").use { ps ->
            ps.setLong(1, orgId); ps.setLong(2, preview.request.seasonId); ps.setObject(3, request.requestKey)
            ps.setString(4, mapper.writeValueAsString(request.target)); ps.setString(5, mapper.writeValueAsString(preview))
            ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        }
        for (allocation in preview.allocations) for (step in allocation.steps) {
            conn.prepareStatement("""INSERT INTO scheduled_task
                (org_id, species_id, activity_type, deadline, target_count, remaining_count, status, notes,
                 season_id, harvest_plan_id, plan_step_key, quantity_unit)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, ?, ?, ?) RETURNING id""").use { ps ->
                ps.setLong(1, orgId); ps.setLong(2, allocation.speciesId); ps.setString(3, step.activityType)
                ps.setDate(4, Date.valueOf(step.date)); ps.setInt(5, step.quantity); ps.setInt(6, step.quantity)
                ps.setString(7, step.name); ps.setLong(8, preview.request.seasonId); ps.setLong(9, id)
                ps.setString(10, step.key); ps.setString(11, step.unit)
                val taskId = ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
                conn.prepareStatement("INSERT INTO scheduled_task_species (scheduled_task_id, species_id) VALUES (?, ?)").use { link ->
                    link.setLong(1, taskId); link.setLong(2, allocation.speciesId); link.executeUpdate()
                }
            }
        }
        find(conn, orgId, id)
    }

    fun complete(orgId: Long, planId: Long, taskId: Long, request: CompleteHarvestPlanTaskRequest): HarvestPlanResponse = transaction { conn ->
        val plan = find(conn, orgId, planId, lock = true)
        if (plan.status != "ACTIVE") throw ClientErrorException("Plan is cancelled", 409)
        conn.prepareStatement("""UPDATE scheduled_task SET remaining_count = remaining_count - ?,
            status = CASE WHEN remaining_count = ? THEN 'COMPLETED' ELSE 'PENDING' END, updated_at = now()
            WHERE id = ? AND harvest_plan_id = ? AND org_id = ? AND remaining_count = ?""").use { ps ->
            ps.setInt(1, request.processedCount); ps.setInt(2, request.processedCount)
            ps.setLong(3, taskId); ps.setLong(4, planId); ps.setLong(5, orgId); ps.setInt(6, request.expectedRemaining)
            if (ps.executeUpdate() != 1) throw ClientErrorException("Task progress changed. Refresh the plan before recording more work.", 409)
        }
        find(conn, orgId, planId)
    }

    fun cancel(orgId: Long, planId: Long): HarvestPlanResponse = transaction { conn ->
        find(conn, orgId, planId, lock = true)
        conn.prepareStatement("UPDATE harvest_plan SET status = 'CANCELLED' WHERE id = ?").use { ps -> ps.setLong(1, planId); ps.executeUpdate() }
        conn.prepareStatement("UPDATE scheduled_task SET status = 'CANCELLED', updated_at = now() WHERE harvest_plan_id = ? AND status = 'PENDING'").use { ps ->
            ps.setLong(1, planId); ps.executeUpdate()
        }
        find(conn, orgId, planId)
    }

    private fun findRequest(conn: Connection, orgId: Long, request: CreateHarvestPlanRequest): HarvestPlanResponse? =
        conn.prepareStatement("SELECT * FROM harvest_plan WHERE org_id = ? AND request_key = ?").use { ps ->
            ps.setLong(1, orgId); ps.setObject(2, request.requestKey)
            ps.executeQuery().use { rs ->
                if (!rs.next()) null else {
                    if (mapper.readValue(rs.getString("request"), HarvestPlanRequest::class.java) != request.target)
                        throw ClientErrorException("This request key was already used for a different target", 409)
                    readPlan(conn, rs)
                }
            }
        }

    private fun find(conn: Connection, orgId: Long, id: Long, lock: Boolean = false): HarvestPlanResponse =
        conn.prepareStatement("SELECT * FROM harvest_plan WHERE org_id = ? AND id = ?" + if (lock) " FOR UPDATE" else "").use { ps ->
            ps.setLong(1, orgId); ps.setLong(2, id)
            ps.executeQuery().use { rs -> if (rs.next()) readPlan(conn, rs) else throw NotFoundException("Harvest plan not found") }
        }

    private fun readPlan(conn: Connection, rs: ResultSet): HarvestPlanResponse {
        val id = rs.getLong("id")
        val tasks = conn.prepareStatement("SELECT * FROM scheduled_task WHERE harvest_plan_id = ? ORDER BY deadline, id").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rows -> buildList { while (rows.next()) add(HarvestPlanTask(
                rows.getLong("id"), rows.getLong("species_id"), rows.getString("plan_step_key"), rows.getInt("remaining_count"), rows.getString("status")
            )) } }
        }
        return HarvestPlanResponse(id, rs.getString("status"), mapper.readValue(rs.getString("snapshot"), HarvestPlanPreview::class.java), tasks, rs.getTimestamp("created_at").toInstant())
    }

    private fun <T> transaction(block: (Connection) -> T): T = ds.connection.use { conn ->
        conn.autoCommit = false
        try { val result = block(conn); conn.commit(); result } catch (e: Exception) { conn.rollback(); throw e }
        finally { conn.autoCommit = true }
    }
}
