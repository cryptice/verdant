package app.verdant.repository

import app.verdant.entity.Outcome
import app.verdant.entity.PestCategory
import app.verdant.entity.PestDiseaseLog
import app.verdant.entity.Severity
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class PestDiseaseLogRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): PestDiseaseLog? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM pest_disease_log WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toPestDiseaseLog() else null }
            }
        }

    fun findByOrgId(orgId: Long, limit: Int = 50, offset: Int = 0): List<PestDiseaseLog> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM pest_disease_log WHERE org_id = ? ORDER BY observed_date DESC, id DESC LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPestDiseaseLog()) }
                }
            }
        }

    fun findBySeasonId(orgId: Long, seasonId: Long, limit: Int = 50, offset: Int = 0): List<PestDiseaseLog> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM pest_disease_log WHERE org_id = ? AND season_id = ? ORDER BY observed_date DESC, id DESC LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, seasonId)
                ps.setInt(3, limit)
                ps.setInt(4, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPestDiseaseLog()) }
                }
            }
        }

    fun persist(log: PestDiseaseLog): PestDiseaseLog {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO pest_disease_log (org_id, season_id, bed_id, species_id, observed_date,
                   category, name, severity, treatment, outcome, notes, image_url, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, log.orgId)
                ps.setObject(2, log.seasonId)
                ps.setObject(3, log.bedId)
                ps.setObject(4, log.speciesId)
                ps.setDate(5, Date.valueOf(log.observedDate))
                ps.setString(6, log.category.name)
                ps.setString(7, log.name)
                ps.setString(8, log.severity.name)
                ps.setString(9, log.treatment)
                log.outcome?.let { ps.setString(10, it.name) } ?: ps.setNull(10, java.sql.Types.VARCHAR)
                ps.setString(11, log.notes)
                ps.setString(12, log.imageUrl)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return log.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(log: PestDiseaseLog) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE pest_disease_log SET season_id = ?, bed_id = ?, species_id = ?, observed_date = ?,
                   category = ?, name = ?, severity = ?, treatment = ?, outcome = ?, notes = ?, image_url = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setObject(1, log.seasonId)
                ps.setObject(2, log.bedId)
                ps.setObject(3, log.speciesId)
                ps.setDate(4, Date.valueOf(log.observedDate))
                ps.setString(5, log.category.name)
                ps.setString(6, log.name)
                ps.setString(7, log.severity.name)
                ps.setString(8, log.treatment)
                log.outcome?.let { ps.setString(9, it.name) } ?: ps.setNull(9, java.sql.Types.VARCHAR)
                ps.setString(10, log.notes)
                ps.setString(11, log.imageUrl)
                ps.setLong(12, log.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM pest_disease_log WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Pest/disease log not found")
            }
        }
    }

    private fun ResultSet.toPestDiseaseLog() = PestDiseaseLog(
        id = getLong("id"),
        orgId = getLong("org_id"),
        seasonId = getObject("season_id") as? Long,
        bedId = getObject("bed_id") as? Long,
        speciesId = getObject("species_id") as? Long,
        observedDate = getDate("observed_date").toLocalDate(),
        category = PestCategory.valueOf(getString("category")),
        name = getString("name"),
        severity = Severity.valueOf(getString("severity")),
        treatment = getString("treatment"),
        outcome = getString("outcome")?.let { Outcome.valueOf(it) },
        notes = getString("notes"),
        imageUrl = getString("image_url"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
