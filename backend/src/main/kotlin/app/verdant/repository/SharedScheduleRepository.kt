package app.verdant.repository

import app.verdant.dto.SharedSchedule
import com.fasterxml.jackson.databind.ObjectMapper
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ClientErrorException
import java.sql.ResultSet

@ApplicationScoped
class SharedScheduleRepository(private val ds: AgroalDataSource, private val mapper: ObjectMapper) {
    fun lock() = ds.connection.use { conn ->
        conn.prepareStatement("SELECT pg_advisory_xact_lock(865402, 2)").use { it.execute() }
    }

    fun findAll(): List<SharedSchedule> = ds.connection.use { conn ->
        conn.prepareStatement("SELECT definition, revision FROM shared_production_schedule ORDER BY priority, key").use { ps ->
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toSchedule()) } }
        }
    }

    fun assignments(): Map<Long, String> = ds.connection.use { conn ->
        conn.prepareStatement("SELECT species_id, schedule_key FROM species_shared_schedule").use { ps ->
            ps.executeQuery().use { rs -> buildMap { while (rs.next()) put(rs.getLong(1), rs.getString(2)) } }
        }
    }

    fun create(schedule: SharedSchedule) = ds.connection.use { conn ->
        conn.prepareStatement("INSERT INTO shared_production_schedule (key, priority, definition) VALUES (?, ?, ?::jsonb)").use { ps ->
            ps.setString(1, schedule.key); ps.setInt(2, schedule.priority)
            ps.setString(3, mapper.writeValueAsString(schedule)); ps.executeUpdate()
        }
    }

    fun update(schedule: SharedSchedule) = ds.connection.use { conn ->
        conn.prepareStatement("UPDATE shared_production_schedule SET priority = ?, definition = ?::jsonb, revision = revision + 1 WHERE key = ? AND revision = ?").use { ps ->
            ps.setInt(1, schedule.priority); ps.setString(2, mapper.writeValueAsString(schedule))
            ps.setString(3, schedule.key); ps.setInt(4, schedule.revision)
            if (ps.executeUpdate() != 1) throw ClientErrorException("This schedule changed. Reload before saving.", 409)
        }
    }

    fun delete(key: String, revision: Int) = ds.connection.use { conn ->
        conn.prepareStatement("DELETE FROM shared_production_schedule WHERE key = ? AND revision = ?").use { ps ->
            ps.setString(1, key); ps.setInt(2, revision)
            if (ps.executeUpdate() != 1) throw ClientErrorException("This schedule changed. Reload before deleting.", 409)
        }
    }

    fun assign(speciesId: Long, key: String?) = ds.connection.use { conn ->
        if (key == null) conn.prepareStatement("DELETE FROM species_shared_schedule WHERE species_id = ?").use { ps ->
            ps.setLong(1, speciesId); ps.executeUpdate()
        } else conn.prepareStatement("INSERT INTO species_shared_schedule (species_id, schedule_key) VALUES (?, ?) ON CONFLICT (species_id) DO UPDATE SET schedule_key = EXCLUDED.schedule_key").use { ps ->
            ps.setLong(1, speciesId); ps.setString(2, key); ps.executeUpdate()
        }
    }

    private fun ResultSet.toSchedule() = mapper.readValue(getString("definition"), SharedSchedule::class.java)
        .copy(revision = getInt("revision"))
}
