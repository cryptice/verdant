package app.verdant.repository

import app.verdant.entity.ProductionTarget
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class ProductionTargetRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): ProductionTarget? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM production_target WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toProductionTarget() else null }
            }
        }

    fun findByUserId(userId: Long, limit: Int = 50, offset: Int = 0): List<ProductionTarget> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM production_target WHERE user_id = ? ORDER BY start_date, id LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, userId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toProductionTarget()) }
                }
            }
        }

    fun findBySeasonId(userId: Long, seasonId: Long, limit: Int = 50, offset: Int = 0): List<ProductionTarget> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM production_target WHERE user_id = ? AND season_id = ? ORDER BY start_date, id LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, seasonId)
                ps.setInt(3, limit)
                ps.setInt(4, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toProductionTarget()) }
                }
            }
        }

    fun persist(target: ProductionTarget): ProductionTarget {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO production_target (user_id, season_id, species_id, stems_per_week,
                   start_date, end_date, notes, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, target.userId)
                ps.setLong(2, target.seasonId)
                ps.setLong(3, target.speciesId)
                ps.setInt(4, target.stemsPerWeek)
                ps.setDate(5, Date.valueOf(target.startDate))
                ps.setDate(6, Date.valueOf(target.endDate))
                ps.setString(7, target.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return target.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(target: ProductionTarget) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE production_target SET season_id = ?, species_id = ?, stems_per_week = ?,
                   start_date = ?, end_date = ?, notes = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setLong(1, target.seasonId)
                ps.setLong(2, target.speciesId)
                ps.setInt(3, target.stemsPerWeek)
                ps.setDate(4, Date.valueOf(target.startDate))
                ps.setDate(5, Date.valueOf(target.endDate))
                ps.setString(6, target.notes)
                ps.setLong(7, target.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM production_target WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Production target not found")
            }
        }
    }

    private fun ResultSet.toProductionTarget() = ProductionTarget(
        id = getLong("id"),
        userId = getLong("user_id"),
        seasonId = getLong("season_id"),
        speciesId = getLong("species_id"),
        stemsPerWeek = getInt("stems_per_week"),
        startDate = getDate("start_date").toLocalDate(),
        endDate = getDate("end_date").toLocalDate(),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
