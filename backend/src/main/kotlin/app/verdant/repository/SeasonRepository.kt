package app.verdant.repository

import app.verdant.entity.Season
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SeasonRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Season? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM season WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSeason() else null }
            }
        }

    fun findByOrgId(orgId: Long): List<Season> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM season WHERE org_id = ? ORDER BY year DESC, id DESC").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSeason()) }
                }
            }
        }

    fun findActiveByOrgId(orgId: Long): List<Season> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM season WHERE org_id = ? AND is_active = true ORDER BY year DESC, id DESC").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSeason()) }
                }
            }
        }

    fun persist(season: Season): Season {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO season (org_id, name, year, start_date, end_date, last_frost_date, first_frost_date,
                   growing_degree_base_c, notes, is_active, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, season.orgId)
                ps.setString(2, season.name)
                ps.setInt(3, season.year)
                ps.setObject(4, season.startDate)
                ps.setObject(5, season.endDate)
                ps.setObject(6, season.lastFrostDate)
                ps.setObject(7, season.firstFrostDate)
                season.growingDegreeBaseC?.let { ps.setDouble(8, it) } ?: ps.setNull(8, java.sql.Types.DOUBLE)
                ps.setString(9, season.notes)
                ps.setBoolean(10, season.isActive)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return season.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(season: Season) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE season SET name = ?, year = ?, start_date = ?, end_date = ?, last_frost_date = ?,
                   first_frost_date = ?, growing_degree_base_c = ?, notes = ?, is_active = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, season.name)
                ps.setInt(2, season.year)
                ps.setObject(3, season.startDate)
                ps.setObject(4, season.endDate)
                ps.setObject(5, season.lastFrostDate)
                ps.setObject(6, season.firstFrostDate)
                season.growingDegreeBaseC?.let { ps.setDouble(7, it) } ?: ps.setNull(7, java.sql.Types.DOUBLE)
                ps.setString(8, season.notes)
                ps.setBoolean(9, season.isActive)
                ps.setLong(10, season.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM season WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Season not found")
            }
        }
    }

    private fun ResultSet.toSeason() = Season(
        id = getLong("id"),
        orgId = getLong("org_id"),
        name = getString("name"),
        year = getInt("year"),
        startDate = getObject("start_date", java.time.LocalDate::class.java),
        endDate = getObject("end_date", java.time.LocalDate::class.java),
        lastFrostDate = getObject("last_frost_date", java.time.LocalDate::class.java),
        firstFrostDate = getObject("first_frost_date", java.time.LocalDate::class.java),
        growingDegreeBaseC = getDouble("growing_degree_base_c").takeIf { !wasNull() },
        notes = getString("notes"),
        isActive = getBoolean("is_active"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
