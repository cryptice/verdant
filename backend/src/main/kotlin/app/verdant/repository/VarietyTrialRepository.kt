package app.verdant.repository

import app.verdant.entity.Reception
import app.verdant.entity.VarietyTrial
import app.verdant.entity.Verdict
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class VarietyTrialRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): VarietyTrial? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM variety_trial WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toVarietyTrial() else null }
            }
        }

    fun findByOrgId(orgId: Long, limit: Int = 50, offset: Int = 0): List<VarietyTrial> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM variety_trial WHERE org_id = ? ORDER BY id DESC LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toVarietyTrial()) }
                }
            }
        }

    fun findBySeasonId(orgId: Long, seasonId: Long, limit: Int = 50, offset: Int = 0): List<VarietyTrial> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM variety_trial WHERE org_id = ? AND season_id = ? ORDER BY id DESC LIMIT ? OFFSET ?").use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, seasonId)
                ps.setInt(3, limit)
                ps.setInt(4, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toVarietyTrial()) }
                }
            }
        }

    fun findBySpeciesId(orgId: Long, speciesId: Long): List<VarietyTrial> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM variety_trial WHERE org_id = ? AND species_id = ? ORDER BY id DESC").use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, speciesId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toVarietyTrial()) }
                }
            }
        }

    fun persist(trial: VarietyTrial): VarietyTrial {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO variety_trial (org_id, season_id, species_id, bed_id, plant_count,
                   stem_yield, avg_stem_length_cm, avg_vase_life_days, quality_score,
                   customer_reception, verdict, notes, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, trial.orgId)
                ps.setLong(2, trial.seasonId)
                ps.setLong(3, trial.speciesId)
                ps.setObject(4, trial.bedId)
                ps.setObject(5, trial.plantCount)
                ps.setObject(6, trial.stemYield)
                ps.setObject(7, trial.avgStemLengthCm)
                ps.setObject(8, trial.avgVaseLifeDays)
                ps.setObject(9, trial.qualityScore)
                trial.customerReception?.let { ps.setString(10, it.name) } ?: ps.setNull(10, java.sql.Types.VARCHAR)
                ps.setString(11, trial.verdict.name)
                ps.setString(12, trial.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return trial.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(trial: VarietyTrial) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE variety_trial SET season_id = ?, species_id = ?, bed_id = ?, plant_count = ?,
                   stem_yield = ?, avg_stem_length_cm = ?, avg_vase_life_days = ?, quality_score = ?,
                   customer_reception = ?, verdict = ?, notes = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setLong(1, trial.seasonId)
                ps.setLong(2, trial.speciesId)
                ps.setObject(3, trial.bedId)
                ps.setObject(4, trial.plantCount)
                ps.setObject(5, trial.stemYield)
                ps.setObject(6, trial.avgStemLengthCm)
                ps.setObject(7, trial.avgVaseLifeDays)
                ps.setObject(8, trial.qualityScore)
                trial.customerReception?.let { ps.setString(9, it.name) } ?: ps.setNull(9, java.sql.Types.VARCHAR)
                ps.setString(10, trial.verdict.name)
                ps.setString(11, trial.notes)
                ps.setLong(12, trial.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM variety_trial WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Variety trial not found")
            }
        }
    }

    private fun ResultSet.toVarietyTrial() = VarietyTrial(
        id = getLong("id"),
        orgId = getLong("org_id"),
        seasonId = getLong("season_id"),
        speciesId = getLong("species_id"),
        bedId = getObject("bed_id") as? Long,
        plantCount = getObject("plant_count") as? Int,
        stemYield = getObject("stem_yield") as? Int,
        avgStemLengthCm = getObject("avg_stem_length_cm") as? Int,
        avgVaseLifeDays = getObject("avg_vase_life_days") as? Int,
        qualityScore = getObject("quality_score") as? Int,
        customerReception = getString("customer_reception")?.let { Reception.valueOf(it) },
        verdict = Verdict.valueOf(getString("verdict")),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
