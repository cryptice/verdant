package app.verdant.repository

import app.verdant.entity.GardenAreaEvent
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement
import java.time.LocalDate

@ApplicationScoped
class GardenAreaEventRepository(private val ds: AgroalDataSource) {

    fun persist(event: GardenAreaEvent): GardenAreaEvent = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO garden_area_event (garden_area_id, event_type, event_date, notes, created_at)
               VALUES (?, ?, ?, ?, now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, event.gardenAreaId)
            ps.setString(2, event.eventType)
            ps.setDate(3, Date.valueOf(event.eventDate))
            ps.setString(4, event.notes)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); event.copy(id = rs.getLong(1)) }
        }
    }

    fun findByAreaId(areaId: Long, limit: Int = 50): List<GardenAreaEvent> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT * FROM garden_area_event WHERE garden_area_id = ?
               ORDER BY event_date DESC, id DESC LIMIT ?"""
        ).use { ps ->
            ps.setLong(1, areaId)
            ps.setInt(2, limit)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toEvent()) } }
        }
    }

    /** Newest [eventType] date for the area, or null if it has never been logged. */
    fun findLatestDate(areaId: Long, eventType: String): LocalDate? = ds.connection.use { conn ->
        conn.prepareStatement(
            "SELECT MAX(event_date) AS latest FROM garden_area_event WHERE garden_area_id = ? AND event_type = ?"
        ).use { ps ->
            ps.setLong(1, areaId)
            ps.setString(2, eventType)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getDate("latest")?.toLocalDate() else null }
        }
    }

    private fun ResultSet.toEvent() = GardenAreaEvent(
        id = getLong("id"),
        gardenAreaId = getLong("garden_area_id"),
        eventType = getString("event_type"),
        eventDate = getDate("event_date").toLocalDate(),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
