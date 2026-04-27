package app.verdant.repository

import app.verdant.entity.BedEvent
import app.verdant.entity.PlantEventType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class BedEventRepository(private val ds: AgroalDataSource) {

    fun persist(event: BedEvent): BedEvent = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO bed_event (bed_id, event_type, event_date, notes, plants_affected, created_at)
               VALUES (?, ?, ?, ?, ?, now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, event.bedId)
            ps.setString(2, event.eventType.name)
            ps.setDate(3, Date.valueOf(event.eventDate))
            ps.setString(4, event.notes)
            event.plantsAffected?.let { ps.setInt(5, it) } ?: ps.setNull(5, java.sql.Types.INTEGER)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); event.copy(id = rs.getLong(1)) }
        }
    }

    fun findByBedId(bedId: Long, limit: Int = 50): List<BedEvent> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT * FROM bed_event WHERE bed_id = ? ORDER BY event_date DESC, id DESC LIMIT ?"""
        ).use { ps ->
            ps.setLong(1, bedId)
            ps.setInt(2, limit)
            ps.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.toBedEvent()) }
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM bed_event WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toBedEvent() = BedEvent(
        id = getLong("id"),
        bedId = getLong("bed_id"),
        eventType = PlantEventType.valueOf(getString("event_type")),
        eventDate = getDate("event_date").toLocalDate(),
        notes = getString("notes"),
        plantsAffected = getInt("plants_affected").takeIf { !wasNull() },
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
