package app.verdant.repository

import app.verdant.entity.SaleLotEvent
import app.verdant.entity.SaleLotEventType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SaleLotEventRepository(private val ds: AgroalDataSource) {

    fun findByLotId(lotId: Long): List<SaleLotEvent> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM sale_lot_event WHERE sale_lot_id = ? ORDER BY created_at, id",
            ).use { ps ->
                ps.setLong(1, lotId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toEvent()) }
                }
            }
        }

    fun persist(event: SaleLotEvent): SaleLotEvent {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO sale_lot_event (sale_lot_id, event_type, payload_json, recorded_by_user_id, created_at)
                   VALUES (?, ?, ?::jsonb, ?, now())""",
                Statement.RETURN_GENERATED_KEYS,
            ).use { ps ->
                ps.setLong(1, event.saleLotId)
                ps.setString(2, event.eventType.name)
                ps.setString(3, event.payloadJson)
                ps.setLong(4, event.recordedByUserId)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return event.copy(id = rs.getLong(1))
                }
            }
        }
    }

    private fun ResultSet.toEvent() = SaleLotEvent(
        id = getLong("id"),
        saleLotId = getLong("sale_lot_id"),
        eventType = SaleLotEventType.valueOf(getString("event_type")),
        payloadJson = getString("payload_json"),
        recordedByUserId = getLong("recorded_by_user_id"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
